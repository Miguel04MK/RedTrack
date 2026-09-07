package com.miguelalvarez.redtrack.dominio.modelo;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Una tecnologia del perfil, con su nivel de dominio y su peso relativo.
 *
 * @param alias variantes con las que aparece escrita en las ofertas
 *              ("springboot", "spring-boot", "java 21"...)
 */
public record Tecnologia(
        String nombre,
        Nivel nivel,
        int peso,
        List<String> alias
) {

    public Tecnologia {
        alias = alias == null ? List.of() : List.copyOf(alias);
    }

    /** El nombre mas todos sus alias, en minusculas y sin repetidos. */
    public List<String> todasLasFormas() {
        return Stream.concat(Stream.of(nombre), alias.stream())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    /** Peso declarado ponderado por el nivel de dominio. */
    public double pesoEfectivo() {
        return peso * nivel.factor();
    }
}
