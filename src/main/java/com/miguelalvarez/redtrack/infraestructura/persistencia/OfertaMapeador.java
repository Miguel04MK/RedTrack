package com.miguelalvarez.redtrack.infraestructura.persistencia;

import com.miguelalvarez.redtrack.dominio.modelo.Analisis;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Frontera entre la entidad de JPA y el modelo de dominio.
 *
 * <p>Los nombres coinciden, asi que MapStruct genera casi todo. Las listas se
 * guardan como texto separado por comas y se convierten a mano.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfertaMapeador {

    String SEPARADOR = ",";

    Oferta aDominio(OfertaEntity entidad);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "notificadaEn", ignore = true)
    void volcarEn(Oferta oferta, @MappingTarget OfertaEntity entidad);

    /** Entidad -> par (oferta, analisis) tal y como lo consume el resumen. */
    default OfertaAnalizada aDominioAnalizada(OfertaEntity e, List<String> vistaEn) {
        Analisis analisis = new Analisis(
                e.getEncaje(),
                aConjunto(e.getTecnologiasPedidas()),
                aConjunto(e.getLasTengo()),
                aConjunto(e.getMeFaltan()),
                e.getSenal(),
                e.getAnosRequeridos(),
                aLista(e.getBanderasRojas()),
                e.getResumenIa());
        return new OfertaAnalizada(aDominio(e), analisis, vistaEn);
    }

    /** Vuelca oferta y analisis sobre una entidad nueva o existente. */
    default void volcarAnalisisEn(Analisis analisis, OfertaEntity entidad) {
        entidad.setEncaje(analisis.encaje());
        entidad.setSenal(analisis.senal());
        entidad.setAnosRequeridos(analisis.anosRequeridos());
        entidad.setTecnologiasPedidas(aTexto(analisis.tecnologiasPedidas()));
        entidad.setLasTengo(aTexto(analisis.lasTengo()));
        entidad.setMeFaltan(aTexto(analisis.meFaltan()));
        entidad.setBanderasRojas(aTexto(analisis.banderasRojas()));
        entidad.setResumenIa(analisis.resumenIA());
    }

    default String aTexto(java.util.Collection<String> valores) {
        return valores == null || valores.isEmpty() ? null : String.join(SEPARADOR, valores);
    }

    default List<String> aLista(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split(SEPARADOR)).map(String::trim).toList();
    }

    default Set<String> aConjunto(String texto) {
        return new LinkedHashSet<>(aLista(texto));
    }
}
