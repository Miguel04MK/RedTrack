package com.miguelalvarez.redtrack.infraestructura.fuentes.remotive;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.miguelalvarez.redtrack.configuracion.RedTrackProperties;
import com.miguelalvarez.redtrack.dominio.modelo.CriterioBusqueda;
import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.servicio.Normalizador;
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
 * WireMock sirve una respuesta real de Remotive, con las descripciones
 * recortadas. Los tests corren offline y en CI.
 */
class RemotiveAdapterTest {

    private WireMockServer servidor;
    private RemotiveAdapter adaptador;

    @BeforeEach
    void levantarApiFalsa() {
        servidor = new WireMockServer(options().dynamicPort());
        servidor.start();
        servidor.stubFor(get(urlPathMatching("/api/remote-jobs"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("remotive-respuesta.json")));

        adaptador = new RemotiveAdapter(
                new RedTrackProperties(null, null,
                        new RedTrackProperties.Remotive(
                                true, "http://localhost:" + servidor.port(),
                                "software-dev", 1.08),
                        null, null),
                RestClient.builder(),
                new Normalizador());
    }

    @AfterEach
    void apagarApiFalsa() {
        servidor.stop();
    }

    @Test
    @DisplayName("descarta las ofertas que solo admiten candidatos en Estados Unidos")
    void descartaLasDeEstadosUnidos() {
        List<Oferta> ofertas = buscar();

        assertThat(ofertas).extracting(Oferta::empresa).doesNotContain("Stateside Inc");
        // De las cinco del fixture, la de USA es la unica que se cae aqui.
        assertThat(ofertas).hasSize(4);
    }

    @Test
    @DisplayName("limpia el HTML de la descripcion antes de guardarla")
    void limpiaElHtml() {
        Oferta junior = porTitulo("Junior Backend Developer");

        assertThat(junior.descripcion())
                .doesNotContain("<div", "<p>", "style=", "color: #333")
                .contains("Junior Backend Developer", "Java 21", "Spring Boot");
        // El contenido de <style> se va entero, no solo sus etiquetas.
        assertThat(junior.descripcion()).doesNotContain(".h2 {");
        // Y las entidades se traducen.
        assertThat(junior.descripcion()).contains("Benefits & perks");
    }

    @Test
    @DisplayName("toda oferta de Remotive es remota y en ingles")
    void siempreRemotaYEnIngles() {
        assertThat(buscar()).allSatisfy(o -> {
            assertThat(o.modalidad()).isEqualTo(Modalidad.REMOTO);
            assertThat(o.idioma()).isEqualTo(Idioma.EN);
            assertThat(o.fuente()).isEqualTo("remotive");
        });
    }

    @Test
    @DisplayName("normaliza el salario a bruto anual en euros")
    void normalizaElSalario() {
        // "$31,2k- $52k": la coma es decimal porque lleva k detras.
        Oferta junior = porTitulo("Junior Backend Developer");
        assertThat(junior.salarioMin()).isEqualTo(28889);   // 31.200 $ / 1,08
        assertThat(junior.salarioMax()).isEqualTo(48148);   // 52.000 $ / 1,08

        // "$14/hour" -> 14 x 1.720 h/ano / 1,08
        Oferta qa = porTitulo("Junior QA Tester");
        assertThat(qa.salarioMin()).isEqualTo(22296);
    }

    @Test
    @DisplayName("un salario que no es una cifra deja la banda vacia en vez de inventarla")
    void salarioNoNumericoNoSeInventa() {
        // "Pay per task". Adivinar aqui falsearia la nota: una oferta sin banda
        // puntua mejor que una con banda mala.
        Oferta copy = porTitulo("Freelance Copywriter");

        assertThat(copy.tieneSalario()).isFalse();
    }

    @Test
    @DisplayName("la categoria software-dev NO filtra: llegan copywriters")
    void laCategoriaNoFiltra() {
        // Documentado como test a proposito: es la razon de que el
        // ClasificadorDeOfertas tenga que mirar el titulo.
        assertThat(buscar()).extracting(Oferta::titulo).contains("Freelance Copywriter");
    }

    @Test
    @DisplayName("si Remotive devuelve 500 se registra y se sigue: lista vacia, sin excepcion")
    void unaFuenteCaidaNoTumbaLaRecoleccion() {
        servidor.resetAll();
        servidor.stubFor(get(urlPathMatching("/api/remote-jobs"))
                .willReturn(aResponse().withStatus(500)));

        assertThat(buscar()).isEmpty();
    }

    private List<Oferta> buscar() {
        return adaptador.buscar(CriterioBusqueda.de("java", "remoto"));
    }

    private Oferta porTitulo(String titulo) {
        return buscar().stream()
                .filter(o -> titulo.equals(o.titulo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No esta en el fixture: " + titulo));
    }
}
