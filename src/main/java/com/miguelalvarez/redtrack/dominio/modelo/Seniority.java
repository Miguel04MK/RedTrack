package com.miguelalvarez.redtrack.dominio.modelo;

/**
 * Senal de seniority detectada en el titulo o la descripcion.
 *
 * <p>Ademas de puntuar, es la salvaguarda del deduplicador: dos ofertas con
 * seniority distinta NO son la misma aunque sus titulos se parezcan al 93%.
 */
public enum Seniority {
    JUNIOR,
    MID,
    SENIOR,
    NO_DICE
}
