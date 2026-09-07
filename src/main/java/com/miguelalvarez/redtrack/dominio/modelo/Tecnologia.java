package com.miguelalvarez.redtrack.dominio.modelo;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Una tecnologia del perfil.
 *
 * <p>{@code nivel} y {@code peso} son dos ejes distintos y conviene no
 * mezclarlos: el nivel es cuanto se domina, el peso es cuanto importa que una
 * oferta la pida. Una tecnologia que no se tiene pesa mucho, precisamente
 * porque exigirla deja fuera.
 *
 * @param peso  1 accesorio, 3 troncal. Es el denominador del bloque de
 *              tecnologias del {@code Puntuador}.
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
