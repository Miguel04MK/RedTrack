package com.miguelalvarez.redtrack.aplicacion;

import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.ResumenDiario;
import com.miguelalvarez.redtrack.dominio.modelo.Seccion;
import com.miguelalvarez.redtrack.dominio.puerto.Notificador;
import com.miguelalvarez.redtrack.dominio.puerto.RepositorioOfertas;
import com.miguelalvarez.redtrack.dominio.servicio.ClasificadorDeOfertas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reparte las ofertas pendientes en las dos secciones del resumen, lo envia y
 * marca lo procesado.
 *
 * <p>El orden importa: solo se marca si el envio se confirmo. Si el notificador
 * falla, todo sigue pendiente y se reintenta manana en vez de perderse.
 *
 * <p>Se marcan TODAS las ofertas evaluadas, tambien las que no entran en ninguna
 * seccion. Una oferta se considera una vez, el dia que aparece: si no entro hoy,
 * no va a entrar manana, porque el veredicto es determinista. Asi el conjunto de
 * pendientes no crece sin limite.
 */
public class GenerarResumenDiarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerarResumenDiarioUseCase.class);

    private final RepositorioOfertas repositorio;
    private final ClasificadorDeOfertas clasificador;
    private final Notificador notificador;
    private final Clock reloj;

    public GenerarResumenDiarioUseCase(RepositorioOfertas repositorio,
                                       ClasificadorDeOfertas clasificador,
                                       Notificador notificador,
                                       Clock reloj) {
        this.repositorio = repositorio;
        this.clasificador = clasificador;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    public ResumenDiario ejecutar(Perfil perfil) {
        List<OfertaAnalizada> pendientes = repositorio.pendientes();

        List<OfertaAnalizada> paraTi = new ArrayList<>();
        List<OfertaAnalizada> podrianInteresarte = new ArrayList<>();

        for (OfertaAnalizada analizada : pendientes) {
            Seccion seccion = clasificador.clasificar(
                    analizada.oferta(), analizada.analisis(), perfil);
            if (seccion == Seccion.PARA_TI) {
                paraTi.add(analizada);
            } else if (seccion == Seccion.PODRIA_INTERESARTE) {
                podrianInteresarte.add(analizada);
            }
        }

        ResumenDiario resumen = new ResumenDiario(
                LocalDate.now(reloj), paraTi, podrianInteresarte);

        if (resumen.estaVacio()) {
            log.info("Nada que enviar: {} ofertas evaluadas, ninguna entra en el resumen",
                    pendientes.size());
            marcar(pendientes);
            return resumen;
        }

        if (notificador.enviar(resumen)) {
            marcar(pendientes);
            log.info("Resumen enviado por {}: {} para ti, {} podrian interesarte",
                    notificador.nombre(), resumen.paraTi().size(),
                    resumen.podrianInteresarte().size());
        } else {
            // No se marca nada: se reintenta entero en la siguiente ejecucion.
            log.warn("Fallo el envio por {}: las {} ofertas quedan pendientes",
                    notificador.nombre(), resumen.cuantas());
        }
        return resumen;
    }

    private void marcar(List<OfertaAnalizada> ofertas) {
        List<String> huellas = ofertas.stream()
                .map(analizada -> analizada.oferta().huella())
                .toList();
        repositorio.marcarProcesadas(huellas, Instant.now(reloj));
    }
}
