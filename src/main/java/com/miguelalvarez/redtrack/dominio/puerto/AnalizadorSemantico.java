package com.miguelalvarez.redtrack.dominio.puerto;

import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;

import java.util.Optional;

/**
 * Capa de IA, opcional por diseno.
 *
 * <p>El sistema funciona completo sin el modelo. Si Ollama esta disponible,
 * anade un resumen; si no, se degrada silenciosamente. Una dependencia externa
 * opcional no debe poder tumbar el servicio.
 *
 * <p>Por eso el metodo devuelve {@link Optional} y no lanza: la ausencia de
 * resumen es un resultado valido, no un error.
 */
public interface AnalizadorSemantico {

    /**
     * Resumen en lenguaje natural de que pide la oferta frente a lo que ofrece
     * el perfil.
     *
     * @return vacio si el analizador no esta disponible, ha excedido su timeout
     *         o ha fallado.
     */
    Optional<String> resumir(Oferta oferta, Perfil perfil);

    /** Para exponerlo en /actuator y en la API sin tener que sondear el modelo. */
    default boolean estaDisponible() {
        return true;
    }
}
