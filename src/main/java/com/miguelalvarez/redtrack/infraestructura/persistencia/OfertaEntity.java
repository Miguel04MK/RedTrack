package com.miguelalvarez.redtrack.infraestructura.persistencia;

import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Seniority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * La oferta canonica en base de datos.
 *
 * <p>Los nombres de campo coinciden con los componentes del record
 * {@code Oferta} para que el mapeo sea directo.
 *
 * <p>El esquema lo crea Flyway, no Hibernate: esta clase describe la tabla, no
 * la genera.
 */
@Entity
@Table(name = "oferta")
public class OfertaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String fuente;

    @Column(name = "id_externo", nullable = false, length = 200)
    private String idExterno;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(length = 300)
    private String empresa;

    @Column(length = 300)
    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modalidad modalidad;

    @Column(name = "salario_min")
    private Integer salarioMin;

    @Column(name = "salario_max")
    private Integer salarioMax;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(length = 1000)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private Idioma idioma;

    @Column(name = "publicada_en")
    private Instant publicadaEn;

    @Column(name = "capturada_en", nullable = false)
    private Instant capturadaEn;

    /** sha256 de empresa|titulo|ubicacion normalizados. Unico. */
    @Column(nullable = false, length = 64, unique = true)
    private String huella;

    // --- Claves de cotejo para el paso 2 de la deduplicacion -----------
    // Se guardan ya normalizadas para poder indexarlas y filtrar candidatas
    // sin traerse la tabla entera a memoria.

    @Column(name = "empresa_normalizada", length = 300)
    private String empresaNormalizada;

    @Column(name = "ubicacion_normalizada", length = 300)
    private String ubicacionNormalizada;

    // --- Analisis ------------------------------------------------------

    @Column(nullable = false)
    private int encaje;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Seniority senal;

    @Column(name = "anos_requeridos")
    private Integer anosRequeridos;

    /** Listas separadas por coma. Suficiente aqui: no se consultan por elemento. */
    @Column(name = "tecnologias_pedidas", columnDefinition = "text")
    private String tecnologiasPedidas;

    @Column(name = "las_tengo", columnDefinition = "text")
    private String lasTengo;

    @Column(name = "me_faltan", columnDefinition = "text")
    private String meFaltan;

    @Column(name = "banderas_rojas", columnDefinition = "text")
    private String banderasRojas;

    @Column(name = "resumen_ia", columnDefinition = "text")
    private String resumenIa;

    /** null mientras no se haya evaluado. Es lo que evita repetir ofertas. */
    @Column(name = "procesada_en")
    private Instant procesadaEn;

    public OfertaEntity() {
        // requerido por JPA, y usado por el adaptador al construir una fila nueva
    }

    // --- getters / setters --------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public void setIdExterno(String idExterno) {
        this.idExterno = idExterno;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public void setModalidad(Modalidad modalidad) {
        this.modalidad = modalidad;
    }

    public Integer getSalarioMin() {
        return salarioMin;
    }

    public void setSalarioMin(Integer salarioMin) {
        this.salarioMin = salarioMin;
    }

    public Integer getSalarioMax() {
        return salarioMax;
    }

    public void setSalarioMax(Integer salarioMax) {
        this.salarioMax = salarioMax;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Idioma getIdioma() {
        return idioma;
    }

    public void setIdioma(Idioma idioma) {
        this.idioma = idioma;
    }

    public Instant getPublicadaEn() {
        return publicadaEn;
    }

    public void setPublicadaEn(Instant publicadaEn) {
        this.publicadaEn = publicadaEn;
    }

    public Instant getCapturadaEn() {
        return capturadaEn;
    }

    public void setCapturadaEn(Instant capturadaEn) {
        this.capturadaEn = capturadaEn;
    }

    public String getHuella() {
        return huella;
    }

    public void setHuella(String huella) {
        this.huella = huella;
    }

    public String getEmpresaNormalizada() {
        return empresaNormalizada;
    }

    public void setEmpresaNormalizada(String empresaNormalizada) {
        this.empresaNormalizada = empresaNormalizada;
    }

    public String getUbicacionNormalizada() {
        return ubicacionNormalizada;
    }

    public void setUbicacionNormalizada(String ubicacionNormalizada) {
        this.ubicacionNormalizada = ubicacionNormalizada;
    }

    public int getEncaje() {
        return encaje;
    }

    public void setEncaje(int encaje) {
        this.encaje = encaje;
    }

    public Seniority getSenal() {
        return senal;
    }

    public void setSenal(Seniority senal) {
        this.senal = senal;
    }

    public Integer getAnosRequeridos() {
        return anosRequeridos;
    }

    public void setAnosRequeridos(Integer anosRequeridos) {
        this.anosRequeridos = anosRequeridos;
    }

    public String getTecnologiasPedidas() {
        return tecnologiasPedidas;
    }

    public void setTecnologiasPedidas(String tecnologiasPedidas) {
        this.tecnologiasPedidas = tecnologiasPedidas;
    }

    public String getLasTengo() {
        return lasTengo;
    }

    public void setLasTengo(String lasTengo) {
        this.lasTengo = lasTengo;
    }

    public String getMeFaltan() {
        return meFaltan;
    }

    public void setMeFaltan(String meFaltan) {
        this.meFaltan = meFaltan;
    }

    public String getBanderasRojas() {
        return banderasRojas;
    }

    public void setBanderasRojas(String banderasRojas) {
        this.banderasRojas = banderasRojas;
    }

    public String getResumenIa() {
        return resumenIa;
    }

    public void setResumenIa(String resumenIa) {
        this.resumenIa = resumenIa;
    }

    public Instant getProcesadaEn() {
        return procesadaEn;
    }

    public void setProcesadaEn(Instant procesadaEn) {
        this.procesadaEn = procesadaEn;
    }
}
