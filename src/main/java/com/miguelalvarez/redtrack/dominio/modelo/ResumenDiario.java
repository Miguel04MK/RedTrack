package com.miguelalvarez.redtrack.dominio.modelo;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lo que se envia cada manana, en dos secciones.
 *
 * <p>{@code paraTi} son las que superan el umbral de encaje. {@code
 * podrianInteresarte} son puestos junior de desarrollo a los que si puedes
 * optar aunque no sean tu stack: existe porque el mercado junior de un stack
 * concreto se seca semanas enteras, y un resumen que no llega es
 * indistinguible de un resumen roto.
 *
 * <p>Las dos van ordenadas de mayor a menor encaje.
 */
public record ResumenDiario(
        LocalDate fecha,
        List<OfertaAnalizada> paraTi,
        List<OfertaAnalizada> podrianInteresarte
) {

    public ResumenDiario {
        paraTi = ordenadas(paraTi);
        podrianInteresarte = ordenadas(podrianInteresarte);
    }

    private static List<OfertaAnalizada> ordenadas(List<OfertaAnalizada> ofertas) {
        return ofertas == null
                ? List.of()
                : ofertas.stream()
                        .sorted(Comparator.comparingInt(OfertaAnalizada::encaje).reversed())
                        .toList();
    }

    /** Todas las ofertas del resumen, de las dos secciones. */
    public List<OfertaAnalizada> todas() {
        return Stream.concat(paraTi.stream(), podrianInteresarte.stream()).toList();
    }

    /** true si no hay nada que enviar en ninguna de las dos secciones. */
    public boolean estaVacio() {
        return paraTi.isEmpty() && podrianInteresarte.isEmpty();
    }

    public int cuantas() {
        return paraTi.size() + podrianInteresarte.size();
    }
}
