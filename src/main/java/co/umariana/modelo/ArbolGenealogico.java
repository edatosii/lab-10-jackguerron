package co.umariana.modelo;

import java.util.ArrayList;
import java.util.ArrayDeque;

import java.util.List;

/**
 * El árbol genealógico: un SUJETO (posición "1") con sus ASCENDIENTES (padres, abuelos... en Ahnentafel)
 * y sus DESCENDIENTES (hijos, nietos... en d'Aboville). Ver la clase Posicion.
 *
 * Son en realidad dos árboles unidos en la raíz:
 *
 *     ascendientes:  árbol BINARIO. Cada persona tiene como mucho un padre (2n) y una madre (2n + 1).
 *     descendientes: árbol GENERAL. Cada persona tiene una lista de hijos (1.1, 1.2, ...) ordenada por número.
 *
 * El árbol solo guarda a los miembros que EXISTEN: no hay "huecos" ni personas vacías. Puede faltar el padre
 * de alguien, o el hijo 1.1 aunque exista el 1.2.
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 2)
 * ------------------------------------------
 * 1. Implemente el árbol con sus PROPIOS NODOS: una clase interna "Nodo" con la Persona, el nodo del
 *    padre, el nodo de la madre y la lista de hijos. NO se permite usar Map ni Set (las pruebas lo
 *    revisan): para buscar una posición recorra el árbol siguiendo su camino
 *      - Ahnentafel: los bits de n después del primer 1 dicen el camino (0 = padre, 1 = madre).
 *        Ej.: 5 = 101 -> desde el sujeto: padre (0), luego madre (1).
 *      - D'Aboville: cada número de la posición dice a qué hijo bajar. Ej.: 1.2.3 -> hijo 2, luego su hijo 3.
 *    Se permite usar List/ArrayList para la lista de hijos.
 * 2. Todos los errores de las reglas se informan con {@link ArbolInvalidoException} (un mensaje claro).
 *    Los datos inválidos de una Persona los detecta Persona.validar() (IllegalArgumentException).
 * 3. NO modifique las pruebas (src/test).
 *
 * REGLAS DEL ÁRBOL
 *   - La posición "1" (el sujeto) existe siempre y no se puede eliminar.
 *   - Una posición no puede repetirse.
 *   - Un ascendiente n solo se puede agregar si ya existe su hijo n / 2, y debe haber NACIDO ANTES
 *     (año de nacimiento menor) que ese hijo.
 *   - Un descendiente solo se puede agregar si ya existe su progenitor, y debe haber NACIDO DESPUÉS
 *     (año de nacimiento mayor) que su progenitor.
 */
public class ArbolGenealogico {

    // TODO Etapa 2: declare la clase interna Nodo y los atributos necesarios (raíz, cantidad de miembros...).

    /**
     * Crea un árbol que solo tiene al sujeto.
     *
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si su posición no es "1"
     */
    public ArbolGenealogico(Persona sujeto) {
        // TODO Etapa 2
        validar(sujeto);
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("El sujeto debe estar en la posición 1.");
        }
        raiz = new Nodo(sujeto);
    }

    /** @return el sujeto (la persona de la posición "1") */
    public Persona sujeto() {
        // TODO Etapa 2
        return raiz.persona;
    }

    /** @return cuántos miembros tiene el árbol (el sujeto cuenta; un árbol nuevo tiene 1) */
    public int tamano() {
        // TODO Etapa 2
        return cantidad;
    }

    /**
     * @return la persona de esa posición, o null si no existe en el árbol o si la posición no es válida
     */
    public Persona buscar(String posicion) {
        // TODO Etapa 2
        Nodo nodo = buscarNodo(posicion);
        return nodo == null ? null : nodo.persona;
    }

    /** @return true si hay alguien en esa posición (false también si la posición no es válida) */
    public boolean contiene(String posicion) {
        // TODO Etapa 2
        return buscarNodo(posicion) != null;
    }

    /**
     * Agrega una persona en la posición que ella misma trae (la usa el constructor del árbol a partir del texto).
     * El orden de llamada importa: primero debe existir la persona de la que depende (ver REGLAS DEL ÁRBOL).
     *
     * @return la misma persona
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si la posición ya existe (o es "1"), si falta la persona de la que depende,
     *         o si los años de nacimiento no cumplen las reglas
     */
    public Persona agregar(Persona persona) {
        // TODO Etapa 2
        validar(persona);
        String posicion = persona.getPosicion();
        if (contiene(posicion)) {
            throw new ArbolInvalidoException("La posición " + posicion + " está repetida.");
        }

        Nodo nuevo = new Nodo(persona);
        if (Posicion.esAscendiente(posicion)) {
            Nodo hijo = exigirNodo(Posicion.posicionDelHijoEnLinea(posicion));
            if (persona.getNacimiento() >= hijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El ascendiente debe haber nacido antes que su hijo.");
            }
            if (Integer.parseInt(posicion) % 2 == 0) {
                hijo.padre = nuevo;
            } else {
                hijo.madre = nuevo;
            }
        } else {
            Nodo progenitor = exigirNodo(Posicion.posicionDelProgenitor(posicion));
            if (persona.getNacimiento() <= progenitor.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El descendiente debe haber nacido después de su progenitor.");
            }
            int orden = Posicion.orden(posicion);
            int indice = 0;
            while (indice < progenitor.hijos.size()
                    && Posicion.orden(progenitor.hijos.get(indice).persona.getPosicion()) < orden) {
                indice++;
            }
            progenitor.hijos.add(indice, nuevo);
        }
        cantidad++;
        return persona;
    }

    /**
     * Agrega el PADRE de la persona de esa posición, en la posición 2n (ver Posicion.posicionDelPadre).
     * Solo se pueden agregar padres al sujeto y a sus ascendientes.
     *
     * @return la persona creada
     * @throws ArbolInvalidoException si la posición no existe, si no es el sujeto ni un ascendiente, si ese padre
     *         ya existe o si no cumple las reglas del árbol
     * @throws IllegalArgumentException si los datos de la persona no son válidos
     */
    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        // TODO Etapa 2
        comprobarLineaAscendente(posicion);
        return agregarEn(Posicion.posicionDelPadre(posicion), nombre, nacimiento, fallecimiento);
    }

    /**
     * Igual que agregarPadre, pero agrega la MADRE (posición 2n + 1).
     */
    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        // TODO Etapa 2
        comprobarLineaAscendente(posicion);
        return agregarEn(Posicion.posicionDeLaMadre(posicion), nombre, nacimiento, fallecimiento);
    }

    /**
     * Agrega un HIJO a la persona de esa posición. Solo se pueden agregar hijos al sujeto y a sus descendientes.
     * Su posición es la del progenitor + "." + el número siguiente al MAYOR número de hijo que ya exista
     * (o 1 si todavía no tiene hijos). Ej.: si "1" tiene los hijos 1.1 y 1.3, el nuevo es 1.4.
     *
     * @return la persona creada
     * @throws ArbolInvalidoException si la posición no existe, si no es el sujeto ni un descendiente, o si no cumple
     *         las reglas del árbol
     * @throws IllegalArgumentException si los datos de la persona no son válidos
     */
    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        // TODO Etapa 2
        if (!Posicion.esSujeto(posicion) && !Posicion.esDescendiente(posicion)) {
            throw new ArbolInvalidoException("La posición debe ser el sujeto o un descendiente.");
        }
        List<Nodo> hijos = exigirNodo(posicion).hijos;
        int orden = hijos.isEmpty() ? 1
                : Posicion.orden(hijos.get(hijos.size() - 1).persona.getPosicion()) + 1;
        return agregarEn(posicion + "." + orden, nombre, nacimiento, fallecimiento);
    }

    /**
     * Elimina a la persona de esa posición y TODO lo que depende de ella:
     *   - si es un ascendiente: ella y todos sus propios ascendientes (sus padres, abuelos...);
     *   - si es un descendiente: ella y todos sus propios descendientes (hijos, nietos...).
     * Las demás posiciones NO cambian de número.
     *
     * @return cuántas personas se eliminaron (ella incluida)
     * @throws ArbolInvalidoException si la posición es "1" (el sujeto no se puede eliminar) o si no existe
     */
    public int eliminar(String posicion) {
        // TODO Etapa 2
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto no se puede eliminar.");
        }
        Nodo nodo = exigirNodo(posicion);
        if (Posicion.esAscendiente(posicion)) {
            Nodo hijo = exigirNodo(Posicion.posicionDelHijoEnLinea(posicion));
            if (hijo.padre == nodo) {
                hijo.padre = null;
            } else {
                hijo.madre = null;
            }
        } else {
            exigirNodo(Posicion.posicionDelProgenitor(posicion)).hijos.remove(nodo);
        }
        int eliminados = contar(nodo);
        cantidad -= eliminados;
        return eliminados;
    }

    /**
     * @return todos los miembros en este orden: el sujeto; luego los ascendientes por número Ahnentafel
     *         ascendente (2, 3, 4, 5...); luego los descendientes en PREORDEN (cada persona y después sus hijos:
     *         1.1, 1.1.1, 1.1.2, 1.2, ...). La lista es una copia nueva: modificarla no cambia el árbol.
     *         Este orden garantiza que cada persona aparece DESPUÉS de la que necesita (útil para guardar y cargar).
     */
    public List<Persona> miembros() {
        // TODO Etapa 2
        List<Persona> resultado = new ArrayList<>();
        resultado.add(sujeto());
        resultado.addAll(ascendientes());
        resultado.addAll(descendientes());
        return resultado;
    }

    /** @return solo los ascendientes, por número Ahnentafel ascendente (sin el sujeto) */
    public List<Persona> ascendientes() {
        // TODO Etapa 2
        List<Persona> resultado = new ArrayList<>();
        ArrayDeque<Nodo> pendientes = new ArrayDeque<>();
        pendientes.add(raiz);
        while (!pendientes.isEmpty()) {
            Nodo nodo = pendientes.remove();
            if (nodo != raiz) {
                resultado.add(nodo.persona);
            }
            if (nodo.padre != null) {
                pendientes.add(nodo.padre);
            }
            if (nodo.madre != null) {
                pendientes.add(nodo.madre);
            }
        }
        return resultado;
    }

    /** @return solo los descendientes, en preorden (sin el sujeto) */
    public List<Persona> descendientes() {
        // TODO Etapa 2
        List<Persona> resultado = new ArrayList<>();
        recorrerHijos(raiz, resultado);
        return resultado;
    }

    private static class Nodo {
        private final Persona persona;
        private Nodo padre;
        private Nodo madre;
        private final List<Nodo> hijos = new ArrayList<>();

        private Nodo(Persona persona) {
            this.persona = persona;
        }
    }

    private final Nodo raiz;
    private int cantidad = 1;

    private static void validar(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona es obligatoria.");
        }
        persona.validar();
    }

    private Nodo buscarNodo(String posicion) {
        Nodo nodo = raiz;
        if (Posicion.esAhnentafel(posicion)) {
            String camino = Integer.toBinaryString(Integer.parseInt(posicion));
            for (int i = 1; i < camino.length() && nodo != null; i++) {
                nodo = camino.charAt(i) == '0' ? nodo.padre : nodo.madre;
            }
            return nodo;
        }
        if (!Posicion.esDAboville(posicion)) {
            return null;
        }

        String[] niveles = posicion.split("\\.");
        for (int i = 1; i < niveles.length && nodo != null; i++) {
            nodo = buscarHijo(nodo, Integer.parseInt(niveles[i]));
        }
        return nodo;
    }

    private static Nodo buscarHijo(Nodo progenitor, int orden) {
        for (Nodo hijo : progenitor.hijos) {
            if (Posicion.orden(hijo.persona.getPosicion()) == orden) {
                return hijo;
            }
        }
        return null;
    }

    private Nodo exigirNodo(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("No existe la posición " + posicion + ".");
        }
        return nodo;
    }

    private Persona agregarEn(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!Posicion.esValida(posicion)) {
            throw new ArbolInvalidoException("La posición excede los límites: " + posicion);
        }
        return agregar(new Persona(posicion, nombre, nacimiento, fallecimiento));
    }

    private void comprobarLineaAscendente(String posicion) {
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("La posición debe ser el sujeto o un ascendiente.");
        }
        exigirNodo(posicion);
    }

    private static int contar(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        int total = 1 + contar(nodo.padre) + contar(nodo.madre);
        for (Nodo hijo : nodo.hijos) {
            total += contar(hijo);
        }
        return total;
    }

    private static void recorrerHijos(Nodo progenitor, List<Persona> resultado) {
        for (Nodo hijo : progenitor.hijos) {
            resultado.add(hijo.persona);
            recorrerHijos(hijo, resultado);
        }
    }
}
