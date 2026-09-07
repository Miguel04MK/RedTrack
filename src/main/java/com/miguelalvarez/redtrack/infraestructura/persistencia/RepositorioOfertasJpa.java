package com.miguelalvarez.redtrack.infraestructura.persistencia;

import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.puerto.RepositorioOfertas;
import com.miguelalvarez.redtrack.dominio.servicio.Normalizador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(RepositorioOfertasJpa.class);

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
    public void guardar(OfertaAnalizada analizada) {
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
    public void registrarAparicion(String huellaCanonica, String fuente,
                                   String idExterno, String url) {
        // La huella es la clave natural; aqui se traduce al id que necesita la
        // clave ajena, sin que eso salga del adaptador.
        ofertas.findByHuella(huellaCanonica).ifPresentOrElse(
                canonica -> apariciones.save(new OfertaFuenteEntity(
                        canonica.getId(), fuente, idExterno, url, Instant.now())),
                () -> log.warn("No existe oferta canonica con huella {}: "
                        + "no se registra la aparicion de {}/{}",
                        huellaCanonica, fuente, idExterno));
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
    public List<OfertaAnalizada> pendientes() {
        return ofertas.findByProcesadaEnIsNullOrderByEncajeDesc()
                .stream()
                .map(e -> mapeador.aDominioAnalizada(e, fuentesDe(e.getId())))
                .toList();
    }

    @Override
    @Transactional
    public void marcarProcesadas(List<String> huellas, Instant cuando) {
        if (huellas == null || huellas.isEmpty()) {
            return;
        }
        int marcadas = ofertas.marcarProcesadas(huellas, cuando);
        if (marcadas != huellas.size()) {
            log.warn("Se pidio marcar {} ofertas como procesadas y se marcaron {}",
                    huellas.size(), marcadas);
        }
    }

    private List<String> fuentesDe(Long ofertaId) {
        return apariciones.findByOfertaId(ofertaId).stream()
                .map(OfertaFuenteEntity::getFuente)
                .distinct()
                .toList();
    }
}
