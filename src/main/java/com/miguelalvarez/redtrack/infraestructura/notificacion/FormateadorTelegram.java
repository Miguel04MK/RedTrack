package com.miguelalvarez.redtrack.infraestructura.notificacion;

import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.modelo.ResumenDiario;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

/**
 * Da forma al mensaje del resumen diario.
 *
 * <p>Separado del envio para poder probar el texto sin tocar la red.
 *
 * <pre>
 *   RedTrack - 7 ofertas nuevas
 *
 *   [87%] Desarrollador Java Junior - Coremain
 *   Santiago · Hibrido · 21.000-25.000 EUR
 *   Pide: Java, Spring Boot, PostgreSQL, Docker
 *   Te falta: nada
 *   https://...
 * </pre>
 */
@Component
public class FormateadorTelegram {

    /** Telegram corta los mensajes en 4096 caracteres. */
    public static final int MAX_CARACTERES = 4096;

    public String formatear(ResumenDiario resumen) {
        StringBuilder sb = new StringBuilder();
        sb.append("*RedTrack* - ").append(resumen.cuantas())
                .append(resumen.cuantas() == 1 ? " oferta nueva" : " ofertas nuevas")
                .append("\n");

        for (OfertaAnalizada analizada : resumen.ofertas()) {
            String bloque = bloqueDe(analizada);
            if (sb.length() + bloque.length() > MAX_CARACTERES - 40) {
                sb.append("\n_...y mas. Mira /api/ofertas para el resto._");
                break;
            }
            sb.append(bloque);
        }
        return sb.toString();
    }

    private String bloqueDe(OfertaAnalizada analizada) {
        Oferta o = analizada.oferta();
        StringBuilder b = new StringBuilder("\n");

        b.append("[").append(analizada.encaje()).append("%] ")
                .append(o.titulo()).append(" - ").append(o.empresa()).append("\n");

        StringJoiner linea = new StringJoiner(" · ");
        if (o.ubicacion() != null && !o.ubicacion().isBlank()) {
            linea.add(o.ubicacion());
        }
        linea.add(capitalizar(o.modalidad().name()));
        linea.add(salarioDe(o));
        b.append(linea).append("\n");

        if (!analizada.analisis().tecnologiasPedidas().isEmpty()) {
            b.append("Pide: ")
                    .append(String.join(", ", analizada.analisis().tecnologiasPedidas()))
                    .append("\n");
        }
        b.append("Te falta: ")
                .append(analizada.analisis().meFaltan().isEmpty()
                        ? "nada"
                        : String.join(", ", analizada.analisis().meFaltan()))
                .append("\n");

        for (String bandera : analizada.analisis().banderasRojas()) {
            b.append("AVISO: ").append(bandera).append("\n");
        }
        if (analizada.vistaEn().size() > 1) {
            b.append("Vista en: ").append(String.join(" y ", analizada.vistaEn())).append("\n");
        }
        if (analizada.analisis().resumenIA() != null) {
            b.append("_").append(analizada.analisis().resumenIA()).append("_\n");
        }
        b.append(o.url()).append("\n");
        return b.toString();
    }

    private String salarioDe(Oferta o) {
        if (!o.tieneSalario()) {
            return "sin salario";
        }
        Integer min = o.salarioMin();
        Integer max = o.salarioMax();
        if (min != null && max != null && !min.equals(max)) {
            return String.format("%,d-%,d EUR", min, max);
        }
        return String.format("%,d EUR", min != null ? min : max);
    }

    private String capitalizar(String texto) {
        return texto.charAt(0) + texto.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
