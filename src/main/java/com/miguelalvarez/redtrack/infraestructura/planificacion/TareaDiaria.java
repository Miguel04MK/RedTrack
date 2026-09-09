package com.miguelalvarez.redtrack.infraestructura.planificacion;

import com.miguelalvarez.redtrack.aplicacion.GenerarResumenDiarioUseCase;
import com.miguelalvarez.redtrack.aplicacion.RecolectarOfertasUseCase;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * El disparador cuando el servicio esta encendido: cada manana laborable a las
 * 8:00.
 *
 * <p>Hace exactamente lo mismo que {@link PasadaUnica}; lo unico que cambia es
 * quien lo llama. Si el servicio no va a estar encendido de forma permanente,
 * usa el perfil {@code una-pasada} y un cron de fuera.
 */
@Component
public class TareaDiaria {

    private static final Logger log = LoggerFactory.getLogger(TareaDiaria.class);

    private final RecolectarOfertasUseCase recolectar;
    private final GenerarResumenDiarioUseCase resumir;
    private final Perfil perfil;

    public TareaDiaria(RecolectarOfertasUseCase recolectar,
                       GenerarResumenDiarioUseCase resumir,
                       Perfil perfil) {
        this.recolectar = recolectar;
        this.resumir = resumir;
        this.perfil = perfil;
    }

    @Scheduled(cron = "${redtrack.cron:0 0 8 * * MON-FRI}", zone = "Europe/Madrid")
    public void ejecutar() {
        log.info("Arranca la recoleccion diaria");
        RecolectarOfertasUseCase.Resultado resultado = recolectar.ejecutar(
                perfil.criterios(), perfil);
        log.info("Recolectadas {} ofertas nuevas de {} vistas",
                resultado.nuevas(), resultado.vistas());
        resumir.ejecutar(perfil);
    }
}
