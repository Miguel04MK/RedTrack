package com.miguelalvarez.redtrack.dominio.puerto;

import com.miguelalvarez.redtrack.dominio.modelo.CriterioBusqueda;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;

import java.util.List;

/**
 * EL puerto del proyecto.
 *
 * <p>Anadir una fuente nueva es implementar esta interfaz y registrar el bean.
 * El nucleo no se toca.
 *
 * <p>Contrato: una implementacion NUNCA propaga excepciones de red. Si su API
 * falla, registra el error y devuelve lista vacia. Que Remotive este caida no
 * puede impedir que lleguen las ofertas de Adzuna.
 */
public interface FuenteDeOfertas {

    /** Identificador corto y estable: "adzuna", "remotive"... Va en {@code Oferta.fuente}. */
    String nombre();

    /** Ofertas en bruto de esta fuente, ya mapeadas al modelo canonico. */
    List<Oferta> buscar(CriterioBusqueda criterio);

    /** Permite desactivar una fuente por configuracion sin borrar su bean. */
    default boolean estaActiva() {
        return true;
    }
}
