package com.miguelalvarez.redtrack.aplicacion;

import com.miguelalvarez.redtrack.dominio.modelo.Analisis;
import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.servicio.ExtractorSenales;
import com.miguelalvarez.redtrack.dominio.servicio.Normalizador;
import com.miguelalvarez.redtrack.dominio.servicio.Puntuador;

import java.time.Instant;

/**
 * El modo "pegame esta oferta que acabo de ver".
 *
 * <p>Es la respuesta practica al truncado de Adzuna: para las ofertas que de
 * verdad interesan, se copia el texto entero y se analiza completo. Sin
 * scraping, sin depender de que la fuente este integrada, y sin guardar nada.
 *
 * <p>Tambien sirve para una oferta vista en cualquier portal que no se puede
 * integrar por sus terminos de uso. El humano hace de puente, que es
 * exactamente lo que un scraper haria mal.
 */
public class AnalizarTextoUseCase {

    private final Puntuador puntuador;
    private final ExtractorSenales extractor;
    private final Normalizador normalizador;

    public AnalizarTextoUseCase(Puntuador puntuador, ExtractorSenales extractor,
                                Normalizador normalizador) {
        this.puntuador = puntuador;
        this.extractor = extractor;
        this.normalizador = normalizador;
    }

    /**
     * @param titulo puede venir vacio; si viene, cuenta para la seniority y para
     *               el filtro de titulos vetados
     * @param texto  el cuerpo de la oferta, en texto plano o en HTML
     */
    public Resultado ejecutar(String titulo, String texto, Perfil perfil) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Hace falta el texto de la oferta");
        }
        String limpio = normalizador.sinHtml(texto);

        Oferta sintetica = new Oferta(
                "manual",
                "pegada",
                titulo == null ? "" : titulo.trim(),
                null,
                null,
                extractor.modalidad(limpio),
                null,
                null,
                limpio,
                null,
                extractor.pareceIngles(limpio) ? Idioma.EN : Idioma.ES,
                Instant.now(),
                Instant.now(),
                null);

        Analisis analisis = puntuador.analizar(sintetica, perfil);
        return new Resultado(analisis, sintetica.modalidad(), avisoDeContexto(sintetica));
    }

    /**
     * Una oferta pegada no trae empresa, ubicacion ni banda salarial, y esos
     * bloques puntuan neutro. Conviene decirlo en vez de dejar que el numero
     * parezca comparable con el de una oferta recolectada.
     */
    private String avisoDeContexto(Oferta oferta) {
        return "Sin ubicacion ni salario, esos bloques puntuan en neutro. "
                + "El encaje no es comparable con el de una oferta recolectada"
                + (oferta.titulo().isBlank()
                        ? ", y sin titulo la seniority solo se busca en el cuerpo." : ".");
    }

    public record Resultado(Analisis analisis, Modalidad modalidad, String aviso) {
    }
}
