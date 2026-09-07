package com.miguelalvarez.redtrack;

import com.miguelalvarez.redtrack.configuracion.RedTrackProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RedTrack: radar de ofertas de empleo.
 *
 * <p>Cada manana consulta APIs publicas de empleo, normaliza las ofertas a un
 * modelo comun, deduplica, puntua el encaje con el perfil y envia por Telegram
 * solo las nuevas que superan el umbral.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RedTrackProperties.class)
public class RedTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedTrackApplication.class, args);
    }
}
