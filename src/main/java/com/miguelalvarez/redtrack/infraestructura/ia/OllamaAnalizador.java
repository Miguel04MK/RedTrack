package com.miguelalvarez.redtrack.infraestructura.ia;

import com.miguelalvarez.redtrack.configuracion.RedTrackProperties;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.puerto.AnalizadorSemantico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

/**
 * Resumen de la oferta con un modelo local via Ollama.
 *
 * <p>Solo se instancia si {@code redtrack.ia.activa=true}. En perfil {@code prod}
 * va desactivado: una t2.micro tiene 1 GB de RAM y no mueve el modelo. Saber que
 * no cabe y decirlo vale mas que intentarlo y que se caiga.
 *
 * <p>Timeout corto a proposito: con 30 ofertas, un modelo que tarde 40 segundos
 * por oferta hace que la tarea diaria no termine nunca.
 */
public class OllamaAnalizador implements AnalizadorSemantico {

    private static final Logger log = LoggerFactory.getLogger(OllamaAnalizador.class);

    private final RedTrackProperties.Ia config;
    private final RestClient cliente;

    public OllamaAnalizador(RedTrackProperties.Ia config, RestClient.Builder builder) {
        this.config = config;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) config.timeout().toMillis());
        fabrica.setReadTimeout((int) config.timeout().toMillis());
        this.cliente = builder.baseUrl(config.url()).requestFactory(fabrica).build();
    }

    @Override
    public Optional<String> resumir(Oferta oferta, Perfil perfil) {
        try {
            OllamaRespuesta respuesta = cliente.post()
                    .uri("/api/generate")
                    .body(Map.of(
                            "model", config.modelo(),
                            "prompt", prompt(oferta, perfil),
                            "stream", false))
                    .retrieve()
                    .body(OllamaRespuesta.class);

            return Optional.ofNullable(respuesta)
                    .map(OllamaRespuesta::response)
                    .filter(t -> !t.isBlank())
                    .map(String::trim);

        } catch (RuntimeException e) {
            // Degradar, no romper. Una dependencia externa opcional no puede
            // tumbar el servicio.
            log.warn("Ollama no ha respondido para '{}': {}. Se sigue sin resumen.",
                    oferta.titulo(), e.getMessage());
            return Optional.empty();
        }
    }

    private String prompt(Oferta oferta, Perfil perfil) {
        // TODO(fase-3): afinar. El README explica POR QUE la IA es opcional,
        //               no el prompt: eso es lo que interesa a quien lo lea.
        return """
                Resume en una sola frase, en espanol y sin adornos, que pide esta \
                oferta de empleo y si encaja con un perfil junior de %s.

                Titulo: %s
                Empresa: %s
                Descripcion: %s
                """.formatted(
                String.join(", ", perfil.dominadas().stream()
                        .map(t -> t.nombre()).toList()),
                oferta.titulo(),
                oferta.empresa(),
                recortar(oferta.descripcion()));
    }

    private String recortar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() > 2000 ? texto.substring(0, 2000) : texto;
    }

    /** Lo que devuelve /api/generate cuando stream=false. */
    record OllamaRespuesta(String model, String response, boolean done) {
    }
}
