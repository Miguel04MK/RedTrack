package com.miguelalvarez.redtrack.dominio.modelo;

import java.time.Instant;

/**
 * Oferta de empleo ya normalizada al modelo canonico.
 *
 * <p>Todos los adaptadores de fuente producen este tipo, independientemente del
 * esquema que devuelva la API de origen. Es el contrato entre la infraestructura
 * y el nucleo.
 *
 * @param fuente     identificador de la fuente ("adzuna", "remotive"...)
 * @param idExterno  identificador de la oferta dentro de esa fuente
 * @param salarioMin bruto anual en euros; null si la fuente no lo publica
 * @param salarioMax bruto anual en euros; igual a min si solo dan una cifra
 * @param huella     sha256 de empresa|titulo|ubicacion normalizados, para deduplicar
 */
public record Oferta(
        String fuente,
        String idExterno,
        String titulo,
        String empresa,
        String ubicacion,
        Modalidad modalidad,
        Integer salarioMin,
        Integer salarioMax,
        String descripcion,
        String url,
        Idioma idioma,
        Instant publicadaEn,
        Instant capturadaEn,
        String huella
) {

    /** Texto sobre el que trabajan el detector de tecnologias y los extractores. */
    public String textoCompleto() {
        return (titulo == null ? "" : titulo) + " " + (descripcion == null ? "" : descripcion);
    }

    /** true si la fuente publica banda salarial. */
    public boolean tieneSalario() {
        return salarioMin != null || salarioMax != null;
    }

    /** Copia de esta oferta con la huella ya calculada. */
    public Oferta conHuella(String nuevaHuella) {
        return new Oferta(fuente, idExterno, titulo, empresa, ubicacion, modalidad,
                salarioMin, salarioMax, descripcion, url, idioma,
                publicadaEn, capturadaEn, nuevaHuella);
    }
}
