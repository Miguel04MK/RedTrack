package com.miguelalvarez.redtrack.infraestructura.planificacion;

import com.miguelalvarez.redtrack.aplicacion.GenerarResumenDiarioUseCase;
import com.miguelalvarez.redtrack.aplicacion.RecolectarOfertasUseCase;
import com.miguelalvarez.redtrack.configuracion.RedTrackProperties;
import com.miguelalvarez.redtrack.dominio.modelo.CriterioBusqueda;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * El disparador de todo: cada manana laborable a las 8:00.
 */
@Component
public class TareaDiaria {

    private static final Logger log = LoggerFactory.getLogger(TareaDiaria.class);

    private final RecolectarOfertasUseCase recolectar;
    private final GenerarResumenDiarioUseCase resumir;
    private final RedTrackProperties propiedades;
    private final Perfil perfil;

    public TareaDiaria(RecolectarOfertasUseCase recolectar,
                       GenerarResumenDiarioUseCase resumir,
                       RedTrackProperties propiedades,
                       Perfil perfil) {
        this.recolectar = recolectar;
        this.resumir = resumir;
        this.propiedades = propiedades;
        this.perfil = perfil;
    }

    @Scheduled(cron = "${redtrack.cron:0 0 8 * * MON-FRI}", zone = "Europe/Madrid")
    public void ejecutar() {
        log.info("Arranca la recoleccion diaria");
        RecolectarOfertasUseCase.Resultado resultado = recolectar.ejecutar(criterios(), perfil);
        log.info("Recolectadas {} ofertas nuevas de {} vistas",
                resultado.nuevas(), resultado.vistas());
        resumir.ejecutar(perfil);
    }

    /** Producto cartesiano de terminos x ubicaciones configurados. */
    public List<CriterioBusqueda> criterios() {
        RedTrackProperties.Busquedas b = propiedades.busquedas();
        List<CriterioBusqueda> criterios = new ArrayList<>();
        for (String termino : b.terminos()) {
            for (String ubicacion : b.ubicaciones()) {
                criterios.add(new CriterioBusqueda(
                        termino, ubicacion, b.maxDiasAntiguedad(), b.maxResultadosPorFuente()));
            }
        }
        return criterios;
    }
}
