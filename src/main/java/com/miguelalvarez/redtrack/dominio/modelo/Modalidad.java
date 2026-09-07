package com.miguelalvarez.redtrack.dominio.modelo;

/**
 * Modalidad de trabajo de una oferta.
 *
 * <p>Se deduce del titulo Y de la descripcion. Prioridad al resolver conflictos:
 * si el texto menciona hibrido y remoto a la vez gana HIBRIDO, que es lo que
 * acaba pasando en la practica.
 */
public enum Modalidad {
    PRESENCIAL,
    HIBRIDO,
    REMOTO,
    DESCONOCIDA
}
