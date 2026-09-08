package com.miguelalvarez.redtrack.infraestructura.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/**
 * Protege los endpoints que HACEN cosas.
 *
 * <p>{@code POST /api/recolectar} y {@code POST /api/resumen} no son consultas:
 * el primero gasta cuota de Adzuna y el segundo hace que el bot te escriba.
 * Expuestos en internet sin proteccion, cualquiera puede dispararlos.
 *
 * <p>Las consultas ({@code GET}) se dejan abiertas: no cuestan nada y no
 * cambian nada.
 *
 * <p><b>Falla cerrado.</b> Si no hay token configurado, los endpoints
 * protegidos devuelven 503 en vez de quedar abiertos. Un despliegue al que se
 * le olvida la variable de entorno tiene que romperse de forma evidente, no
 * quedarse en barra libre.
 *
 * <p>No se usa Spring Security a proposito: para un servicio de un solo usuario
 * con dos endpoints, un token en cabecera es proporcionado y se entiende de una
 * lectura. Si algun dia se publica la API, esto se sustituye por autenticacion
 * de verdad.
 */
public class FiltroDeToken extends OncePerRequestFilter {

    public static final String CABECERA = "X-RedTrack-Token";

    private static final Logger log = LoggerFactory.getLogger(FiltroDeToken.class);

    private static final Set<String> PROTEGIDAS = Set.of(
            "/api/recolectar",
            "/api/resumen");

    private final String token;

    public FiltroDeToken(String token) {
        this.token = token;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {

        if (!PROTEGIDAS.contains(peticion.getRequestURI())) {
            cadena.doFilter(peticion, respuesta);
            return;
        }

        if (token == null || token.isBlank()) {
            log.error("{} esta protegido pero no hay REDTRACK_API_TOKEN configurado",
                    peticion.getRequestURI());
            responder(respuesta, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Endpoint deshabilitado: falta configurar REDTRACK_API_TOKEN");
            return;
        }

        if (!coincide(peticion.getHeader(CABECERA))) {
            log.warn("Intento no autorizado contra {} desde {}",
                    peticion.getRequestURI(), peticion.getRemoteAddr());
            responder(respuesta, HttpServletResponse.SC_UNAUTHORIZED,
                    "Falta o no es valida la cabecera " + CABECERA);
            return;
        }

        cadena.doFilter(peticion, respuesta);
    }

    /**
     * Comparacion en tiempo constante.
     *
     * <p>Un {@code equals} normal corta en el primer caracter distinto, y la
     * diferencia de tiempo permite adivinar el token caracter a caracter. Con dos
     * endpoints y un solo usuario el riesgo es teorico, pero comparar secretos
     * asi no cuesta nada. Ver {@link MessageDigest#isEqual}.
     */
    private boolean coincide(String recibido) {
        if (recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(
                recibido.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    private void responder(HttpServletResponse respuesta, int codigo, String mensaje)
            throws IOException {
        respuesta.setStatus(codigo);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}
