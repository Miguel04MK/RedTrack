package com.miguelalvarez.redtrack.infraestructura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Spring Data. Detalle de infraestructura: el dominio solo conoce
 * {@code RepositorioOfertas}.
 */
public interface OfertaJpaRepository extends JpaRepository<OfertaEntity, Long> {

    Optional<OfertaEntity> findByHuella(String huella);

    boolean existsByFuenteAndIdExterno(String fuente, String idExterno);

    /** Candidatas para la comparacion difusa de titulos. */
    List<OfertaEntity> findByEmpresaNormalizadaAndUbicacionNormalizada(
            String empresaNormalizada, String ubicacionNormalizada);

    /** Ofertas que aun no se han evaluado en ningun resumen. */
    List<OfertaEntity> findByProcesadaEnIsNullOrderByEncajeDesc();

    @Modifying
    @Query("update OfertaEntity o set o.procesadaEn = :cuando where o.huella in :huellas")
    int marcarProcesadas(@Param("huellas") List<String> huellas,
                         @Param("cuando") Instant cuando);
}
