package com.miguelalvarez.redtrack.configuracion;

import com.miguelalvarez.redtrack.infraestructura.web.FiltroDeToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra el filtro que protege los endpoints que hacen cosas.
 *
 * <p>Solo tiene sentido cuando hay servidor web: en el modo "una pasada" no se
 * levanta ninguno.
 */
@Configuration
@ConditionalOnWebApplication
public class WebConfiguracion {

    @Bean
    FilterRegistrationBean<FiltroDeToken> filtroDeToken(RedTrackProperties propiedades) {
        String token = propiedades.api() == null ? null : propiedades.api().token();

        FilterRegistrationBean<FiltroDeToken> registro = new FilterRegistrationBean<>();
        registro.setFilter(new FiltroDeToken(token));
        registro.addUrlPatterns("/api/*");
        registro.setName("filtroDeToken");
        return registro;
    }
}
