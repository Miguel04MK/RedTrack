package com.miguelalvarez.redtrack.configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa la planificacion, salvo en el modo "una pasada".
 *
 * <p>Ahi no solo sobra: estorba. El planificador de Spring levanta hilos que no
 * son demonio, y con ellos vivos el proceso no termina nunca — que es justo lo
 * contrario de lo que se busca en una ejecucion puntual.
 */
@Configuration
@EnableScheduling
@Profile("!una-pasada")
public class PlanificacionConfiguracion {
}
