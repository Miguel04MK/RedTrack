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

    /**
     * Las tecnologias mas pedidas.
     *
     * <p>Consulta nativa porque {@code tecnologias_pedidas} se guarda como texto
     * separado por comas y hay que desplegarlo con {@code unnest}, que es de
     * Postgres y no existe en JPQL.
     *
     * <p>Devuelve {@code Object[]} en vez de una proyeccion por interfaz para no
     * depender de como Postgres trata las mayusculas de los alias.
     *
     * @return filas de [tecnologia (String), ofertas (Number)]
     */
    @Query(value = """
            SELECT trim(t) AS tecnologia, COUNT(*) AS ofertas
            FROM oferta, unnest(string_to_array(tecnologias_pedidas, ',')) AS t
            WHERE tecnologias_pedidas IS NOT NULL AND tecnologias_pedidas <> ''
            GROUP BY trim(t)
            ORDER BY COUNT(*) DESC
            LIMIT :top
            """, nativeQuery = true)
    List<Object[]> tecnologiasMasPedidas(@Param("top") int top);

    /**
     * Salario medio por ubicacion, solo de las ofertas que publican banda.
     *
     * @return filas de [ubicacion (String), salarioMedio (Number), ofertas (Number)]
     */
    @Query(value = """
            SELECT ubicacion_normalizada, ROUND(AVG((salario_min + salario_max) / 2.0)),
                   COUNT(*)
            FROM oferta
            WHERE salario_min IS NOT NULL
              AND salario_max IS NOT NULL
              AND ubicacion_normalizada IS NOT NULL
              AND ubicacion_normalizada <> ''
            GROUP BY ubicacion_normalizada
            ORDER BY 2 DESC
            """, nativeQuery = true)
    List<Object[]> salarioMedioPorUbicacion();
}
