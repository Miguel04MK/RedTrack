package com.miguelalvarez.redtrack.dominio.modelo;

/**
 * Senal de seniority detectada en el titulo o la descripcion.
 *
 * <p>Ademas de puntuar, es la salvaguarda del deduplicador: dos ofertas con
 * seniority distinta NO son la misma aunque sus titulos se parezcan al 93%.
 */
public enum Seniority {

    JUNIOR(1.00),

    /**
     * La mayoria de las ofertas. No decir nada es mejor senal que decir MID:
     * una vacante que no se pronuncia suele estar abierta a perfiles junior.
     */
    NO_DICE(0.92),

    /**
     * Etiqueta vaga: cada empresa la usa a su manera. Por eso no hunde la
     * oferta por si sola. Quien decide de verdad son los anos requeridos, que
     * entran como un segundo factor.
     */
    MID(0.75),

    SENIOR(0.18);

    private final double factor;

    Seniority(double factor) {
        this.factor = factor;
    }

    /**
     * Multiplicador que se aplica al encaje.
     *
     * <p>Multiplica en vez de sumar a proposito. Un bloque que suma siempre lo
     * pueden compensar los demas: con 20 puntos de seniority, una oferta senior
     * que encaje bien en tecnologias, ubicacion y salario seguia superando el
     * umbral. Y una oferta senior no es una oferta un poco peor para un junior:
     * es una oferta que no sirve.
     *
     * <p>Este factor es solo la mitad de la accesibilidad: se multiplica ademas
     * por el de los anos requeridos. La etiqueta es un proxy de los anos, asi
     * que cuando la oferta dice los anos, mandan los anos.
     */
    public double factor() {
        return factor;
    }
}
