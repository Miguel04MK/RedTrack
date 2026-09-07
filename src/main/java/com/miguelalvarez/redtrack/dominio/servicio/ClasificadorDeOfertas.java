package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Analisis;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.Seccion;

import java.util.regex.Pattern;

/**
 * Decide en que seccion del resumen entra cada oferta.
 *
 * <p>Hay dos preguntas distintas y cada seccion responde a una:
 *
 * <pre>
 *   PARA_TI             ¿encaja conmigo?      -> el encaje supera el umbral
 *   PODRIA_INTERESARTE  ¿puedo optar a esto?  -> es junior de desarrollo, sea del stack que sea
 * </pre>
 *
 * <p>La segunda seccion existe porque el mercado junior de un stack concreto se
 * seca semanas enteras: con datos reales de una semana, Adzuna no tenia ni una
 * oferta junior de Java en Galicia. Un resumen que nunca llega es
 * indistinguible de un resumen roto.
 *
 * <p>Deliberadamente NO mira las tecnologias. Ese es justo el punto: una oferta
 * de junior de PHP entra, y se avisa de que el stack no es el tuyo. La maquina
 * informa, la persona decide.
 */
public final class ClasificadorDeOfertas {

    /**
     * El titulo tiene que parecer un puesto de desarrollo.
     *
     * <p>Hace falta porque buscar "junior" a secas en Adzuna devuelve
     * "Junior Territory Manager", "Comercial Tecnico Junior", "Tecnico
     * Automatista Junior" y "Practicas RRHH". Y la categoria de Adzuna no sirve
     * para filtrar: 13 de cada 17 ofertas de informatica reales vienen con
     * category "unknown".
     *
     * <p>Solo terminos positivos, y ninguno tan generico como "tecnico", que es
     * el que arrastra la mayor parte del ruido.
     */
    private static final Pattern PUESTO_DE_DESARROLLO = Pattern.compile(
            "desarrollador|desarrolladora|programador|programadora|developer|"
                    + "\\bdev\\b|engineer|ingenier[oa]\\s+(de\\s+)?software|software|"
                    + "backend|back-end|frontend|front-end|fullstack|full\\s*stack|"
                    + "\\bweb\\b|\\bqa\\b|tester|testing|devops|sysadmin|"
                    + "administrador\\s+de\\s+sistemas|\\bdata\\b|\\bdatos\\b|\\bcloud\\b",
            Pattern.CASE_INSENSITIVE);

    private final Accesibilidad accesibilidad;

    public ClasificadorDeOfertas(Accesibilidad accesibilidad) {
        this.accesibilidad = accesibilidad;
    }

    public Seccion clasificar(Oferta oferta, Analisis analisis, Perfil perfil) {
        if (analisis.superaUmbral(perfil.preferencias().umbralEncaje())) {
            return Seccion.PARA_TI;
        }
        if (esJuniorDeDesarrollo(oferta, analisis, perfil)) {
            return Seccion.PODRIA_INTERESARTE;
        }
        return Seccion.NINGUNA;
    }

    /**
     * Un puesto de desarrollo sin ninguna evidencia de no ser junior.
     *
     * <p>El encaje no entra en la cuenta: si entrara, esta seccion seria otra
     * vez la primera con el liston mas bajo, y lo que se busca es justo lo
     * contrario.
     */
    boolean esJuniorDeDesarrollo(Oferta oferta, Analisis analisis, Perfil perfil) {
        return pareceDesarrollo(oferta)
                && accesibilidad.sinEvidenciaDeNoSerJunior(
                        analisis.senal(), analisis.anosRequeridos(), oferta, perfil);
    }

    /** Expuesto para los tests: es la parte que filtra el ruido de la busqueda. */
    boolean pareceDesarrollo(Oferta oferta) {
        return oferta.titulo() != null && PUESTO_DE_DESARROLLO.matcher(oferta.titulo()).find();
    }
}
