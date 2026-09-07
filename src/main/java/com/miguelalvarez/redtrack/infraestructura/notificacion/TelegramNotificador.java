package com.miguelalvarez.redtrack.infraestructura.notificacion;

import com.miguelalvarez.redtrack.configuracion.RedTrackProperties;
import com.miguelalvarez.redtrack.dominio.modelo.ResumenDiario;
import com.miguelalvarez.redtrack.dominio.puerto.Notificador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Envia el resumen por Telegram.
 *
 * <p>Montarlo: hablar con &#64;BotFather, {@code /newbot}, y sacar el chat_id de
 * {@code https://api.telegram.org/bot<TOKEN>/getUpdates}.
 *
 * <p>El token llega SIEMPRE por variable de entorno. Nunca va en el codigo ni en
 * el yml.
 */
@Component
public class TelegramNotificador implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificador.class);
    public static final String NOMBRE = "telegram";

    private final RedTrackProperties.Telegram config;
    private final RestClient cliente;
    private final FormateadorTelegram formateador;

    public TelegramNotificador(RedTrackProperties propiedades,
                               RestClient.Builder builder,
                               FormateadorTelegram formateador) {
        this.config = propiedades.telegram();
        this.cliente = builder.baseUrl(config.url()).build();
        this.formateador = formateador;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public boolean enviar(ResumenDiario resumen) {
        if (!estaConfigurado()) {
            log.warn("Telegram sin configurar (falta TELEGRAM_BOT_TOKEN o TELEGRAM_CHAT_ID). "
                    + "El resumen de {} ofertas no se envia.", resumen.cuantas());
            return false;
        }
        try {
            cliente.post()
                    .uri("/bot{token}/sendMessage", config.botToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", config.chatId(),
                            "text", formateador.formatear(resumen),
                            "parse_mode", "Markdown",
                            "disable_web_page_preview", true))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            // Contrato del puerto: no propagar. Un fallo de Telegram no puede
            // tumbar la tarea diaria ni perder lo ya guardado.
            log.error("Fallo al enviar por Telegram: {}", e.getMessage());
            return false;
        }
    }

    private boolean estaConfigurado() {
        return config.activo()
                && config.botToken() != null && !config.botToken().isBlank()
                && config.chatId() != null && !config.chatId().isBlank();
    }
}
