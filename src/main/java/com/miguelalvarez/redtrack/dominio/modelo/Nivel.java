package com.miguelalvarez.redtrack.dominio.modelo;

/**
 * Nivel de dominio de una tecnologia del perfil.
 *
 * <p>El factor multiplica el peso de la tecnologia al calcular el bloque de
 * 50 puntos de encaje tecnologico.
 */
public enum Nivel {
    ALTO(1.0),
    MEDIO(0.7),
    BAJO(0.3),
    NINGUNO(0.0);

    private final double factor;

    Nivel(double factor) {
        this.factor = factor;
    }

    public double factor() {
        return factor;
    }
}
