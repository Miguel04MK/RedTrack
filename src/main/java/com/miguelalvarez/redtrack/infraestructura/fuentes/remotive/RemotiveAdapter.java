package com.miguelalvarez.redtrack.infraestructura.fuentes.remotive;

import com.miguelalvarez.redtrack.configuracion.RedTrackProperties;
import com.miguelalvarez.redtrack.dominio.modelo.CriterioBusqueda;
import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.puerto.FuenteDeOfertas;
import com.miguelalvarez.redtrack.dominio.servicio.Normalizador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Segunda fuente: empleo remoto.
 *
 * <p>Se integra por un motivo concreto: <b>devuelve la descripcion completa</b>.
 * Medido sobre respuestas reales, entre 2.658 y 28.465 caracteres, ninguna
 * truncada, frente a los 500 de Adzuna. Con el texto entero, el detector de
 * tecnologias y el extractor de anos empiezan a funcionar de verdad.
 *
 * <p>A cambio, la normalizacion es mucho mas sucia. Tres cosas que no se ven
 * hasta que se miran los datos:
 * <ul>
 *   <li>La descripcion viene en <b>HTML</b>, y hay que limpiarla antes de
 *       buscar tecnologias, o se acaba buscando dentro de atributos de estilo.</li>
 *   <li>La categoria {@code software-dev} <b>no filtra</b>: en una sola consulta
 *       aparecieron "Sales Jedi", "Freelance Writer" y "Freelance Copywriter".</li>
 *   <li>Muchas ofertas son <b>solo para Estados Unidos</b>, y esas no sirven.</li>
 * </ul>
 *
 * <p>Todas las ofertas son REMOTO por definicion, y en ingles.
 */
@Component
public class RemotiveAdapter implements FuenteDeOfertas {

    private static final Logger log = LoggerFactory.getLogger(RemotiveAdapter.class);
    public static final String NOMBRE = "remotive";

    /**
     * Zonas desde las que se puede optar estando en Espana.
     *
     * <p>"USA" o "USA, Canada, USA timezones" quedan fuera: son ofertas reales
     * pero no para alguien que vive aqui, y colarlas seria ruido garantizado.
     */
    private static final Pattern ZONA_VALIDA = Pattern.compile(
            "worldwide|anywhere|europe|emea|spain|espa[nñ]a|european",
            Pattern.CASE_INSENSITIVE);

    /** Formato de publication_date: "2026-09-05T14:12:33". */
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final RedTrackProperties.Remotive config;
    private final RestClient cliente;
    private final Normalizador normalizador;
    private final SalarioRemotive salarios;

    public RemotiveAdapter(RedTrackProperties propiedades,
                           RestClient.Builder builder,
                           Normalizador normalizador) {
        this.config = propiedades.remotive();
        this.cliente = builder.baseUrl(config.url()).build();
        this.normalizador = normalizador;
        this.salarios = new SalarioRemotive(config.dolaresPorEuro());
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public boolean estaActiva() {
        return config.activa();
    }

    /**
     * Remotive no tiene busqueda por ubicacion ni por antiguedad: se pide la
     * categoria entera y se filtra aqui. Por eso {@code criterio.donde()} se
     * ignora, tal y como permite el contrato del puerto.
     *
     * <p>TODO(fase-4): como ignora la ubicacion, los 9 terminos x 3 ubicaciones
     * producen 27 llamadas casi identicas. Medido: 402 ofertas vistas para 14
     * nuevas. El deduplicador lo absorbe sin ensuciar nada, pero es trafico
     * tirado. La fuente deberia poder declarar que dimensiones del criterio
     * entiende, o el caso de uso agrupar los criterios por fuente.
     */
    @Override
    public List<Oferta> buscar(CriterioBusqueda criterio) {
        try {
            RemotiveRespuesta respuesta = cliente.get()
                    .uri(uri -> uri
                            .path("/api/remote-jobs")
                            .queryParam("category", config.categoria())
                            .queryParam("search", criterio.que())
                            .queryParam("limit", criterio.maxResultados())
                            .build())
                    .retrieve()
                    .body(RemotiveRespuesta.class);

            if (respuesta == null || respuesta.jobs() == null) {
                return List.of();
            }
            return respuesta.jobs().stream()
                    .filter(this::sePuedeOptarDesdeEspana)
                    .map(this::aOferta)
                    .toList();

        } catch (RuntimeException e) {
            // Contrato del puerto: una fuente caida no tumba la recoleccion.
            log.error("Remotive no ha respondido para '{}': {}", criterio.que(), e.getMessage());
            return List.of();
        }
    }

    boolean sePuedeOptarDesdeEspana(RemotiveRespuesta.Oferta o) {
        String zona = o.candidateRequiredLocation();
        if (zona == null || zona.isBlank()) {
            // Sin restriccion declarada, se asume abierta.
            return true;
        }
        return ZONA_VALIDA.matcher(zona).find();
    }

    private Oferta aOferta(RemotiveRespuesta.Oferta o) {
        String descripcion = normalizador.sinHtml(o.description());
        Optional<int[]> banda = salarios.aBandaAnualEnEuros(o.salary());

        return new Oferta(
                NOMBRE,
                String.valueOf(o.id()),
                o.title(),
                o.companyName(),
                o.candidateRequiredLocation(),
                Modalidad.REMOTO,
                banda.map(b -> b[0]).orElse(null),
                banda.map(b -> b[1]).orElse(null),
                descripcion,
                o.url(),
                Idioma.EN,
                aInstant(o.publicationDate()),
                Instant.now(),
                null // la huella la calcula el caso de uso
        );
    }

    private Instant aInstant(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return Instant.now();
        }
        try {
            return LocalDateTime.parse(fecha.trim().toUpperCase(Locale.ROOT), FECHA)
                    .toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            log.debug("Fecha de Remotive no reconocida: '{}'", fecha);
            return Instant.now();
        }
    }
}
