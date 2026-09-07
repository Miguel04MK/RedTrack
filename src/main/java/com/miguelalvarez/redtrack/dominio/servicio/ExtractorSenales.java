package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Seniority;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Saca de la prosa de la oferta las senales que el puntuador necesita:
 * modalidad, anos de experiencia y seniority.
 */
public final class ExtractorSenales {

    // --- Modalidad -----------------------------------------------------
    private static final Pattern REMOTO = Pattern.compile(
            "100\\s*%\\s*remoto|teletrabajo|full\\s*remote|fully\\s*remote|\\bremoto\\b|\\bremote\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HIBRIDO = Pattern.compile(
            "h[ií]brid[oa]|hybrid", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRESENCIAL = Pattern.compile(
            "presencial|on[\\s-]?site", Pattern.CASE_INSENSITIVE);

    // --- Anos de experiencia -------------------------------------------
    private static final Pattern ANOS_ES = Pattern.compile(
            "(?:al menos|m[ií]nimo|m[áa]s de|\\+)?\\s*(\\d+)\\s*(?:-\\s*(\\d+)\\s*)?a[ñn]os?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ANOS_EN = Pattern.compile(
            "(\\d+)\\s*(?:-\\s*(\\d+)\\s*)?\\+?\\s*years?", Pattern.CASE_INSENSITIVE);

    // --- Seniority ------------------------------------------------------
    private static final Pattern JUNIOR = Pattern.compile(
            "\\bjunior\\b|\\bj[úu]nior\\b|\\bjr\\.?\\b|reci[ée]n titulad|sin experiencia|"
                    + "\\btrainee\\b|\\bbecari|\\bentry[\\s-]level\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SENIOR = Pattern.compile(
            "\\bsenior\\b|\\bs[ée]nior\\b|\\bsr\\.?\\b|\\blead\\b|\\barquitect|\\bprincipal\\b|"
                    + "\\bstaff\\b|\\bmanager\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MID = Pattern.compile(
            "\\bmid\\b|\\bmid[\\s-]level\\b|\\bsemi[\\s-]?senior\\b|\\bintermedio\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Modalidad a partir del titulo y la descripcion.
     *
     * <p>Prioridad deliberada: si el texto dice hibrido Y remoto, gana HIBRIDO.
     * Es lo que acaba pasando en la practica.
     */
    public Modalidad modalidad(String texto) {
        if (texto == null || texto.isBlank()) {
            return Modalidad.DESCONOCIDA;
        }
        if (HIBRIDO.matcher(texto).find()) {
            return Modalidad.HIBRIDO;
        }
        if (REMOTO.matcher(texto).find()) {
            return Modalidad.REMOTO;
        }
        if (PRESENCIAL.matcher(texto).find()) {
            return Modalidad.PRESENCIAL;
        }
        return Modalidad.DESCONOCIDA;
    }

    /**
     * Anos de experiencia requeridos.
     *
     * <p>Se queda con el numero MAS ALTO que encuentre: si la oferta dice
     * "2-4 anos", el filtro real es 4.
     *
     * @return null si la oferta no lo dice
     */
    public Integer anosRequeridos(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        Integer maximo = null;
        for (Pattern patron : List.of(ANOS_ES, ANOS_EN)) {
            Matcher m = patron.matcher(texto);
            while (m.find()) {
                maximo = mayor(maximo, aEntero(m.group(1)));
                maximo = mayor(maximo, aEntero(m.group(2)));
            }
        }
        return maximo;
    }

    /**
     * Seniority declarada.
     *
     * <p>El orden importa: SENIOR gana a JUNIOR porque "Senior Java Developer
     * (no junior)" es una oferta senior.
     */
    public Seniority seniority(String texto) {
        if (texto == null || texto.isBlank()) {
            return Seniority.NO_DICE;
        }
        if (SENIOR.matcher(texto).find()) {
            return Seniority.SENIOR;
        }
        if (JUNIOR.matcher(texto).find()) {
            return Seniority.JUNIOR;
        }
        if (MID.matcher(texto).find()) {
            return Seniority.MID;
        }
        return Seniority.NO_DICE;
    }

    private Integer aEntero(String grupo) {
        if (grupo == null) {
            return null;
        }
        try {
            return Integer.valueOf(grupo);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer mayor(Integer a, Integer b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return Math.max(a, b);
    }

    /** Heuristica barata de idioma; suficiente para marcar las fuentes remotas. */
    public boolean pareceIngles(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        String t = texto.toLowerCase(Locale.ROOT);
        int marcadores = 0;
        for (String palabra : List.of(" the ", " and ", " you ", " we ", " your ", " with ", " for ")) {
            if (t.contains(palabra)) {
                marcadores++;
            }
        }
        return marcadores >= 3;
    }
}
