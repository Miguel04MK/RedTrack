package com.miguelalvarez.redtrack.infraestructura.fuentes.adzuna;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.miguelalvarez.redtrack.configuracion.RedTrackProperties;
import com.miguelalvarez.redtrack.dominio.modelo.CriterioBusqueda;
import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.servicio.ExtractorSenales;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * WireMock simula la API de Adzuna.
 *
 * <p>Consecuencia: estos tests corren OFFLINE y en CI. Que Adzuna este caida, que
 * cambie su rate limit o que no haya claves configuradas no rompe la build.
 */
class AdzunaAdapterTest {

    private WireMockServer servidor;
    private AdzunaAdapter adaptador;

    @BeforeEach
    void levantarApiFalsa() {
        servidor = new WireMockServer(options().dynamicPort());
        servidor.start();
        adaptador = new AdzunaAdapter(
                propiedadesApuntandoA("http://localhost:" + servidor.port()),
                RestClient.builder(),
                new ExtractorSenales());
    }

    @AfterEach
    void apagarApiFalsa() {
        servidor.stop();
    }

    @Test
    @DisplayName("mapea la respuesta de Adzuna al modelo canonico")
    void mapeaLaRespuesta() {
        servidor.stubFor(get(urlPathMatching("/v1/api/jobs/es/search/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("adzuna-respuesta.json")));

        List<Oferta> ofertas = adaptador.buscar(CriterioBusqueda.de("java spring", "galicia"));

        assertThat(ofertas).hasSize(2);

        Oferta primera = ofertas.getFirst();
        assertThat(primera.fuente()).isEqualTo("adzuna");
        assertThat(primera.idExterno()).isEqualTo("4912345678");
        assertThat(primera.titulo()).isEqualTo("Desarrollador/a Java Junior");
        assertThat(primera.empresa()).isEqualTo("Coremain");
        assertThat(primera.modalidad()).isEqualTo(Modalidad.HIBRIDO);
        assertThat(primera.salarioMin()).isEqualTo(21000);
        assertThat(primera.salarioMax()).isEqualTo(25000);
        assertThat(primera.idioma()).isEqualTo(Idioma.ES);
        assertThat(primera.url()).contains("adzuna.es");
    }

    @Test
    @DisplayName("una oferta sin salario no rompe el mapeo")
    void salarioAusenteEsNull() {
        servidor.stubFor(get(urlPathMatching("/v1/api/jobs/es/search/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("adzuna-respuesta.json")));

        Oferta segunda = adaptador.buscar(CriterioBusqueda.de("java", "madrid")).get(1);

        assertThat(segunda.tieneSalario()).isFalse();
        assertThat(segunda.salarioMin()).isNull();
        assertThat(segunda.idioma()).isEqualTo(Idioma.EN);
    }

    @Test
    @DisplayName("si Adzuna devuelve 500 se registra y se sigue: lista vacia, sin excepcion")
    void unaFuenteCaidaNoTumbaLaRecoleccion() {
        servidor.stubFor(get(urlPathMatching("/v1/api/jobs/.*"))
                .willReturn(aResponse().withStatus(500)));

        assertThat(adaptador.buscar(CriterioBusqueda.de("java", "galicia"))).isEmpty();
    }

    @Test
    @DisplayName("sin claves configuradas la fuente se declara inactiva")
    void sinClavesNoEstaActiva() {
        AdzunaAdapter sinClaves = new AdzunaAdapter(
                new RedTrackProperties(
                        new RedTrackProperties.Adzuna(true, "http://localhost", "es", "", ""),
                        null, null, null, null),
                RestClient.builder(),
                new ExtractorSenales());

        assertThat(sinClaves.estaActiva()).isFalse();
    }

    private RedTrackProperties propiedadesApuntandoA(String url) {
        return new RedTrackProperties(
                new RedTrackProperties.Adzuna(true, url, "es", "app-id", "app-key"),
                null, null, null, null);
    }
}
