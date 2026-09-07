package com.miguelalvarez.redtrack.infraestructura.fuentes.remotive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Esquema de la respuesta de Remotive, tal cual llega.
 *
 * <p>A diferencia de Adzuna, {@code description} viene COMPLETA: entre 2.600 y
 * 28.000 caracteres medidos sobre respuestas reales, ninguna truncada. Ese es
 * el motivo de integrar esta fuente.
 *
 * <p>Viene en HTML, asi que hay que limpiarla antes de pasarsela al detector de
 * tecnologias.
 *
 * @param jobCount lo declara como "job-count", con guion
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemotiveRespuesta(
        @JsonProperty("job-count") int jobCount,
        List<Oferta> jobs
) {

    /**
     * @param tags   palabras clave de la oferta. NO son una lista limpia de
     *               tecnologias: una oferta de "Sales Jedi" trae "SOLID" entre
     *               sus tags y una de "Freelance Writer" trae "REST". Sirven
     *               como pista, nunca como fuente de verdad.
     * @param salary texto libre: "$50-$75 /hour", "$20k -$35k", "Pay per task"...
     * @param candidateRequiredLocation "Worldwide", "Europe", "USA", o listas
     *               como "LATAM, Europe, USA, Canada, APAC"
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Oferta(
            long id,
            String url,
            String title,
            @JsonProperty("company_name") String companyName,
            String category,
            List<String> tags,
            @JsonProperty("job_type") String jobType,
            @JsonProperty("publication_date") String publicationDate,
            @JsonProperty("candidate_required_location") String candidateRequiredLocation,
            String salary,
            String description
    ) {
    }
}
