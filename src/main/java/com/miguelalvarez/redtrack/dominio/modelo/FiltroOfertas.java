package com.miguelalvarez.redtrack.dominio.modelo;

import java.time.LocalDate;

/**
 * Criterios para consultar lo ya guardado.
 *
 * <p>Todos los campos son opcionales: un {@code null} significa "no filtres por
 * esto".
 */
public record FiltroOfertas(
        Integer minEncaje,
        Modalidad modalidad,
        LocalDate desde,
        String fuente,
        int limite
) {

    /** Tope duro, para que nadie se traiga la tabla entera de un GET. */
    public static final int LIMITE_MAXIMO = 200;

    public FiltroOfertas {
        limite = limite <= 0 ? 50 : Math.min(limite, LIMITE_MAXIMO);
    }

    public static FiltroOfertas todo() {
        return new FiltroOfertas(null, null, null, null, 50);
    }
}
