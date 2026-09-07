package com.miguelalvarez.redtrack.infraestructura.persistencia;

import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.puerto.RepositorioOfertas;
import com.miguelalvarez.redtrack.dominio.servicio.Normalizador;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida de persistencia: implementa el puerto con Spring Data.
 */
@Repository
public class RepositorioOfertasJpa implements RepositorioOfertas {

    private final OfertaJpaRepository ofertas;
    private final OfertaFuenteJpaRepository apariciones;
    private final OfertaMapeador mapeador;
    private final Normalizador normalizador;

    public RepositorioOfertasJpa(OfertaJpaRepository ofertas,
                                 OfertaFuenteJpaRepository apariciones,
                                 OfertaMapeador mapeador,
                                 Normalizador normalizador) {
        this.ofertas = ofertas;
        this.apariciones = apariciones;
        this.mapeador = mapeador;
        this.normalizador = normalizador;
    }

    @Override
    @Transactional
    public Long guardar(OfertaAnalizada analizada) {
        Oferta oferta = analizada.oferta();
        OfertaEntity entidad = new OfertaEntity();

        mapeador.volcarEn(oferta, entidad);
        mapeador.volcarAnalisisEn(analizada.analisis(), entidad);
        entidad.setEmpresaNormalizada(normalizador.normalizar(oferta.empresa()));
        entidad.setUbicacionNormalizada(normalizador.normalizar(oferta.ubicacion()));

        OfertaEntity guardada = ofertas.save(entidad);
        apariciones.save(new OfertaFuenteEntity(
                guardada.getId(), oferta.fuente(), oferta.idExterno(),
                oferta.url(), Instant.now()));
        return guardada.getId();
    }

    @Override
    public Optional<Oferta> porHuella(String huella) {
        return ofertas.findByHuella(huella).map(mapeador::aDominio);
    }

    @Override
    public boolean existe(String fuente, String idExterno) {
        return ofertas.existsByFuenteAndIdExterno(fuente, idExterno)
                || apariciones.existsByFuenteAndIdExterno(fuente, idExterno);
    }

    @Override
    @Transactional
    public void registrarAparicion(Long idCanonica, String fuente, String idExterno, String url) {
        apariciones.save(new OfertaFuenteEntity(
                idCanonica, fuente, idExterno, url, Instant.now()));
    }

    @Override
    public List<Oferta> candidatasParaCotejar(String empresaNormalizada,
                                              String ubicacionNormalizada) {
        return ofertas
                .findByEmpresaNormalizadaAndUbicacionNormalizada(
                        empresaNormalizada, ubicacionNormalizada)
                .stream()
                .map(mapeador::aDominio)
                .toList();
    }

    @Override
    public List<OfertaAnalizada> pendientesDeNotificar(int umbralEncaje) {
        return ofertas
                .findByNotificadaEnIsNullAndEncajeGreaterThanEqualOrderByEncajeDesc(umbralEncaje)
                .stream()
                .map(e -> mapeador.aDominioAnalizada(e, fuentesDe(e.getId())))
                .toList();
    }

    @Override
    @Transactional
    public void marcarNotificadas(List<Long> ids, Instant cuando) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ofertas.marcarNotificadas(ids, cuando);
    }

    private List<String> fuentesDe(Long ofertaId) {
        return apariciones.findByOfertaId(ofertaId).stream()
                .map(OfertaFuenteEntity::getFuente)
                .distinct()
                .toList();
    }
}
