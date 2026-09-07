package com.miguelalvarez.redtrack.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * En que fuentes ha aparecido una misma oferta canonica.
 *
 * <p>Que una oferta este en varios sitios es informacion util, no basura: en el
 * resumen diario se muestra como "vista en Adzuna y Remotive".
 */
@Entity
@Table(name = "oferta_fuente")
public class OfertaFuenteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oferta_id", nullable = false)
    private Long ofertaId;

    @Column(nullable = false, length = 40)
    private String fuente;

    @Column(name = "id_externo", nullable = false, length = 200)
    private String idExterno;

    @Column(length = 1000)
    private String url;

    @Column(name = "vista_en", nullable = false)
    private Instant vistaEn;

    protected OfertaFuenteEntity() {
        // requerido por JPA
    }

    public OfertaFuenteEntity(Long ofertaId, String fuente, String idExterno,
                              String url, Instant vistaEn) {
        this.ofertaId = ofertaId;
        this.fuente = fuente;
        this.idExterno = idExterno;
        this.url = url;
        this.vistaEn = vistaEn;
    }

    public Long getId() {
        return id;
    }

    public Long getOfertaId() {
        return ofertaId;
    }

    public String getFuente() {
        return fuente;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public String getUrl() {
        return url;
    }

    public Instant getVistaEn() {
        return vistaEn;
    }
}
