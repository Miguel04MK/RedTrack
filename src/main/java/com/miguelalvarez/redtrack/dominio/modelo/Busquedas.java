package com.miguelalvarez.redtrack.dominio.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Que se busca. Forma parte del perfil, no de la configuracion tecnica.
 *
 * <p>Estaba en {@code application.yml} y era una incoherencia: <i>que busco</i>
 * es tan personal como <i>que se hacer</i>. Quien clonase el proyecto tenia que
 * tocar dos sitios, y uno de ellos era un fichero de configuracion de Spring.
 *
 * @param terminos              el stack propio, para la seccion "para ti"
 * @param terminosExploratorios puestos junior de desarrollo de cualquier stack,
 *                              para la seccion "podrian interesarte". Tienen que
 *                              ser especificos: buscar "junior" a secas devuelve
 *                              comerciales y practicas de administracion.
 */
public record Busquedas(
        List<String> terminos,
        List<String> terminosExploratorios,
        List<String> ubicaciones,
        int maxDiasAntiguedad,
        int maxResultadosPorFuente
) {

    public Busquedas {
        terminos = terminos == null ? List.of() : List.copyOf(terminos);
        terminosExploratorios = terminosExploratorios == null
                ? List.of() : List.copyOf(terminosExploratorios);
        ubicaciones = ubicaciones == null ? List.of() : List.copyOf(ubicaciones);
        maxDiasAntiguedad = maxDiasAntiguedad <= 0 ? 7 : maxDiasAntiguedad;
        maxResultadosPorFuente = maxResultadosPorFuente <= 0 ? 50 : maxResultadosPorFuente;
    }

    public static Busquedas vacias() {
        return new Busquedas(List.of(), List.of(), List.of(), 7, 50);
    }

    /**
     * Producto cartesiano de terminos por ubicaciones.
     *
     * <p>Los exploratorios van en la misma lista: una oferta no sabe a que
     * seccion pertenece, eso lo decide el clasificador despues, sobre la oferta
     * ya normalizada y puntuada.
     */
    public List<CriterioBusqueda> criterios() {
        List<String> todosLosTerminos = new ArrayList<>(terminos);
        todosLosTerminos.addAll(terminosExploratorios);

        List<CriterioBusqueda> criterios = new ArrayList<>();
        for (String termino : todosLosTerminos) {
            for (String ubicacion : ubicaciones) {
                criterios.add(new CriterioBusqueda(
                        termino, ubicacion, maxDiasAntiguedad, maxResultadosPorFuente));
            }
        }
        return criterios;
    }
}
