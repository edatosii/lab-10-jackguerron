package co.umariana.dao;

import co.umariana.modelo.Persona;
import java.sql.Types;

import co.umariana.conexion.ConexionBD;
import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ResumenArbol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object): la única clase que habla con la base de datos. Guarda los árboles genealógicos
 * en las tablas "arboles" y "miembros" (ver db/schema.sql). Se construye igual que EstudianteDAO del laboratorio 9.
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 4)
 * ------------------------------------------
 * 1. Cada método marcado con "TODO" lanza UnsupportedOperationException. Reemplace esa línea por su
 *    implementación con JDBC.
 * 2. NO modifique las pruebas (src/test). Las pruebas NO necesitan MySQL: usan una base de datos H2 en memoria.
 * 3. Esta etapa usa las anteriores: para guardar recorra arbol.miembros(); para cargar, reconstruya el árbol con
 *    new ArbolGenealogico(sujeto) y arbol.agregar(...).
 *
 * REGLAS (las pruebas las verifican)
 *  - Los métodos NO lanzan SQLException: la capturan, imprimen el mensaje con System.out.println y devuelven
 *    un valor "de error" (-1, false, lista vacía o null).
 *  - Use SIEMPRE PreparedStatement con parámetros (?), nunca concatene datos dentro del SQL.
 *  - Guardar un árbol son VARIAS sentencias (el árbol y todos sus miembros): hágalo en una TRANSACCIÓN
 *    (con.setAutoCommit(false); ... con.commit(); y con.rollback() si algo falla) para que nunca quede
 *    guardado "a medias". Antes de cerrar la conexión vuelva a dejar setAutoCommit(true).
 *  - El id de cada fila lo genera la base de datos (AUTO_INCREMENT).
 *  - Los miembros se guardan en el orden de arbol.miembros(): así, al leerlos ORDER BY id, cada persona
 *    aparece después de la que necesita para poder agregarla al árbol.
 */
public class ArbolDAO {

    // =====================================================================
    // ETAPA 4.1. Guardar un árbol nuevo (INSERT en dos tablas, con transacción)
    // =====================================================================

    /**
     * Guarda un árbol nuevo: una fila en "arboles" (con el nombre) y una fila en "miembros" por cada persona.
     *
     * @return el id que la base de datos le dio al árbol; -1 si el nombre es nulo, está en blanco o tiene más
     *         de 100 caracteres, si el árbol es null, o si hubo un error (en ese caso NO queda nada guardado)
     */
    public long guardar(String nombre, ArbolGenealogico arbol) {
        // TODO Etapa 4.1: valide el nombre; abra la conexión y desactive el auto-commit;
        //   INSERT INTO arboles (nombre) VALUES (?)  -> use prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        //   y getGeneratedKeys() para leer el id;
        //   luego, por cada miembro: INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento)
        //   VALUES (?, ?, ?, ?, ?)  (fallecimiento null -> ps.setNull(5, java.sql.Types.INTEGER));
        //   commit; si hay un SQLException: rollback y devuelva -1.
        if (nombre == null || nombre.isBlank() || nombre.length() > 100 || arbol == null) {
            return -1;
        }
        return enTransaccion(con -> {
            long id;
            String sql = "INSERT INTO arboles (nombre) VALUES (?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre);
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (!claves.next()) {
                        throw new SQLException("La base de datos no devolvió el ID del árbol.");
                    }
                    id = claves.getLong(1);
                }
            }
            insertarMiembros(con, id, arbol);
            return id;
        }, -1L);
    }

    // =====================================================================
    // ETAPA 4.2. Listar (SELECT con JOIN y COUNT)
    // =====================================================================

    /**
     * @return un resumen por cada árbol guardado (id, nombre, cuántos miembros tiene y cuándo se creó),
     *         ordenados por id; lista vacía si no hay árboles o si hay un error
     */
    public List<ResumenArbol> listar() {
        // TODO Etapa 4.2: SELECT a.id, a.nombre, a.creado, COUNT(m.id) AS total
        //                 FROM arboles a LEFT JOIN miembros m ON m.arbol_id = a.id
        //                 GROUP BY a.id, a.nombre, a.creado ORDER BY a.id
        //   (el LEFT JOIN hace que aparezcan también los árboles sin miembros, con total 0).
        //   Para la fecha use rs.getTimestamp("creado").toLocalDateTime().
        String sql = """
                SELECT a.id, a.nombre, a.creado, COUNT(m.id) AS total
                FROM arboles a LEFT JOIN miembros m ON m.arbol_id = a.id
                GROUP BY a.id, a.nombre, a.creado ORDER BY a.id
                """;
        List<ResumenArbol> arboles = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet filas = ps.executeQuery()) {
            while (filas.next()) {
                arboles.add(new ResumenArbol(filas.getLong("id"), filas.getString("nombre"),
                        filas.getInt("total"), filas.getTimestamp("creado").toLocalDateTime()));
            }
            return arboles;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ArrayList<>();
        }
    }

    // =====================================================================
    // ETAPA 4.3. Cargar un árbol (SELECT y reconstrucción del árbol)
    // =====================================================================

    /**
     * @return el árbol con ese id, reconstruido con todos sus miembros; null si no existe ese id o si hay un error
     */
    public ArbolGenealogico cargar(long id) {
        // TODO Etapa 4.3: SELECT posicion, nombre, nacimiento, fallecimiento FROM miembros
        //                 WHERE arbol_id = ? ORDER BY id
        //   La primera fila es el sujeto: new ArbolGenealogico(sujeto); las demás: arbol.agregar(persona).
        //   Para leer un entero que puede ser NULL: int f = rs.getInt("fallecimiento"); y luego rs.wasNull().
        //   Si no hay filas, el árbol no existe (devuelva null).
        String sql = """
                SELECT posicion, nombre, nacimiento, fallecimiento
                FROM miembros WHERE arbol_id = ? ORDER BY id
                """;
        try (Connection con = ConexionBD.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet filas = ps.executeQuery()) {
                if (!filas.next()) {
                    return null;
                }
                ArbolGenealogico arbol = new ArbolGenealogico(leerPersona(filas));
                while (filas.next()) {
                    arbol.agregar(leerPersona(filas));
                }
                return arbol;
            }
        } catch (SQLException | IllegalArgumentException error) {
            System.out.println(error.getMessage());
            return null;
        }
    }

    // =====================================================================
    // ETAPA 4.4. Actualizar (reemplazar los miembros de un árbol, con transacción)
    // =====================================================================

    /**
     * Guarda los cambios de un árbol ya existente (por ejemplo, después de agregar o eliminar miembros):
     * deja en la base de datos exactamente los miembros de {@code arbol}. El nombre del árbol NO cambia.
     *
     * @return true si se actualizó; false si no existe un árbol con ese id, si el árbol es null o si hubo un
     *         error (en ese caso NO se modifica nada: ni se borran los miembros anteriores)
     */
    public boolean actualizar(long id, ArbolGenealogico arbol) {
        // TODO Etapa 4.4: en una transacción: compruebe que el árbol existe (SELECT id FROM arboles WHERE id = ?),
        //   DELETE FROM miembros WHERE arbol_id = ?  y vuelva a insertar los miembros de arbol.miembros().
        if (arbol == null) {
            return false;
        }
        return enTransaccion(con -> {
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM arboles WHERE id = ? FOR UPDATE")) {
                ps.setLong(1, id);
                try (ResultSet filas = ps.executeQuery()) {
                    if (!filas.next()) {
                        return false;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM miembros WHERE arbol_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            insertarMiembros(con, id, arbol);
            return true;
        }, false);
    }

    // =====================================================================
    // ETAPA 4.5. Eliminar (DELETE)
    // =====================================================================

    /**
     * Elimina el árbol y todos sus miembros (la base de datos borra los miembros sola: ON DELETE CASCADE).
     *
     * @return true si se eliminó; false si no existía ese id o si hubo un error
     */
    public boolean eliminar(long id) {
        // TODO Etapa 4.5: DELETE FROM arboles WHERE id = ?
        try (Connection con = ConexionBD.conectar();
                PreparedStatement ps = con.prepareStatement("DELETE FROM arboles WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return false;
        }
    }

    private static Persona leerPersona(ResultSet fila) throws SQLException {
        int valor = fila.getInt("fallecimiento");
        Integer fallecimiento = fila.wasNull() ? null : valor;
        return new Persona(fila.getString("posicion"), fila.getString("nombre"),
                fila.getInt("nacimiento"), fallecimiento);
    }

    private static void insertarMiembros(Connection con, long id, ArbolGenealogico arbol) throws SQLException {
        String sql = """
                INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Persona persona : arbol.miembros()) {
                ps.setLong(1, id);
                ps.setString(2, persona.getPosicion());
                ps.setString(3, persona.getNombre());
                ps.setInt(4, persona.getNacimiento());
                Integer fallecimiento = persona.getFallecimiento();
                if (fallecimiento == null) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, fallecimiento);
                }
                ps.executeUpdate();
            }
        }
    }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar(Connection con) throws SQLException;
    }

    private static <T> T enTransaccion(Operacion<T> operacion, T valorDeError) {
        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(con);
                con.commit();
                con.setAutoCommit(true);
                return resultado;
            } catch (SQLException | RuntimeException error) {
                try {
                    con.rollback();
                } catch (SQLException falloRollback) {
                    error.addSuppressed(falloRollback);
                    throw error;
                }
                con.setAutoCommit(true);
                throw error;
            }
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return valorDeError;
        }
    }
}
