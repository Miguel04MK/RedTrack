package com.miguelalvarez.redtrack.infraestructura.fuentes.remotive;

import com.miguelalvarez.redtrack.configuracion.RedTrackProperties;
import com.miguelalvarez.redtrack.dominio.modelo.CriterioBusqueda;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.puerto.FuenteDeOfertas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Segunda fuente: empleo remoto.
 *
 * <p>Con dos fuentes ya existe el problema de deduplicacion y de normalizacion,
 * que es lo interesante. La tercera y la cuarta ya no ensenan nada nuevo.
 *
 * <p>ANTES DE IMPLEMENTAR: hazle un {@code curl}. El guion avisa de que las APIs
 * abiertas de empleo aparecen y desaparecen, y la unica verificada es Adzuna.
 *
 * <pre>
 *   curl "https://remotive.com/api/remote-jobs?category=software-dev&amp;limit=3"
 * </pre>
 *
 * <p>Ojo: son ofertas mayoritariamente en ingles y muchas piden nivel alto.
 * Por eso {@code Oferta.idioma} existe y el puntuador las trata aparte.
 */
@Component
public class RemotiveAdapter implements FuenteDeOfertas {

    private static final Logger log = LoggerFactory.getLogger(RemotiveAdapter.class);
    public static final String NOMBRE = "remotive";

    private final RedTrackProperties.Remotive config;
    private final RestClient cliente;

    public RemotiveAdapter(RedTrackProperties propiedades, RestClient.Builder builder) {
        this.config = propiedades.remotive();
        this.cliente = builder.baseUrl(config.url()).build();
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public boolean estaActiva() {
        return config.activa();
    }

    @Override
    public List<Oferta> buscar(CriterioBusqueda criterio) {
        // TODO(fase-3): implementar. Pasos:
        //   1. curl a la API y guardar una respuesta real en
        //      src/test/resources/remotive-respuesta.json
        //   2. escribir el DTO RemotiveRespuesta a partir de ese JSON
        //   3. mapear a Oferta: modalidad SIEMPRE REMOTO, idioma EN,
        //      salario en texto libre -> normalizar a bruto anual en euros
        //   4. test con WireMock sirviendo ese JSON
        log.debug("RemotiveAdapter aun no implementado; devuelve vacio");
        return List.of();
    }
}
