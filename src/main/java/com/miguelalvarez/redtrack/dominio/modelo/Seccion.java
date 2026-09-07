package com.miguelalvarez.redtrack.dominio.modelo;

/**
 * En que parte del resumen diario entra una oferta.
 */
public enum Seccion {

    /** Supera el umbral de encaje: es tu stack y puedes optar. */
    PARA_TI,

    /**
     * No es tu stack, pero es un puesto junior de desarrollo al que si puedes
     * optar. Existe porque el mercado junior de un stack concreto se seca
     * semanas enteras, y un resumen que no llega es indistinguible de uno roto.
     */
    PODRIA_INTERESARTE,

    /** No entra en el resumen. */
    NINGUNA
}
