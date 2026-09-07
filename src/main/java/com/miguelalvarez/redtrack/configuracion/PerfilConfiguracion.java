package com.miguelalvarez.redtrack.configuracion;

import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.infraestructura.perfil.CargadorDePerfil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Carga el perfil al arrancar.
 *
 * <p>La ruta se puede sobreescribir con {@code redtrack.perfil.ruta} para apuntar
 * a un fichero fuera del jar (por ejemplo, un volumen montado en la EC2) sin
 * tener que reconstruir la imagen.
 */
@Configuration
public class PerfilConfiguracion {

    @Bean
    CargadorDePerfil cargadorDePerfil(
            @Value("${redtrack.perfil.ruta:classpath:perfil.yaml}") Resource recurso) {
        return new CargadorDePerfil(recurso);
    }

    @Bean
    Perfil perfil(CargadorDePerfil cargador) {
        return cargador.cargar();
    }
}
