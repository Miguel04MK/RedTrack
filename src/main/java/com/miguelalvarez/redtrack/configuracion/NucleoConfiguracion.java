package com.miguelalvarez.redtrack.configuracion;

import com.miguelalvarez.redtrack.aplicacion.GenerarResumenDiarioUseCase;
import com.miguelalvarez.redtrack.aplicacion.RecolectarOfertasUseCase;
import com.miguelalvarez.redtrack.dominio.puerto.AnalizadorSemantico;
import com.miguelalvarez.redtrack.dominio.puerto.FuenteDeOfertas;
import com.miguelalvarez.redtrack.dominio.puerto.Notificador;
import com.miguelalvarez.redtrack.dominio.puerto.RepositorioOfertas;
import com.miguelalvarez.redtrack.dominio.servicio.Accesibilidad;
import com.miguelalvarez.redtrack.dominio.servicio.ClasificadorDeOfertas;
import com.miguelalvarez.redtrack.dominio.servicio.Deduplicador;
import com.miguelalvarez.redtrack.dominio.servicio.DetectorTecnologias;
import com.miguelalvarez.redtrack.dominio.servicio.ExtractorSenales;
import com.miguelalvarez.redtrack.dominio.servicio.Normalizador;
import com.miguelalvarez.redtrack.dominio.servicio.Puntuador;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

/**
 * Aqui se ensambla el nucleo.
 *
 * <p>Las clases de {@code dominio} no llevan anotaciones de Spring a proposito:
 * son Java puro y se instancian aqui. Asi sus tests no necesitan levantar
 * contexto.
 */
@Configuration
public class NucleoConfiguracion {

    @Bean
    Clock reloj() {
        return Clock.systemDefaultZone();
    }

    @Bean
    Normalizador normalizador() {
        return new Normalizador();
    }

    @Bean
    ExtractorSenales extractorSenales() {
        return new ExtractorSenales();
    }

    @Bean
    DetectorTecnologias detectorTecnologias() {
        return new DetectorTecnologias();
    }

    @Bean
    Deduplicador deduplicador(Normalizador normalizador, ExtractorSenales extractor) {
        return new Deduplicador(normalizador, extractor);
    }

    @Bean
    Accesibilidad accesibilidad() {
        return new Accesibilidad();
    }

    @Bean
    ClasificadorDeOfertas clasificadorDeOfertas(Accesibilidad accesibilidad) {
        return new ClasificadorDeOfertas(accesibilidad);
    }

    @Bean
    Puntuador puntuador(DetectorTecnologias detector, ExtractorSenales extractor,
                        Normalizador normalizador, Accesibilidad accesibilidad) {
        return new Puntuador(detector, extractor, normalizador, accesibilidad);
    }

    /**
     * Spring inyecta aqui TODAS las implementaciones de {@link FuenteDeOfertas}
     * que encuentre. Anadir una fuente es crear su bean: este metodo no se toca.
     */
    @Bean
    RecolectarOfertasUseCase recolectarOfertas(List<FuenteDeOfertas> fuentes,
                                               RepositorioOfertas repositorio,
                                               Normalizador normalizador,
                                               Deduplicador deduplicador,
                                               Puntuador puntuador,
                                               AnalizadorSemantico analizador) {
        return new RecolectarOfertasUseCase(
                fuentes, repositorio, normalizador, deduplicador, puntuador, analizador);
    }

    @Bean
    GenerarResumenDiarioUseCase generarResumenDiario(RepositorioOfertas repositorio,
                                                     ClasificadorDeOfertas clasificador,
                                                     Notificador notificador,
                                                     Clock reloj) {
        return new GenerarResumenDiarioUseCase(repositorio, clasificador, notificador, reloj);
    }
}
