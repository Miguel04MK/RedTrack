package com.miguelalvarez.redtrack.infraestructura.fuentes.adzuna;

import com.miguelalvarez.redtrack.configuracion.RedTrackProperties;
import com.miguelalvarez.redtrack.dominio.modelo.CriterioBusqueda;
import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.puerto.FuenteDeOfertas;
import com.miguelalvarez.redtrack.dominio.servicio.ExtractorSenales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Fuente principal, y la unica verificada del guion.
 *
 * <p>Registro gratuito en https://developer.adzuna.com/signup para conseguir
 * {@code app_id} y {@code app_key}, que llegan por variable de entorno.
 *
 * <p>Es la unica fuente que publica SALARIO, lo que ademas habilita los graficos
 * de mercado del README.
 */
@Component
public class AdzunaAdapter implements FuenteDeOfertas {

    private static final Logger log = LoggerFactory.getLogger(AdzunaAdapter.class);
    public static final String NOMBRE = "adzuna";

    private final RedTrackProperties.Adzuna config;
    private final RestClient cliente;
    private final ExtractorSenales extractor;

    public AdzunaAdapter(RedTrackProperties propiedades,
                         RestClient.Builder builder,
                         ExtractorSenales extractor) {
        this.config = propiedades.adzuna();
        this.cliente = builder.baseUrl(config.url()).build();
        this.extractor = extractor;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public boolean estaActiva() {
        boolean tieneClaves = config.appId() != null && !config.appId().isBlank()
                && config.appKey() != null && !config.appKey().isBlank();
        if (config.activa() && !tieneClaves) {
            log.warn("Adzuna esta activa pero faltan ADZUNA_APP_ID / ADZUNA_APP_KEY");
        }
        return config.activa() && tieneClaves;
    }

    @Override
    public List<Oferta> buscar(CriterioBusqueda criterio) {
        try {
            AdzunaRespuesta respuesta = cliente.get()
                    .uri(uri -> uri
                            .path("/v1/api/jobs/{pais}/search/1")
                            .queryParam("app_id", config.appId())
                            .queryParam("app_key", config.appKey())
                            .queryParam("results_per_page", criterio.maxResultados())
                            .queryParam("what", criterio.que())
                            .queryParam("where", criterio.donde())
                            .queryParam("max_days_old", criterio.maxDiasAntiguedad())
                            .queryParam("content-type", "application/json")
                            .build(config.pais()))
                    .retrieve()
                    .body(AdzunaRespuesta.class);

            if (respuesta == null || respuesta.results() == null) {
                return List.of();
            }
            return respuesta.results().stream().map(this::aOferta).toList();

        } catch (RuntimeException e) {
            // Contrato del puerto: una fuente caida no puede tumbar la recoleccion.
            log.error("Adzuna no ha respondido para '{}' en '{}': {}",
                    criterio.que(), criterio.donde(), e.getMessage());
            return List.of();
        }
    }

    private Oferta aOferta(AdzunaRespuesta.Resultado r) {
        String descripcion = r.description() == null ? "" : r.description();
        String texto = (r.title() == null ? "" : r.title()) + " " + descripcion;

        return new Oferta(
                NOMBRE,
                r.id(),
                r.title(),
                r.company() == null ? null : r.company().displayName(),
                r.location() == null ? null : r.location().displayName(),
                extractor.modalidad(texto),
                aEuros(r.salaryMin()),
                aEuros(r.salaryMax()),
                descripcion,
                r.redirectUrl(),
                extractor.pareceIngles(texto) ? Idioma.EN : Idioma.ES,
                aInstant(r.created()),
                Instant.now(),
                null // la huella la calcula el caso de uso con el Normalizador
        );
    }

    /**
     * Adzuna devuelve el salario ya como bruto anual en la divisa del pais.
     *
     * <p>TODO(fase-2): {@code salary_is_predicted} vale "1" cuando la cifra es una
     * estimacion de Adzuna, no un dato publicado por la empresa. Habria que
     * marcarlo para no puntuar una estimacion como si fuese una banda real.
     */
    private Integer aEuros(Double importe) {
        return importe == null ? null : (int) Math.round(importe);
    }

    private Instant aInstant(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return Instant.now();
        }
        try {
            return OffsetDateTime.parse(fecha).toInstant();
        } catch (DateTimeParseException e) {
            log.debug("Fecha de Adzuna no reconocida: '{}'", fecha);
            return Instant.now();
        }
    }
}
