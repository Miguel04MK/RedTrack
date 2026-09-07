package com.miguelalvarez.redtrack.infraestructura.notificacion;

import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.modelo.ResumenDiario;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Da forma al mensaje del resumen diario.
 *
 * <p>Separado del envio para poder probar el texto sin tocar la red.
 *
 * <pre>
 *   RedTrack - 1 para ti · 3 podrian interesarte
 *
 *   PARA TI
 *
 *   [87%] Desarrollador Java Junior - Coremain
 *   Santiago · Hibrido · 21.000-25.000 EUR
 *   Pide: Java, Spring Boot, PostgreSQL
 *   Te falta: nada
 *   https://...
 *
 *   PODRIAN INTERESARTE
 *   Junior de desarrollo, aunque no sea tu stack.
 *
 *   [21%] Desarrollador PHP Junior - Otra
 *   Vigo · Presencial · sin salario
 *   Stack que no tienes: PHP
 *   https://...
 * </pre>
 */
@Component
public class FormateadorTelegram {

    /** Telegram corta los mensajes en 4096 caracteres. */
    public static final int MAX_CARACTERES = 4096;

    private static final int MARGEN = 60;

    public String formatear(ResumenDiario resumen) {
        StringBuilder sb = new StringBuilder();
        sb.append("*RedTrack* - ").append(cabecera(resumen)).append("\n");

        boolean cabeTodo = escribirSeccion(sb, "PARA TI", null, resumen.paraTi());
        if (cabeTodo) {
            escribirSeccion(sb, "PODRIAN INTERESARTE",
                    "_Junior de desarrollo, aunque no sea tu stack._",
                    resumen.podrianInteresarte());
        }
        return sb.toString();
    }

    private String cabecera(ResumenDiario resumen) {
        StringJoiner partes = new StringJoiner(" · ");
        if (!resumen.paraTi().isEmpty()) {
            partes.add(resumen.paraTi().size() + " para ti");
        }
        if (!resumen.podrianInteresarte().isEmpty()) {
            partes.add(resumen.podrianInteresarte().size() + " podrian interesarte");
        }
        return partes.toString();
    }

    /** @return false si hubo que cortar por longitud. */
    private boolean escribirSeccion(StringBuilder sb, String titulo, String subtitulo,
                                    List<OfertaAnalizada> ofertas) {
        if (ofertas.isEmpty()) {
            return true;
        }
        sb.append("\n*").append(titulo).append("*\n");
        if (subtitulo != null) {
            sb.append(subtitulo).append("\n");
        }
        for (OfertaAnalizada analizada : ofertas) {
            String bloque = bloqueDe(analizada);
            if (sb.length() + bloque.length() > MAX_CARACTERES - MARGEN) {
                sb.append("\n_...y mas. Mira /api/ofertas para el resto._");
                return false;
            }
            sb.append(bloque);
        }
        return true;
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

        // El aviso que pidio Mikel: la oferta entra igual, pero se dice claro
        // que el stack no es el suyo. La maquina informa, la persona decide.
        if (!analizada.analisis().meFaltan().isEmpty()) {
            b.append("Stack que no tienes: ")
                    .append(String.join(", ", analizada.analisis().meFaltan()))
                    .append("\n");
        } else if (!analizada.analisis().tecnologiasPedidas().isEmpty()) {
            b.append("Te falta: nada\n");
        }

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
        return texto.charAt(0) + texto.substring(1).toLowerCase(Locale.ROOT);
    }
}
