package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Analisis;
import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Nivel;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.Preferencias;
import com.miguelalvarez.redtrack.dominio.modelo.Seniority;
import com.miguelalvarez.redtrack.dominio.modelo.Tecnologia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PuntuadorTest {

    private static final int UMBRAL = 55;

    private final Normalizador normalizador = new Normalizador();
    private final ExtractorSenales extractor = new ExtractorSenales();
    private final Puntuador puntuador =
            new Puntuador(new DetectorTecnologias(), extractor, normalizador);

    @Nested
    @DisplayName("la seniority multiplica, no suma")
    class LaSeniorityMultiplica {

        @Test
        @DisplayName("la oferta perfecta para un junior saca 100")
        void ofertaPerfecta() {
            Analisis a = puntuador.analizar(oferta(
                    "Desarrollador Java Junior",
                    "Trabajaras con Java y Spring Boot. Se pide 1 ano de experiencia."),
                    perfil());

            assertThat(a.senal()).isEqualTo(Seniority.JUNIOR);
            assertThat(a.encaje()).isEqualTo(100);
        }

        @Test
        @DisplayName("la misma oferta sin decir seniority se queda en 85")
        void mismaOfertaSinDecirNada() {
            Analisis a = puntuador.analizar(oferta(
                    "Desarrollador Java",
                    "Trabajaras con Java y Spring Boot. Se pide 1 ano de experiencia."),
                    perfil());

            assertThat(a.senal()).isEqualTo(Seniority.NO_DICE);
            assertThat(a.encaje()).isEqualTo(85);
        }

        @Test
        @DisplayName("REGRESION: la misma oferta en senior cae del umbral")
        void mismaOfertaEnSeniorCaeDelUmbral() {
            // Todo identico salvo la senal, que aqui viene de la descripcion
            // para aislar el multiplicador del filtro por titulo.
            Analisis a = puntuador.analizar(oferta(
                    "Desarrollador Java",
                    "Buscamos un perfil senior. Trabajaras con Java y Spring Boot. "
                            + "Se pide 1 ano de experiencia."),
                    perfil());

            assertThat(a.senal()).isEqualTo(Seniority.SENIOR);
            assertThat(a.encaje()).isEqualTo(15);
            assertThat(a.superaUmbral(UMBRAL)).isFalse();
        }

        @Test
        @DisplayName("REGRESION: el caso real de Adzuna que sacaba 73")
        void elCasoRealDeAdzuna() {
            // Una oferta senior que solo menciona Java saturaba el bloque de
            // tecnologias y superaba el umbral con la seniority como bloque.
            Analisis a = puntuador.analizar(new Oferta(
                    "adzuna", "4998", "Senior Java Software Engineer",
                    "Trileuco Solutions", "Arbo, Pontevedra", Modalidad.DESCONOCIDA,
                    null, null, "Buscamos Senior Java Software Engineer con el fin ...",
                    "https://ejemplo", Idioma.ES, Instant.now(), Instant.now(), null),
                    perfil());

            assertThat(a.superaUmbral(UMBRAL)).isFalse();
        }
    }

    @Nested
    @DisplayName("descartar_si_titulo_contiene descarta de verdad")
    class FiltroDuroPorTitulo {

        @Test
        void tituloVetadoVaACero() {
            Analisis a = puntuador.analizar(oferta(
                    "Senior Java Developer",
                    "Java y Spring Boot, 1 ano de experiencia."),
                    perfil());

            assertThat(a.encaje()).isZero();
        }

        @Test
        @DisplayName("y la bandera roja explica por que")
        void laBanderaLoExplica() {
            Analisis a = puntuador.analizar(oferta(
                    "Engineering Manager",
                    "Java y Spring Boot."),
                    perfil());

            assertThat(a.encaje()).isZero();
            assertThat(a.banderasRojas()).anyMatch(b -> b.contains("manager"));
        }

        @Test
        @DisplayName("solo mira el titulo: 'reportaras al arquitecto' no descarta")
        void soloMiraElTitulo() {
            Analisis a = puntuador.analizar(oferta(
                    "Desarrollador Java Junior",
                    "Reportaras al arquitecto del equipo. Java y Spring Boot, 1 ano."),
                    perfil());

            assertThat(a.encaje()).isPositive();
        }
    }

    @Nested
    class Bloques {

        @Test
        @DisplayName("REGRESION: exigir una tecnologia que no se tiene NO puede salir gratis")
        void tecnologiaQueNoSeTiene() {
            // Angular esta en el perfil de prueba con peso 0 a proposito: es la
            // configuracion que hacia que exigirla no bajase la nota, porque el
            // denominador del bloque de tecnologias la ignoraba.
            Analisis conAngular = puntuador.analizar(oferta(
                    "Desarrollador Junior",
                    "Java, Spring Boot y Angular. 1 ano de experiencia."),
                    perfil());
            Analisis sinAngular = puntuador.analizar(oferta(
                    "Desarrollador Junior",
                    "Java y Spring Boot. 1 ano de experiencia."),
                    perfil());

            assertThat(conAngular.meFaltan()).contains("Angular");
            assertThat(conAngular.encaje()).isLessThan(sinAngular.encaje());
        }

        @Test
        @DisplayName("pedir muchos anos anula ese bloque")
        void muchosAnosAnulanElBloque() {
            Analisis a = puntuador.analizar(oferta(
                    "Desarrollador Java Junior",
                    "Java y Spring Boot. Se requieren al menos 6 anos."),
                    perfil());

            assertThat(a.anosRequeridos()).isEqualTo(6);
            assertThat(a.encaje()).isEqualTo(80);
            assertThat(a.banderasRojas()).anyMatch(b -> b.contains("6 anos"));
        }

        @Test
        void remotoPuntuaComoUbicacionDeseada() {
            assertThat(puntuador.puntosUbicacion(
                    new Oferta("x", "1", "t", "e", "Berlin", Modalidad.REMOTO,
                            null, null, "", "u", Idioma.ES,
                            Instant.now(), Instant.now(), null), perfil()))
                    .isEqualTo(15);
        }

        @Test
        @DisplayName("no publicar salario puntua mas que publicar una banda mala")
        void bandaMalaPuntuaMenosQueNoPublicar() {
            Oferta sinBanda = new Oferta("x", "1", "t", "e", "vigo", Modalidad.HIBRIDO,
                    null, null, "", "u", Idioma.ES, Instant.now(), Instant.now(), null);
            Oferta bandaBaja = new Oferta("x", "2", "t", "e", "vigo", Modalidad.HIBRIDO,
                    14000, 16000, "", "u", Idioma.ES, Instant.now(), Instant.now(), null);

            assertThat(puntuador.puntosSalario(bandaBaja, perfil()))
                    .isLessThan(puntuador.puntosSalario(sinBanda, perfil()));
        }
    }

    // ------------------------------------------------------------------

    private Oferta oferta(String titulo, String descripcion) {
        return new Oferta("adzuna", "1", titulo, "Coremain", "Vigo",
                Modalidad.HIBRIDO, 25000, 30000, descripcion, "https://ejemplo",
                Idioma.ES, Instant.now(), Instant.now(), null);
    }

    private Perfil perfil() {
        return new Perfil(
                List.of(
                        new Tecnologia("Java", Nivel.ALTO, 3, List.of("jdk")),
                        new Tecnologia("Spring Boot", Nivel.ALTO, 3, List.of("springboot")),
                        new Tecnologia("Angular", Nivel.NINGUNO, 0, List.of())),
                new Preferencias(
                        List.of("vigo", "remoto"), 24000, "B1",
                        List.of("senior", "lead", "arquitecto", "manager"), 3, UMBRAL));
    }
}
