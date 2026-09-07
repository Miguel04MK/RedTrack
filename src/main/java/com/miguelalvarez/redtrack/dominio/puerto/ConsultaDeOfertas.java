package com.miguelalvarez.redtrack.dominio.puerto;

import com.miguelalvarez.redtrack.dominio.modelo.FiltroOfertas;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;

import java.util.List;
import java.util.Optional;

/**
 * El lado de LECTURA, separado de {@link RepositorioOfertas}.
 *
 * <p>Son dos responsabilidades distintas y con formas distintas: el repositorio
 * sirve al ciclo de recoleccion (guardar, deduplicar, marcar procesadas) y esto
 * sirve a la API. Mezclarlas acabaria con un puerto de quince metodos donde la
 * mitad no la usa nadie.
 *
 * <p>Las ofertas se referencian por su HUELLA, igual que en el resto del
 * dominio: es una clave natural y unica, y evita exponer los ids de JPA.
 */
public interface ConsultaDeOfertas {

    List<OfertaAnalizada> buscar(FiltroOfertas filtro);

    Optional<OfertaAnalizada> porHuella(String huella);

    /** Agregados del mercado, calculados sobre todo lo recolectado. */
    Estadisticas estadisticas(int topTecnologias);

    /**
     * @param tecnologias las mas pedidas, de mayor a menor
     * @param salarios    salario medio por ubicacion, solo de las que publican banda
     */
    record Estadisticas(
            long ofertasAnalizadas,
            List<ConteoTecnologia> tecnologias,
            List<SalarioPorUbicacion> salarios
    ) {

        public record ConteoTecnologia(String tecnologia, long ofertas) {
        }

        public record SalarioPorUbicacion(String ubicacion, int salarioMedio, long ofertas) {
        }
    }
}
