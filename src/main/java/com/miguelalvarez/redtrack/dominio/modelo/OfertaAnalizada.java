package com.miguelalvarez.redtrack.dominio.modelo;

import java.util.List;

/**
 * Una oferta junto con su analisis. Es lo que viaja del nucleo a las salidas
 * (Telegram, API REST).
 *
 * @param vistaEn fuentes en las que aparecio esta misma oferta canonica.
 *                Que una oferta este en varios sitios es informacion util,
 *                no basura: en el resumen se muestra ("vista en Adzuna y Remotive").
 */
public record OfertaAnalizada(
        Oferta oferta,
        Analisis analisis,
        List<String> vistaEn
) {

    public OfertaAnalizada {
        vistaEn = vistaEn == null ? List.of() : List.copyOf(vistaEn);
    }

    public static OfertaAnalizada de(Oferta oferta, Analisis analisis) {
        return new OfertaAnalizada(oferta, analisis, List.of(oferta.fuente()));
    }

    public int encaje() {
        return analisis.encaje();
    }
}
