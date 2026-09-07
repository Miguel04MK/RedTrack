package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Oferta;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * El trabajo sucio que nadie ensena: dejar los datos de cinco fuentes distintas
 * en una sola forma.
 *
 * <p>Java puro. Sin Spring, sin JPA, sin Jackson.
 */
public final class Normalizador {

    /** Coletillas que ensucian el titulo y rompen la comparacion de huellas. */
    private static final List<Pattern> COLETILLAS = List.of(
            Pattern.compile("\\(m/f/d\\)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\(h/m/x\\)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bref\\.?\\s*\\d+\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(jr|sr)\\.?\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final Pattern PUNTUACION = Pattern.compile("[^a-z0-9 ]");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");
    private static final Pattern DIACRITICOS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    /**
     * Minusculas, sin tildes, sin puntuacion, espacios colapsados y sin coletillas.
     *
     * <p>Es la base de la huella: dos textos que representen lo mismo tienen que
     * salir identicos de aqui.
     */
    public String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String resultado = texto;
        for (Pattern coletilla : COLETILLAS) {
            resultado = coletilla.matcher(resultado).replaceAll(" ");
        }
        resultado = Normalizer.normalize(resultado, Normalizer.Form.NFD);
        resultado = DIACRITICOS.matcher(resultado).replaceAll("");
        resultado = resultado.toLowerCase(Locale.ROOT);
        resultado = PUNTUACION.matcher(resultado).replaceAll(" ");
        resultado = ESPACIOS.matcher(resultado).replaceAll(" ");
        return resultado.trim();
    }

    /**
     * Huella exacta de la oferta.
     *
     * <p>{@code sha256(empresa|titulo|ubicacion)} ya normalizados. Cuesta diez
     * lineas y pilla la mayoria de los duplicados; el resto los caza el
     * {@link Deduplicador} por similitud.
     */
    public String huellaDe(Oferta oferta) {
        String semilla = normalizar(oferta.empresa()) + "|"
                + normalizar(oferta.titulo()) + "|"
                + normalizar(oferta.ubicacion());
        return sha256(semilla);
    }

    private String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda JVM; si falta, el entorno esta roto.
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }

    // ------------------------------------------------------------------
    //  TODO(fase-1): pendiente de implementar. Ver apartado 5 del guion.
    // ------------------------------------------------------------------

    /**
     * Mapea la ubicacion en bruto contra el catalogo de provincias espanolas.
     *
     * <p>Entradas reales: "Madrid, Comunidad de Madrid", "madrid", "Madrid, ES",
     * "Remote (Spain)". Devuelve null cuando la oferta es puramente remota.
     *
     * <p>TODO(fase-1): catalogo de provincias + alias.
     */
    public String normalizarUbicacion(String ubicacionEnBruto) {
        return normalizar(ubicacionEnBruto);
    }

    /**
     * Lleva cualquier salario a bruto anual en euros.
     *
     * <p>Las fuentes lo dan por hora, por mes o por ano, y a veces en otra divisa.
     *
     * <p>TODO(fase-1): deteccion de periodicidad y conversion.
     */
    public Integer aBrutoAnual(Double importe, String periodicidad, String divisa) {
        throw new UnsupportedOperationException("TODO(fase-1): normalizacion de salario");
    }
}
