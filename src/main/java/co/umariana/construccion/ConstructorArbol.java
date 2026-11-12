package co.umariana.construccion;

import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;

/**
 * Construye un árbol genealógico a partir de un texto: cada línea es una persona.
 *
 *     posicion;nombre;nacimiento;fallecimiento
 *
 * Ejemplo (el fallecimiento puede quedar vacío):
 *
 *     1;John Doe;2002;
 *     2;Henry Doe;1920;1990
 *     3;Maria Doe;1921;
 *     1.1;Timmy Doe;2021;
 *     1.2;Amy Doe;2026;
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 3)
 * ------------------------------------------
 * Reemplace el "throw new UnsupportedOperationException" por su implementación. Esta clase usa lo que ya
 * hizo en las etapas anteriores (Posicion, Persona y ArbolGenealogico). NO modifique las pruebas (src/test).
 *
 * REGLAS
 *   1. Si el texto es null o no tiene ninguna línea con contenido: ArbolInvalidoException (mensaje: "El texto está vacío...").
 *   2. Las líneas pueden terminar en \n o en \r\n. Las líneas en blanco se ignoran, pero los números de línea
 *      que se informan cuentan TODAS las líneas del texto (la primera es la línea 1).
 *   3. Cada línea tiene 4 campos separados por ';' (el cuarto, el fallecimiento, puede estar vacío). Se acepta
 *      también la línea con solo 3 campos y sin el último ';'. Los campos se recortan (trim).
 *      Si hay menos de 3 o más de 4 campos: error.
 *   4. El nacimiento y el fallecimiento deben ser números enteros (el fallecimiento vacío es "no tiene").
 *   5. Cada persona debe ser válida (Persona.validar) y no puede haber dos líneas con la misma posición.
 *   6. Debe existir la persona "1" (el sujeto), pero puede estar en cualquier línea.
 *   7. Las líneas pueden venir en CUALQUIER orden. El constructor debe agregarlas al árbol en un orden en el
 *      que cada persona ya tenga a la que necesita (por ejemplo, primero los ascendientes por número Ahnentafel
 *      ascendente y los descendientes por generación). Las reglas del árbol (ver ArbolGenealogico) las valida el árbol.
 *   8. Todos los errores asociados a una línea se informan con ArbolInvalidoException y un mensaje que EMPIEZA con
 *      "Línea N: " (N es el número de línea de la persona con el problema), por ejemplo:
 *          "Línea 3: la posición 2 está repetida."
 *      Los errores que no son de una línea (texto vacío, falta el sujeto) no llevan ese prefijo.
 *   9. Solo se muestran los miembros que están en el texto: no se crean personas vacías para los que faltan.
 */
public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    /**
     * Convierte el texto en un árbol (ver las reglas arriba).
     *
     * @throws ArbolInvalidoException si el texto no cumple las reglas
     */
    public static ArbolGenealogico construir(String texto) {
        // TODO Etapa 3
        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException("El texto está vacío.");
        }

        List<Entrada> familiares = new ArrayList<>();
        Set<String> posiciones = new HashSet<>();
        Persona sujeto = null;
        String[] lineas = texto.split("\\r?\\n");
        for (int i = 0; i < lineas.length; i++) {
            if (lineas[i].isBlank()) {
                continue;
            }
            try {
                Persona persona = leerPersona(lineas[i]);
                String posicion = persona.getPosicion();
                if (!posiciones.add(posicion)) {
                    throw new ArbolInvalidoException("La posición " + posicion + " está repetida.");
                }
                if (Posicion.esSujeto(posicion)) {
                    sujeto = persona;
                } else {
                    familiares.add(new Entrada(i + 1, persona));
                }
            } catch (IllegalArgumentException error) {
                throw errorDeLinea(i + 1, error);
            }
        }
        if (sujeto == null) {
            throw new ArbolInvalidoException("Falta el sujeto en la posición 1.");
        }

        familiares.sort(Comparator.comparingInt(entrada ->
                Posicion.generacion(entrada.persona().getPosicion())));
        ArbolGenealogico arbol = new ArbolGenealogico(sujeto);
        for (Entrada entrada : familiares) {
            try {
                arbol.agregar(entrada.persona());
            } catch (IllegalArgumentException error) {
                throw errorDeLinea(entrada.linea(), error);
            }
        }
        return arbol;
    }

    private record Entrada(int linea, Persona persona) {}

    private static Persona leerPersona(String linea) {
        String[] campos = linea.split(";", -1);
        if (campos.length < 3 || campos.length > 4) {
            throw new ArbolInvalidoException("Se esperaban 3 o 4 campos separados por punto y coma.");
        }
        int nacimiento = leerAnio(campos[2].trim(), "nacimiento");
        Integer fallecimiento = null;
        if (campos.length == 4 && !campos[3].trim().isEmpty()) {
            fallecimiento = leerAnio(campos[3].trim(), "fallecimiento");
        }
        Persona persona = new Persona(campos[0].trim(), campos[1].trim(), nacimiento, fallecimiento);
        persona.validar();
        return persona;
    }

    private static int leerAnio(String valor, String campo) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException error) {
            throw new ArbolInvalidoException("El año de " + campo + " debe ser un entero.");
        }
    }

    private static ArbolInvalidoException errorDeLinea(int linea, IllegalArgumentException error) {
        return new ArbolInvalidoException("Línea " + linea + ": " + error.getMessage());
    }
}
