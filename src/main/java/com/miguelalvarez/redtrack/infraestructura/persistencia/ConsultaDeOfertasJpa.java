package com.miguelalvarez.redtrack.infraestructura.persistencia;

import com.miguelalvarez.redtrack.dominio.modelo.FiltroOfertas;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.puerto.ConsultaDeOfertas;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lado de lectura, con Criteria API.
 *
 * <p>Se usa Criteria y no una consulta JPQL con {@code (:param is null or ...)}
 * porque los filtros son opcionales e independientes: con cuatro campos son
 * dieciseis combinaciones, y una consulta llena de comprobaciones de nulo obliga
 * a la base de datos a evaluarlas todas en cada ejecucion.
 */
@Repository
public class ConsultaDeOfertasJpa implements ConsultaDeOfertas {

    private final EntityManager em;
    private final OfertaJpaRepository ofertas;
    private final OfertaFuenteJpaRepository apariciones;
    private final OfertaMapeador mapeador;

    public ConsultaDeOfertasJpa(EntityManager em,
                                OfertaJpaRepository ofertas,
                                OfertaFuenteJpaRepository apariciones,
                                OfertaMapeador mapeador) {
        this.em = em;
        this.ofertas = ofertas;
        this.apariciones = apariciones;
        this.mapeador = mapeador;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfertaAnalizada> buscar(FiltroOfertas filtro) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OfertaEntity> consulta = cb.createQuery(OfertaEntity.class);
        Root<OfertaEntity> oferta = consulta.from(OfertaEntity.class);

        List<Predicate> condiciones = new ArrayList<>();
        if (filtro.minEncaje() != null) {
            condiciones.add(cb.greaterThanOrEqualTo(oferta.get("encaje"), filtro.minEncaje()));
        }
        if (filtro.modalidad() != null) {
            condiciones.add(cb.equal(oferta.get("modalidad"), filtro.modalidad()));
        }
        if (filtro.desde() != null) {
            Instant desde = filtro.desde().atStartOfDay(ZoneOffset.UTC).toInstant();
            condiciones.add(cb.greaterThanOrEqualTo(oferta.get("publicadaEn"), desde));
        }
        if (filtro.fuente() != null && !filtro.fuente().isBlank()) {
            condiciones.add(cb.equal(oferta.get("fuente"), filtro.fuente()));
        }

        consulta.where(condiciones.toArray(new Predicate[0]))
                .orderBy(cb.desc(oferta.get("encaje")));

        return em.createQuery(consulta)
                .setMaxResults(filtro.limite())
                .getResultList()
                .stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OfertaAnalizada> porHuella(String huella) {
        return ofertas.findByHuella(huella).map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Estadisticas estadisticas(int topTecnologias) {
        return new Estadisticas(
                ofertas.count(),
                ofertas.tecnologiasMasPedidas(topTecnologias).stream()
                        .map(f -> new Estadisticas.ConteoTecnologia(
                                (String) f[0], ((Number) f[1]).longValue()))
                        .toList(),
                ofertas.salarioMedioPorUbicacion().stream()
                        .map(f -> new Estadisticas.SalarioPorUbicacion(
                                (String) f[0],
                                ((Number) f[1]).intValue(),
                                ((Number) f[2]).longValue()))
                        .toList());
    }

    private OfertaAnalizada aDominio(OfertaEntity e) {
        return mapeador.aDominioAnalizada(e, fuentesDe(e.getId()));
    }

    private List<String> fuentesDe(Long ofertaId) {
        return apariciones.findByOfertaId(ofertaId).stream()
                .map(OfertaFuenteEntity::getFuente)
                .distinct()
                .toList();
    }
}
