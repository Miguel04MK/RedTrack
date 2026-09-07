package com.miguelalvarez.redtrack.dominio.puerto;

import com.miguelalvarez.redtrack.dominio.modelo.ResumenDiario;

/**
 * Salida de notificacion: Telegram, email, lo que sea.
 *
 * <p>Contrato: no propaga excepciones. Si el envio falla, lo registra. Un fallo
 * de Telegram no puede tumbar la tarea programada ni impedir que las ofertas
 * queden guardadas.
 */
public interface Notificador {

    String nombre();

    /**
     * Envia el resumen.
     *
     * @return true si el envio se confirmo; false si fallo (ya registrado).
     */
    boolean enviar(ResumenDiario resumen);
}
