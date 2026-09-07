package com.miguelalvarez.redtrack.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuracion del servicio, bajo el prefijo {@code redtrack} en application.yml.
 *
 * <p>Los secretos (claves de Adzuna, token de Telegram) llegan por variable de
 * entorno; aqui solo se declaran los nombres. Nunca se escriben valores reales
 * en el yml.
 */
@ConfigurationProperties(prefix = "redtrack")
public record RedTrackProperties(
        Busquedas busquedas,
        Adzuna adzuna,
        Remotive remotive,
        Telegram telegram,
        Ia ia,
        Api api
) {

    /**
     * @param token protege POST /api/recolectar y POST /api/resumen. Si esta
     *              vacio, esos endpoints se deshabilitan con un 503 en vez de
     *              quedar abiertos: un despliegue al que se le olvida la
     *              variable tiene que romperse de forma evidente, no quedarse
     *              en barra libre.
     */
    public record Api(String token) {
    }

    /**
     * Que se busca.
     *
     * @param terminos             el stack propio, para la seccion "para ti"
     * @param terminosExploratorios puestos junior de desarrollo de cualquier
     *                             stack, para la seccion "podrian interesarte".
     *                             Tienen que ser especificos: buscar "junior" a
     *                             secas devuelve Territory Managers y practicas
     *                             de RRHH, y la categoria de Adzuna no sirve
     *                             para filtrarlo porque la mayoria de ofertas
     *                             de informatica vienen sin categoria.
     */
    public record Busquedas(
            List<String> terminos,
            List<String> terminosExploratorios,
            List<String> ubicaciones,
            int maxDiasAntiguedad,
            int maxResultadosPorFuente
    ) {
    }

    public record Adzuna(
            boolean activa,
            String url,
            String pais,
            String appId,
            String appKey
    ) {
    }

    /**
     * @param dolaresPorEuro Remotive publica los salarios en dolares y en texto
     *                       libre. Una aproximacion basta: la banda ya es
     *                       orientativa, y no merece la pena una llamada a una
     *                       API de divisas para esto.
     */
    public record Remotive(
            boolean activa,
            String url,
            String categoria,
            double dolaresPorEuro
    ) {
    }

    public record Telegram(
            boolean activo,
            String url,
            String botToken,
            String chatId
    ) {
    }

    /**
     * IA local, opcional.
     *
     * <p>En perfil {@code prod} va SIEMPRE desactivada: una t2.micro tiene 1 GB
     * de RAM y no mueve el modelo.
     *
     * @param timeout corto a proposito. Con 30 ofertas, un timeout largo hace
     *                que la tarea diaria no termine nunca.
     */
    public record Ia(
            boolean activa,
            String url,
            String modelo,
            Duration timeout
    ) {
    }
}
