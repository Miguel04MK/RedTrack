package com.miguelalvarez.redtrack.dominio.modelo;

import java.util.List;

/**
 * Preferencias del perfil. Se cargan de perfil.yaml para poder ajustarlas sin
 * recompilar.
 *
 * @param umbralEncaje encaje minimo para entrar en el resumen diario
 */
public record Preferencias(
        List<String> ubicacionesDeseadas,
        int salarioObjetivo,
        String idiomaMaximo,
        List<String> descartarSiTituloContiene,
        int anosMaximosAceptables,
        int umbralEncaje
) {

    public Preferencias {
        ubicacionesDeseadas = ubicacionesDeseadas == null
                ? List.of() : List.copyOf(ubicacionesDeseadas);
        descartarSiTituloContiene = descartarSiTituloContiene == null
                ? List.of() : List.copyOf(descartarSiTituloContiene);
    }
}
