package com.miguelalvarez.redtrack.configuracion;

import com.miguelalvarez.redtrack.dominio.puerto.AnalizadorSemantico;
import com.miguelalvarez.redtrack.infraestructura.ia.NuloAnalizador;
import com.miguelalvarez.redtrack.infraestructura.ia.OllamaAnalizador;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * La capa de IA, montada para que degrade con elegancia.
 *
 * <p>El sistema funciona completo sin el modelo. Si Ollama esta disponible,
 * anade un resumen; si no, se degrada silenciosamente. Una dependencia externa
 * opcional no debe poder tumbar el servicio.
 *
 * <p>El bean nulo es {@code @ConditionalOnMissingBean}: si el de Ollama no se
 * crea, siempre queda una implementacion. Nunca hay un null suelto.
 */
@Configuration
public class IaConfiguracion {

    @Bean
    @ConditionalOnProperty(name = "redtrack.ia.activa", havingValue = "true")
    AnalizadorSemantico ollamaAnalizador(RedTrackProperties propiedades,
                                         RestClient.Builder builder) {
        return new OllamaAnalizador(propiedades.ia(), builder);
    }

    @Bean
    @ConditionalOnMissingBean(AnalizadorSemantico.class)
    AnalizadorSemantico nuloAnalizador() {
        return new NuloAnalizador();
    }
}
