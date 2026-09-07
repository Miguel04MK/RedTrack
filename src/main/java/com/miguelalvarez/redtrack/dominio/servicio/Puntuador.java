package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Analisis;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Nivel;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.Seniority;
import com.miguelalvarez.redtrack.dominio.modelo.Tecnologia;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * El encaje responde a DOS preguntas distintas, y por eso tiene dos partes.
 *
 * <pre>
 *   ¿cuanto me gusta?      ->  suma      65 tecnologias + 20 ubicacion + 15 salario
 *   ¿tengo alguna opcion?  ->  multiplica  factorSeniority x factorAnos
 *
 *   encaje = (tecnologias + ubicacion + salario) x accesibilidad
 * </pre>
 *
 * <p>La accesibilidad MULTIPLICA en vez de sumar, y es una desviacion
 * deliberada del diseno inicial motivada por los datos: con la seniority como
 * bloque de 20 puntos, las ofertas senior reales sacaban 73 sobre 100 y
 * superaban el umbral, porque los otros bloques compensaban el cero. Un bloque
 * que suma siempre se puede compensar.
 *
 * <p>Los anos requeridos tambien viven en el multiplicador, y no en un bloque
 * que suma. La etiqueta de seniority es un proxy de los anos: cuando la oferta
 * dice los anos, mandan los anos. Asi una MID de dos anos con encaje
 * excepcional puede entrar, y una MID de cuatro no entra ni siendo perfecta,
 * sin necesidad de inventar categorias intermedias en el enum.
 *
 * <p>Las banderas rojas NO restan. Se muestran aparte: la maquina informa, la
 * persona decide. Asi una oferta buena no cae del resumen por un "se valora
 * ingles". La excepcion es {@code descartar_si_titulo_contiene}, que si es un
 * filtro duro: el nombre del ajuste promete descartar, y descarta.
 */
public final class Puntuador {

    private static final int MAX_TECNOLOGIAS = 65;
    private static final int MAX_UBICACION = 20;
    private static final int MAX_SALARIO = 15;

    private static final Pattern INGLES_ALTO = Pattern.compile(
            "ingl[ée]s\\s*(alto|avanzado|fluido|c1|c2)|\\bc1\\b|\\bc2\\b|fluent\\s+english|"
                    + "english\\s*:?\\s*(advanced|fluent|native)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VEHICULO = Pattern.compile(
            "veh[ií]culo\\s+propio|carn[ée]\\s+de\\s+conducir|permiso\\s+de\\s+conducir",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VIAJAR = Pattern.compile(
            "disponibilidad\\s+para\\s+viajar|willing(ness)?\\s+to\\s+travel",
            Pattern.CASE_INSENSITIVE);

    private final DetectorTecnologias detector;
    private final ExtractorSenales extractor;
    private final Normalizador normalizador;

    public Puntuador(DetectorTecnologias detector, ExtractorSenales extractor,
                     Normalizador normalizador) {
        this.detector = detector;
        this.extractor = extractor;
        this.normalizador = normalizador;
    }

    /** Analiza la oferta contra el perfil. El resumen de IA se anade despues, si lo hay. */
    public Analisis analizar(Oferta oferta, Perfil perfil) {
        String texto = oferta.textoCompleto();

        Set<String> pedidas = detector.detectar(texto, perfil);
        Set<String> tengo = new LinkedHashSet<>();
        Set<String> faltan = new LinkedHashSet<>();
        for (String nombre : pedidas) {
            boolean laTengo = perfil.buscar(nombre)
                    .map(t -> t.nivel() != Nivel.NINGUNO)
                    .orElse(false);
            (laTengo ? tengo : faltan).add(nombre);
        }

        Seniority senal = extractor.seniority(texto);
        Integer anos = extractor.anosRequeridos(texto);

        int bruto = puntosTecnologias(pedidas, perfil)
                + puntosUbicacion(oferta, perfil)
                + puntosSalario(oferta, perfil);

        int encaje = descartadaPorTitulo(oferta, perfil)
                ? 0
                : (int) Math.round(bruto * accesibilidad(senal, anos));

        return new Analisis(
                acotar(encaje),
                pedidas,
                tengo,
                faltan,
                senal,
                anos,
                banderasRojas(oferta, perfil, faltan),
                null);
    }

    // ------------------------------------------------------------------
    //  Bloques de puntuacion
    // ------------------------------------------------------------------

    /**
     * 55 pts: suma de pesos de las pedidas que tengo / suma de pesos de todas
     * las pedidas.
     *
     * <p>El denominador aplica un peso minimo de 1. Sin eso, una tecnologia
     * declarada con {@code peso: 0} no entra en la cuenta y exigirla sale
     * gratis: la oferta podria pedir Angular, .NET y Kafka sin que la nota
     * bajara un punto. El peso mide lo que importa que la oferta la pida; que
     * se tenga o no ya lo dice el nivel.
     */
    int puntosTecnologias(Set<String> pedidas, Perfil perfil) {
        if (pedidas.isEmpty()) {
            return 0;
        }
        double numerador = 0;
        double denominador = 0;
        for (String nombre : pedidas) {
            Optional<Tecnologia> t = perfil.buscar(nombre);
            if (t.isEmpty()) {
                continue;
            }
            numerador += t.get().pesoEfectivo();
            denominador += Math.max(1, t.get().peso());
        }
        if (denominador == 0) {
            return 0;
        }
        return (int) Math.round(MAX_TECNOLOGIAS * (numerador / denominador));
    }

    /**
     * Cuanta opcion real hay de entrar en esta oferta, entre 0 y 1.
     *
     * <p>Producto de dos factores independientes: la etiqueta de seniority y
     * los anos pedidos. Independientes porque dicen cosas distintas y a veces
     * se contradicen: "mid-level" con 2 anos es una puerta entornada,
     * "mid-level" con 5 es una puerta cerrada, y la etiqueta es la misma.
     */
    double accesibilidad(Seniority senal, Integer anos) {
        return senal.factor() * factorAnos(anos);
    }

    /**
     * Los anos son el dato duro. Caen rapido a partir de tres porque a partir
     * de ahi la oferta deja de ser alcanzable, no solo menos comoda.
     *
     * <p>Cuando la oferta no los dice se aplica 0.90: no se penaliza el
     * silencio, pero tampoco se premia. Hoy es el caso mayoritario, porque
     * Adzuna recorta la descripcion a 500 caracteres.
     */
    double factorAnos(Integer anos) {
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

    /** 15 pts. */
    int puntosUbicacion(Oferta oferta, Perfil perfil) {
        if (oferta.modalidad() == Modalidad.REMOTO) {
            return MAX_UBICACION;
        }
        String ubicacion = normalizador.normalizar(oferta.ubicacion());
        if (ubicacion.isBlank()) {
            return 6;
        }
        for (String deseada : perfil.preferencias().ubicacionesDeseadas()) {
            if (ubicacion.contains(normalizador.normalizar(deseada))) {
                return MAX_UBICACION;
            }
        }
        // TODO(fase-2): distinguir "resto de Espana" (6) de "extranjero" (0)
        //               con el catalogo de provincias del Normalizador.
        return 6;
    }

    /** 10 pts. Publicar banda por debajo del objetivo puntua MENOS que no publicarla. */
    int puntosSalario(Oferta oferta, Perfil perfil) {
        if (!oferta.tieneSalario()) {
            return 6;
        }
        Integer suelo = oferta.salarioMin() != null ? oferta.salarioMin() : oferta.salarioMax();
        return suelo >= perfil.preferencias().salarioObjetivo() ? MAX_SALARIO : 4;
    }

    /**
     * Filtro duro: si el titulo contiene una de las palabras vetadas en el
     * perfil, la oferta va a cero y no hay encaje que la salve.
     *
     * <p>Se mira solo el TITULO, no la descripcion: "reportaras al arquitecto"
     * no convierte una oferta junior en una oferta de arquitecto.
     */
    boolean descartadaPorTitulo(Oferta oferta, Perfil perfil) {
        String titulo = normalizador.normalizar(oferta.titulo());
        return perfil.preferencias().descartarSiTituloContiene().stream()
                .anyMatch(veto -> titulo.contains(normalizador.normalizar(veto)));
    }

    // ------------------------------------------------------------------
    //  Banderas rojas: informan, no restan
    // ------------------------------------------------------------------

    List<String> banderasRojas(Oferta oferta, Perfil perfil, Set<String> faltan) {
        List<String> banderas = new ArrayList<>();
        String texto = oferta.textoCompleto();

        if (INGLES_ALTO.matcher(texto).find() && nivelDeInglesInsuficiente(perfil)) {
            banderas.add("pide ingles alto (tu nivel: "
                    + perfil.preferencias().idiomaMaximo() + ")");
        }
        if (VEHICULO.matcher(texto).find()) {
            banderas.add("pide vehiculo propio");
        }
        if (VIAJAR.matcher(texto).find()) {
            banderas.add("pide disponibilidad para viajar");
        }

        Integer anos = extractor.anosRequeridos(texto);
        if (anos != null && anos > perfil.preferencias().anosMaximosAceptables()) {
            banderas.add("pide " + anos + " anos");
        }

        String titulo = normalizador.normalizar(oferta.titulo());
        for (String descarte : perfil.preferencias().descartarSiTituloContiene()) {
            if (titulo.contains(normalizador.normalizar(descarte))) {
                banderas.add("titulo contiene '" + descarte + "'");
            }
        }
        for (String tecnologia : faltan) {
            banderas.add("tecnologia troncal que no tienes: " + tecnologia);
        }
        return banderas;
    }

    private boolean nivelDeInglesInsuficiente(Perfil perfil) {
        String nivel = perfil.preferencias().idiomaMaximo();
        if (nivel == null) {
            return true;
        }
        String n = nivel.toUpperCase(Locale.ROOT);
        return !(n.startsWith("C") || n.equals("B2"));
    }

    private int acotar(int valor) {
        return Math.max(0, Math.min(100, valor));
    }
}
