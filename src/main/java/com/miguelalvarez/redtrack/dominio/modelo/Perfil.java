package com.miguelalvarez.redtrack.dominio.modelo;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Perfil profesional contra el que se puntuan las ofertas.
 *
 * <p>Vive fuera del codigo, en perfil.yaml, para poder tocarlo sin recompilar.
 */
public record Perfil(
        List<Tecnologia> tecnologias,
        Preferencias preferencias
) {

    public Perfil {
        tecnologias = tecnologias == null ? List.of() : List.copyOf(tecnologias);
    }

    /** Busca una tecnologia del perfil por nombre, ignorando mayusculas. */
    public Optional<Tecnologia> buscar(String nombre) {
        String objetivo = nombre.toLowerCase(Locale.ROOT);
        return tecnologias.stream()
                .filter(t -> t.nombre().toLowerCase(Locale.ROOT).equals(objetivo))
                .findFirst();
    }

    /** Tecnologias que el perfil declara dominar en algun grado. */
    public List<Tecnologia> dominadas() {
        return tecnologias.stream()
                .filter(t -> t.nivel() != Nivel.NINGUNO)
                .toList();
    }
}
