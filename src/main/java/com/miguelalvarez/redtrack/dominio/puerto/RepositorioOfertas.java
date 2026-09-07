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

    /** Ofertas que superan el umbral y aun no se han notificado. */
    List<OfertaAnalizada> pendientesDeNotificar(int umbralEncaje);

    /**
     * Marca como notificadas para que no vuelvan a salir en el resumen de manana.
     *
     * <p>Solo se llama si el envio se confirmo: si el notificador falla, las
     * ofertas siguen pendientes y se reintentan.
     */
    void marcarNotificadas(List<String> huellas, Instant cuando);
}
