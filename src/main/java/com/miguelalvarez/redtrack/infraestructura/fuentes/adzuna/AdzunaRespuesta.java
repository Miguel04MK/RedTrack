package com.miguelalvarez.redtrack.infraestructura.fuentes.adzuna;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Esquema de la respuesta de Adzuna, tal cual llega.
 *
 * <p>Vive en infraestructura a proposito: el dominio no debe conocer el formato
 * de ninguna fuente. Aqui es donde se traduce a {@code Oferta}.
 *
 * <p>{@code @JsonIgnoreProperties} porque Adzuna anade campos sin avisar y un
 * campo nuevo no puede tumbar la recoleccion.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdzunaRespuesta(
        long count,
        List<Resultado> results
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resultado(
            String id,
            String title,
            String description,
            Empresa company,
            Ubicacion location,
            @JsonProperty("salary_min") Double salaryMin,
            @JsonProperty("salary_max") Double salaryMax,
            @JsonProperty("salary_is_predicted") String salaryIsPredicted,
            @JsonProperty("redirect_url") String redirectUrl,
            String created,
            Categoria category
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Empresa(@JsonProperty("display_name") String displayName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ubicacion(
            @JsonProperty("display_name") String displayName,
            List<String> area
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Categoria(String label, String tag) {
    }
}
