package com.miguelalvarez.redtrack.infraestructura.planificacion;

import com.miguelalvarez.redtrack.aplicacion.GenerarResumenDiarioUseCase;
import com.miguelalvarez.redtrack.aplicacion.RecolectarOfertasUseCase;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.ResumenDiario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Modo "una pasada": arranca, recolecta, envia el resumen y se muere.
 *
 * <p>Es el mismo trabajo que hace {@link TareaDiaria} a las 8:00, pero sin
 * servidor y sin quedarse esperando. Sirve para ejecutarlo desde fuera: un cron
 * del sistema, un workflow programado de GitHub Actions, o lo que sea.
 *
 * <p><b>El nucleo no se entera.</b> Mismos casos de uso, mismos puertos, mismos
 * adaptadores. Lo unico que cambia es quien dispara: antes una anotacion
 * {@code @Scheduled}, aqui un {@link ApplicationRunner}. Eso es exactamente lo
 * que compra la arquitectura de puertos y adaptadores.
 *
 * <p>Se activa con el perfil {@code una-pasada}, que ademas apaga el servidor
 * web y la planificacion.
 *
 * <p>El <b>codigo de salida</b> refleja si fue bien: 0 correcto, 1 si algo
 * fallo. Asi un workflow que lo ejecute se pone en rojo cuando el radar se
 * rompe, en vez de fallar en silencio.
 */
@Component
@Profile("una-pasada")
public class PasadaUnica implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasadaUnica.class);

    private final RecolectarOfertasUseCase recolectar;
    private final GenerarResumenDiarioUseCase resumir;
    private final CriteriosDeBusqueda criterios;
    private final Perfil perfil;
    private final ApplicationContext contexto;

    public PasadaUnica(RecolectarOfertasUseCase recolectar,
                       GenerarResumenDiarioUseCase resumir,
                       CriteriosDeBusqueda criterios,
                       Perfil perfil,
                       ApplicationContext contexto) {
        this.recolectar = recolectar;
        this.resumir = resumir;
        this.criterios = criterios;
        this.perfil = perfil;
        this.contexto = contexto;
    }

    @Override
    public void run(ApplicationArguments args) {
        int codigo = 0;
        try {
            log.info("Pasada unica: arranca la recoleccion");
            RecolectarOfertasUseCase.Resultado resultado = recolectar.ejecutar(
                    criterios.todos(), perfil);
            log.info("Recolectadas {} nuevas de {} vistas ({} duplicadas)",
                    resultado.nuevas(), resultado.vistas(), resultado.duplicadas());

            ResumenDiario resumen = resumir.ejecutar(perfil);
            log.info("Pasada unica terminada: {} para ti, {} podrian interesarte",
                    resumen.paraTi().size(), resumen.podrianInteresarte().size());

        } catch (RuntimeException e) {
            log.error("La pasada unica ha fallado", e);
            codigo = 1;
        }

        // Cierre explicito. Sin esto, cualquier hilo no demonio que quede vivo
        // dejaria el proceso colgado, y un workflow programado se quedaria
        // esperando hasta agotar su tiempo.
        int salida = codigo;
        System.exit(SpringApplication.exit(contexto, () -> salida));
    }
}
