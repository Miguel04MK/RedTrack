package com.miguelalvarez.redtrack.aplicacion;

import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.ResumenDiario;
import com.miguelalvarez.redtrack.dominio.puerto.Notificador;
import com.miguelalvarez.redtrack.dominio.puerto.RepositorioOfertas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Pasos 4 a 6 del flujo diario: filtrar por umbral, enviar y marcar como
 * notificadas.
 *
 * <p>El orden importa: solo se marcan como notificadas si el envio se confirmo.
 * Si Telegram falla, manana se reintentan en vez de perderse.
 */
public class GenerarResumenDiarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerarResumenDiarioUseCase.class);

    private final RepositorioOfertas repositorio;
    private final Notificador notificador;
    private final Clock reloj;

    public GenerarResumenDiarioUseCase(RepositorioOfertas repositorio,
                                       Notificador notificador,
                                       Clock reloj) {
        this.repositorio = repositorio;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    public ResumenDiario ejecutar(Perfil perfil) {
        int umbral = perfil.preferencias().umbralEncaje();
        List<OfertaAnalizada> pendientes = repositorio.pendientesDeNotificar(umbral);

        ResumenDiario resumen = new ResumenDiario(LocalDate.now(reloj), pendientes);
        if (resumen.estaVacio()) {
            log.info("Sin ofertas nuevas por encima de {}: no se envia nada", umbral);
            return resumen;
        }

        if (notificador.enviar(resumen)) {
            repositorio.marcarNotificadas(huellasDe(resumen), Instant.now(reloj));
            log.info("Resumen enviado por {} con {} ofertas",
                    notificador.nombre(), resumen.cuantas());
        } else {
            // No se marcan: se reintentan en la siguiente ejecucion.
            log.warn("Fallo el envio por {}: las {} ofertas quedan pendientes",
                    notificador.nombre(), resumen.cuantas());
        }
        return resumen;
    }

    private List<String> huellasDe(ResumenDiario resumen) {
        return resumen.ofertas().stream()
                .map(analizada -> analizada.oferta().huella())
                .toList();
    }
}
