package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.Seniority;

/**
 * Responde a una sola pregunta: <b>¿tengo alguna opcion real en esta oferta?</b>
 *
 * <p>Es deliberadamente independiente de las tecnologias. Que una oferta sea
 * alcanzable y que encaje con lo que sabes son cosas distintas, y mezclarlas
 * fue el error del diseno inicial.
 *
 * <p>Vive aparte del {@link Puntuador} porque tiene dos usuarios: el puntuador
 * la usa como multiplicador del encaje, y el {@link ClasificadorDeOfertas} la
 * usa para decidir que ofertas son alcanzables aunque no sean del stack.
 *
 * <p>Tres evidencias independientes, que a veces se contradicen:
 * <ul>
 *   <li>la <b>etiqueta</b> de seniority, que cada empresa usa a su manera;</li>
 *   <li>los <b>anos</b> pedidos, que son el dato duro;</li>
 *   <li>la <b>banda salarial</b>, que delata la seniority que el titulo calla.</li>
 * </ul>
 */
public final class Accesibilidad {

    /** Por encima de este multiplo del salario objetivo, la banda es sospechosa. */
    private static final double BANDA_SOSPECHOSA = 1.6;

    /** Mas anos que esto y la oferta deja de ser alcanzable para un junior. */
    private static final int ANOS_ALCANZABLES = 2;

    /** Factor total, entre 0 y 1. */
    public double de(Seniority senal, Integer anos, Oferta oferta, Perfil perfil) {
        return senal.factor() * porAnos(anos) * porSalario(oferta, perfil);
    }

    /**
     * Los anos son el dato duro. Caen rapido a partir de tres porque a partir de
     * ahi la oferta deja de ser alcanzable, no solo menos comoda.
     *
     * <p>Cuando la oferta no los dice se aplica 0.90: no se penaliza el silencio,
     * pero tampoco se premia. Hoy es el caso mayoritario, porque Adzuna recorta
     * la descripcion a 500 caracteres.
     */
    public double porAnos(Integer anos) {
        if (anos == null) {
            return 0.90;
        }
        if (anos <= 1) {
            return 1.00;
        }
        return switch (anos) {
            case 2 -> 0.92;
            case 3 -> 0.70;
            case 4 -> 0.40;
            default -> 0.20;
        };
    }

    /**
     * El salario como evidencia de seniority.
     *
     * <p>Un sueldo muy por encima del objetivo no es una buena noticia para un
     * junior: es la prueba de que la oferta no es para el. Es un dato de
     * seniority disfrazado de dato de compensacion.
     *
     * <p>El caso que lo motivo: Minsait publico "Full Stack Java-React" con banda
     * 60.000-90.000 y sin la palabra senior en el titulo, y "Senior Full-Stack
     * Engineer" con la banda IDENTICA. Mismo puesto, distinto titular.
     *
     * <p>Proxy parcial: solo 3 de cada 15 ofertas publican banda. No penaliza el
     * silencio, que es lo mayoritario.
     */
    public double porSalario(Oferta oferta, Perfil perfil) {
        double veces = vecesElObjetivo(oferta, perfil);
        if (veces <= BANDA_SOSPECHOSA) {
            return 1.00;
        }
        if (veces <= 2.0) {
            return 0.70;
        }
        if (veces <= 2.5) {
            return 0.40;
        }
        return 0.20;
    }

    /**
     * {@code true} si no hay NINGUNA evidencia de que la oferta no sea junior.
     *
     * <p>No es lo mismo que "el factor es alto": es un predicado explicito, y se
     * lee en voz alta. Ademas evita una trampa numerica: con los anos
     * desconocidos, que es el caso mayoritario, el techo de una oferta que no
     * declara seniority es 0.92 x 0.90 = 0.828, asi que cualquier umbral por
     * encima de eso no lo pasaria nadie.
     */
    public boolean sinEvidenciaDeNoSerJunior(Seniority senal, Integer anos,
                                             Oferta oferta, Perfil perfil) {
        return senal != Seniority.SENIOR
                && (anos == null || anos <= ANOS_ALCANZABLES)
                && vecesElObjetivo(oferta, perfil) <= BANDA_SOSPECHOSA;
    }

    /** Cuantas veces el salario objetivo es el suelo de la banda. 0 si no la publica. */
    private double vecesElObjetivo(Oferta oferta, Perfil perfil) {
        Integer suelo = oferta.salarioMin() != null ? oferta.salarioMin() : oferta.salarioMax();
        int objetivo = perfil.preferencias().salarioObjetivo();
        if (suelo == null || objetivo <= 0) {
            return 0;
        }
        return (double) suelo / objetivo;
    }
}
