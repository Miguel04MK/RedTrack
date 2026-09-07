package com.miguelalvarez.redtrack.infraestructura.fuentes.remotive;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte el salario de Remotive, que viene en texto libre, a bruto anual en
 * euros.
 *
 * <p>Formatos reales encontrados en una sola consulta a su API:
 *
 * <pre>
 *   "$50-$75 /hour"     "$20k -$35k"       "$14/hour"
 *   "$120 - $170 /hour" "$150k - $230k"    "$31,2k- $52k"
 *   "OTE $25k - $35k"   "Pay per task"     ""
 * </pre>
 *
 * <p>Esto es el ETL de verdad: cinco fuentes con cinco formatos de salario. El
 * codigo bonito es el 20%; el 80% es que los datos sucios entren limpios.
 *
 * <p>Cuando no se entiende el texto se devuelve vacio en vez de inventar una
 * cifra. Una oferta sin salario puntua mejor que una con una banda mala, asi que
 * adivinar aqui falsearia la nota.
 */
final class SalarioRemotive {

    /** Horas laborables al ano: 40 h/semana x 43 semanas efectivas. */
    private static final int HORAS_ANUALES = 1720;

    /** Aproximacion suficiente: la banda ya es orientativa. */
    private final double dolaresPorEuro;

    /**
     * Una cifra con sus separadores, seguida opcionalmente de "k".
     *
     * <p>Tiene que aceptar tanto "31,2k" (coma decimal) como "40,000" (coma de
     * millares): quien decide cual es cual es la "k", no la coma.
     */
    private static final Pattern CIFRA = Pattern.compile(
            "(\\d[\\d.,]*)\\s*(k)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern POR_HORA = Pattern.compile(
            "/\\s*h|per\\s+hour|hourly|/hr", Pattern.CASE_INSENSITIVE);

    SalarioRemotive(double dolaresPorEuro) {
        this.dolaresPorEuro = dolaresPorEuro;
    }

    /** @return [min, max] en euros brutos anuales, o vacio si no se entiende. */
    Optional<int[]> aBandaAnualEnEuros(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String limpio = texto.toLowerCase(Locale.ROOT).replace("$", " ").replace("€", " ");

        Matcher m = CIFRA.matcher(limpio);
        Integer min = null;
        Integer max = null;
        boolean porHora = POR_HORA.matcher(limpio).find();

        while (m.find()) {
            Integer valor = aEuros(m.group(1), m.group(2) != null, porHora);
            if (valor == null) {
                continue;
            }
            if (min == null) {
                min = valor;
            } else if (max == null) {
                max = valor;
            }
        }

        if (min == null) {
            // "Pay per task", "Competitive", texto sin cifras...
            return Optional.empty();
        }
        if (max == null) {
            max = min;
        }
        if (min > max) {
            int intercambio = min;
            min = max;
            max = intercambio;
        }
        return Optional.of(new int[]{min, max});
    }

    private Integer aEuros(String cifra, boolean llevaK, boolean porHora) {
        String limpia = cifra.replaceAll("[.,]+$", "");
        double valor;
        try {
            if (llevaK) {
                // Con "k" detras, el separador es decimal: "31,2k" son 31.200.
                valor = Double.parseDouble(limpia.replace(',', '.')) * 1000;
            } else {
                // Sin "k", los separadores son de millares: "40,000" son 40.000.
                valor = Double.parseDouble(limpia.replace(",", "").replace(".", ""));
            }
        } catch (NumberFormatException e) {
            return null;
        }
        if (porHora) {
            valor *= HORAS_ANUALES;
        }
        double enEuros = valor / dolaresPorEuro;

        // Descarta ruido: un anual por debajo de 6.000 no es un salario, es un
        // numero suelto del texto ("2025", "40 hours"...).
        if (enEuros < 6000 || enEuros > 500_000) {
            return null;
        }
        return (int) Math.round(enEuros);
    }
}
