package com.miguelalvarez.redtrack.dominio.puerto;

import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Salida de persistencia. El dominio no sabe que detras hay JPA.
 */
public interface RepositorioOfertas {

    /** Guarda una oferta canonica con su analisis y devuelve su id. */
    Long guardar(OfertaAnalizada analizada);

    /** Busca la oferta canonica que comparta huella exacta. */
    Optional<Oferta> porHuella(String huella);

    /** true si ya tenemos esta oferta de esta misma fuente. */
    boolean existe(String fuente, String idExterno);

    /** Registra que una oferta canonica ya conocida ha aparecido tambien en otra fuente. */
    void registrarAparicion(Long idCanonica, String fuente, String idExterno, String url);

    /** Candidatas para la comparacion difusa: misma empresa y ubicacion normalizadas. */
    List<Oferta> candidatasParaCotejar(String empresaNormalizada, String ubicacionNormalizada);

    /** Ofertas que superan el umbral y aun no se han notificado. */
    List<OfertaAnalizada> pendientesDeNotificar(int umbralEncaje);

    /** Marca como notificadas para que no vuelvan a salir en el resumen de manana. */
    void marcarNotificadas(List<Long> ids, Instant cuando);
}
