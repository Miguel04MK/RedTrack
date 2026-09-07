package com.miguelalvarez.redtrack.dominio.puerto;

import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Salida de persistencia. El dominio no sabe que detras hay JPA.
 *
 * <p>Las ofertas se referencian por su HUELLA, no por un id de base de datos.
 * La huella es una clave natural del dominio y es unica en la tabla, asi que
 * sirve igual de bien y evita que la identidad de JPA se filtre hasta aqui.
 */
public interface RepositorioOfertas {

    /** Guarda una oferta canonica con su analisis. */
    void guardar(OfertaAnalizada analizada);

    /** Busca la oferta canonica que comparta huella exacta. */
    Optional<Oferta> porHuella(String huella);

    /** true si ya tenemos esta oferta de esta misma fuente. */
    boolean existe(String fuente, String idExterno);

    /**
     * Registra que una oferta canonica ya conocida ha aparecido tambien en otra
     * fuente, para poder decir luego "vista en Adzuna y Remotive".
     *
     * @param huellaCanonica huella de la oferta ya guardada con la que coincide
     */
    void registrarAparicion(String huellaCanonica, String fuente, String idExterno, String url);

    /** Candidatas para la comparacion difusa: misma empresa y ubicacion normalizadas. */
    List<Oferta> candidatasParaCotejar(String empresaNormalizada, String ubicacionNormalizada);

    /**
     * Ofertas aun sin evaluar en ningun resumen.
     *
     * <p>No filtra por encaje: quien decide que entra y que no es el dominio,
     * y la seccion "podrian interesarte" acepta ofertas con encaje bajo a
     * proposito. Un filtro por umbral aqui la vaciaria.
     */
    List<OfertaAnalizada> pendientes();

    /**
     * Marca como procesadas para que no vuelvan a evaluarse manana.
     *
     * <p>Se marcan todas las evaluadas, entren o no en el resumen: el veredicto
     * es determinista, asi que lo que no entro hoy no entrara manana. Solo se
     * llama si el envio se confirmo.
     */
    void marcarProcesadas(List<String> huellas, Instant cuando);
}
