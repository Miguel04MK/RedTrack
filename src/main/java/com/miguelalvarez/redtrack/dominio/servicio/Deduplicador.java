package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.Seniority;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

/**
 * El problema tecnico bonito: la misma oferta aparece en tres sitios con titulos
 * ligeramente distintos.
 *
 * <p>Dos pasos:
 * <ol>
 *   <li>Huella exacta (la calcula {@link Normalizador}). Pilla la mayoria.</li>
 *   <li>Similitud de Jaro-Winkler entre titulos, SOLO si empresa y ubicacion
 *       coinciden.</li>
 * </ol>
 *
 * <p>Y una salvaguarda que es lo mas importante de esta clase:
 *
 * <pre>
 *   "Desarrollador Java Junior" vs "Desarrollador/a Java Junior" -> 0.97 duplicado
 *   "Desarrollador Java Junior" vs "Desarrollador Java Senior"   -> 0.93 NO duplicado
 * </pre>
 *
 * <p>El segundo caso es la trampa: si las senales de seniority DIFIEREN no son la
 * misma oferta, aunque el texto se parezca al 93%.
 */
public final class Deduplicador {

    /** Por encima de esto dos titulos se consideran el mismo puesto. */
    public static final double UMBRAL_SIMILITUD = 0.90;

    private static final JaroWinklerSimilarity SIMILITUD = new JaroWinklerSimilarity();

    private final Normalizador normalizador;
    private final ExtractorSenales extractor;

    public Deduplicador(Normalizador normalizador, ExtractorSenales extractor) {
        this.normalizador = normalizador;
        this.extractor = extractor;
    }

    /** true si {@code candidata} es la misma oferta que {@code conocida}. */
    public boolean sonLaMisma(Oferta candidata, Oferta conocida) {
        if (candidata == null || conocida == null) {
            return false;
        }

        // Paso 1: huella exacta.
        String huellaA = candidata.huella() != null
                ? candidata.huella() : normalizador.huellaDe(candidata);
        String huellaB = conocida.huella() != null
                ? conocida.huella() : normalizador.huellaDe(conocida);
        if (huellaA.equals(huellaB)) {
            return true;
        }

        // Paso 2: solo tiene sentido comparar titulos si es la misma empresa
        // en el mismo sitio.
        if (!mismaEmpresaYUbicacion(candidata, conocida)) {
            return false;
        }

        // Salvaguarda: seniority distinta -> puestos distintos, se parezcan lo
        // que se parezcan.
        if (seniorityDifiere(candidata, conocida)) {
            return false;
        }

        return similitudDeTitulos(candidata, conocida) > UMBRAL_SIMILITUD;
    }

    /** Jaro-Winkler entre los titulos normalizados. Expuesto para los tests. */
    public double similitudDeTitulos(Oferta a, Oferta b) {
        Double resultado = SIMILITUD.apply(
                normalizador.normalizar(a.titulo()),
                normalizador.normalizar(b.titulo()));
        return resultado == null ? 0.0 : resultado;
    }

    private boolean mismaEmpresaYUbicacion(Oferta a, Oferta b) {
        return normalizador.normalizar(a.empresa()).equals(normalizador.normalizar(b.empresa()))
                && normalizador.normalizar(a.ubicacion()).equals(normalizador.normalizar(b.ubicacion()));
    }

    private boolean seniorityDifiere(Oferta a, Oferta b) {
        Seniority sa = extractor.seniority(a.titulo());
        Seniority sb = extractor.seniority(b.titulo());
        // NO_DICE no contradice a nada: solo bloquea cuando ambas se pronuncian
        // y dicen cosas distintas.
        if (sa == Seniority.NO_DICE || sb == Seniority.NO_DICE) {
            return false;
        }
        return sa != sb;
    }
}
