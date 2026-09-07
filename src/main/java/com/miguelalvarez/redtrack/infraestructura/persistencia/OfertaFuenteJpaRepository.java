package com.miguelalvarez.redtrack.infraestructura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfertaFuenteJpaRepository extends JpaRepository<OfertaFuenteEntity, Long> {

    List<OfertaFuenteEntity> findByOfertaId(Long ofertaId);

    boolean existsByFuenteAndIdExterno(String fuente, String idExterno);
}
