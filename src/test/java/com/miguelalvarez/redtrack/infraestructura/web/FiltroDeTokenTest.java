package com.miguelalvarez.redtrack.infraestructura.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroDeTokenTest {

    private static final String TOKEN = "un-token-largo-y-secreto";

    private final MockHttpServletResponse respuesta = new MockHttpServletResponse();
    private final MockFilterChain cadena = new MockFilterChain();

    @Test
    @DisplayName("las consultas pasan sin token")
    void lasConsultasPasan() throws Exception {
        filtrar(new FiltroDeToken(TOKEN), peticion("GET", "/api/ofertas"));

        assertThat(paso()).isTrue();
        assertThat(respuesta.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("sin token, un endpoint protegido devuelve 401")
    void sinTokenNoPasa() throws Exception {
        filtrar(new FiltroDeToken(TOKEN), peticion("POST", "/api/recolectar"));

        assertThat(paso()).isFalse();
        assertThat(respuesta.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("con un token equivocado tampoco")
    void conTokenMaloNoPasa() throws Exception {
        MockHttpServletRequest peticion = peticion("POST", "/api/resumen");
        peticion.addHeader(FiltroDeToken.CABECERA, "no-es-este");

        filtrar(new FiltroDeToken(TOKEN), peticion);

        assertThat(paso()).isFalse();
        assertThat(respuesta.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("con el token correcto pasa")
    void conElTokenCorrectoPasa() throws Exception {
        MockHttpServletRequest peticion = peticion("POST", "/api/resumen");
        peticion.addHeader(FiltroDeToken.CABECERA, TOKEN);

        filtrar(new FiltroDeToken(TOKEN), peticion);

        assertThat(paso()).isTrue();
    }

    @Test
    @DisplayName("SIN TOKEN CONFIGURADO, los protegidos se cierran con 503, no se abren")
    void sinTokenConfiguradoFallaCerrado() throws Exception {
        // Lo importante de todo el filtro. Un despliegue al que se le olvida la
        // variable de entorno tiene que romperse de forma evidente, nunca
        // quedarse en barra libre.
        MockHttpServletRequest peticion = peticion("POST", "/api/recolectar");
        peticion.addHeader(FiltroDeToken.CABECERA, "lo-que-sea");

        filtrar(new FiltroDeToken("   "), peticion);

        assertThat(paso()).isFalse();
        assertThat(respuesta.getStatus()).isEqualTo(503);
    }

    @Test
    @DisplayName("sin token configurado, las consultas siguen abiertas")
    void sinTokenConfiguradoLasConsultasSiguenAbiertas() throws Exception {
        filtrar(new FiltroDeToken(null), peticion("GET", "/api/estadisticas"));

        assertThat(paso()).isTrue();
    }

    // ------------------------------------------------------------------

    private MockHttpServletRequest peticion(String metodo, String ruta) {
        MockHttpServletRequest peticion = new MockHttpServletRequest(metodo, ruta);
        peticion.setRequestURI(ruta);
        return peticion;
    }

    private void filtrar(FiltroDeToken filtro, MockHttpServletRequest peticion) throws Exception {
        filtro.doFilter(peticion, respuesta, cadena);
    }

    /** true si la peticion llego al otro lado del filtro. */
    private boolean paso() {
        FilterChain c = cadena;
        return ((MockFilterChain) c).getRequest() != null;
    }
}
