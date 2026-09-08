package com.miguelalvarez.redtrack.infraestructura.planificacion;

import com.miguelalvarez.redtrack.configuracion.RedTrackProperties;
import com.miguelalvarez.redtrack.dominio.modelo.CriterioBusqueda;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Traduce la configuracion de busquedas a criterios de dominio.
 *
 * <p>Vive aparte de {@link TareaDiaria} porque tiene tres usuarios que no
 * tienen nada que ver entre si: la tarea programada, el endpoint de
 * recoleccion manual y el modo "una pasada". Tenerlo dentro de la tarea
 * obligaba a los otros dos a depender de un cron que no les importa.
 */
@Component
public class CriteriosDeBusqueda {

    private final RedTrackProperties propiedades;

    public CriteriosDeBusqueda(RedTrackProperties propiedades) {
        this.propiedades = propiedades;
    }

    /**
     * Producto cartesiano de terminos x ubicaciones.
     *
     * <p>Incluye los exploratorios: alimentan la seccion "podrian interesarte".
     * Van en la misma recoleccion porque una oferta no sabe a que seccion
     * pertenece: eso lo decide el clasificador despues, sobre la oferta ya
     * normalizada y puntuada.
     */
    public List<CriterioBusqueda> todos() {
        RedTrackProperties.Busquedas b = propiedades.busquedas();

        List<String> terminos = new ArrayList<>(b.terminos());
        if (b.terminosExploratorios() != null) {
            terminos.addAll(b.terminosExploratorios());
        }

        List<CriterioBusqueda> criterios = new ArrayList<>();
        for (String termino : terminos) {
            for (String ubicacion : b.ubicaciones()) {
                criterios.add(new CriterioBusqueda(
                        termino, ubicacion, b.maxDiasAntiguedad(), b.maxResultadosPorFuente()));
            }
        }
        return criterios;
    }
}
