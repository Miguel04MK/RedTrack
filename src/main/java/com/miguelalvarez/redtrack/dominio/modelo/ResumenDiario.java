package com.miguelalvarez.redtrack.dominio.modelo;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Lo que se envia cada mañana: las ofertas nuevas que superan el umbral,
 * ordenadas de mayor a menor encaje.
 */
public record ResumenDiario(
        LocalDate fecha,
        List<OfertaAnalizada> ofertas
) {

    public ResumenDiario {
        ofertas = ofertas == null
                ? List.of()
                : ofertas.stream()
                        .sorted(Comparator.comparingInt(OfertaAnalizada::encaje).reversed())
                        .toList();
    }

    public boolean estaVacio() {
        return ofertas.isEmpty();
    }

    public int cuantas() {
        return ofertas.size();
    }
}
