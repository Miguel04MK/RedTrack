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
 * Reparte 100 puntos de encaje entre cinco bloques:
 *
 * <pre>
 *   50  tecnologias
 *   20  seniority
 *   15  anos requeridos
 *   10  ubicacion
 *    5  salario
 * </pre>
 *
 * <p>Las banderas rojas NO restan. Se muestran aparte: la maquina informa,
 * la persona decide. Ademas de ser mejor producto, evita que una oferta buena
 * caiga del resumen por un "se valora ingles".
 */
public final class Puntuador {

    private static final int MAX_TECNOLOGIAS = 50;
    private static final int MAX_SENIORITY = 20;
    private static final int MAX_ANOS = 15;
    private static final int MAX_UBICACION = 10;
    private static final int MAX_SALARIO = 5;

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

        int encaje = puntosTecnologias(pedidas, perfil)
                + puntosSeniority(senal)
                + puntosAnos(anos)
                + puntosUbicacion(oferta, perfil)
                + puntosSalario(oferta, perfil);

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

    /** 50 pts: suma de pesos de las pedidas que tengo / suma de pesos de todas las pedidas. */
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
            denominador += t.get().peso();
        }
        if (denominador == 0) {
            return 0;
        }
        return (int) Math.round(MAX_TECNOLOGIAS * (numerador / denominador));
    }

    /** 20 pts. Una oferta que no se pronuncia vale mas que una explicitamente MID. */
    int puntosSeniority(Seniority senal) {
        return switch (senal) {
            case JUNIOR -> 20;
            case NO_DICE -> 12;
            case MID -> 6;
            case SENIOR -> 0;
        };
    }

    /** 15 pts. */
    int puntosAnos(Integer anos) {
        if (anos == null) {
            return 10;
        }
        if (anos <= 1) {
            return 15;
        }
        return switch (anos) {
            case 2 -> 10;
            case 3 -> 5;
            default -> 0;
        };
    }

    /** 10 pts. */
    int puntosUbicacion(Oferta oferta, Perfil perfil) {
        if (oferta.modalidad() == Modalidad.REMOTO) {
            return 10;
        }
        String ubicacion = normalizador.normalizar(oferta.ubicacion());
        if (ubicacion.isBlank()) {
            return 4;
        }
        for (String deseada : perfil.preferencias().ubicacionesDeseadas()) {
            if (ubicacion.contains(normalizador.normalizar(deseada))) {
                return 10;
            }
        }
        // TODO(fase-2): distinguir "resto de Espana" (4) de "extranjero" (0)
        //               con el catalogo de provincias del Normalizador.
        return 4;
    }

    /** 5 pts. Publicar banda por debajo del objetivo puntua MENOS que no publicarla. */
    int puntosSalario(Oferta oferta, Perfil perfil) {
        if (!oferta.tieneSalario()) {
            return 3;
        }
        Integer suelo = oferta.salarioMin() != null ? oferta.salarioMin() : oferta.salarioMax();
        return suelo >= perfil.preferencias().salarioObjetivo() ? 5 : 2;
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
