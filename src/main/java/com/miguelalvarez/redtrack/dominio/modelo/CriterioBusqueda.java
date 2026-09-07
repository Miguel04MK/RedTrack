package com.miguelalvarez.redtrack.dominio.modelo;

/**
 * Criterio con el que se interroga a una fuente.
 *
 * <p>Cada adaptador lo traduce a los parametros que entienda su API. Los campos
 * que una fuente no soporte se ignoran: las de empleo remoto, por ejemplo, no
 * tienen concepto de {@code donde}.
 *
 * @param maxDiasAntiguedad no traer ofertas mas viejas que esto
 */
public record CriterioBusqueda(
        String que,
        String donde,
        int maxDiasAntiguedad,
        int maxResultados
) {

    public static CriterioBusqueda de(String que, String donde) {
        return new CriterioBusqueda(que, donde, 7, 50);
    }
}
