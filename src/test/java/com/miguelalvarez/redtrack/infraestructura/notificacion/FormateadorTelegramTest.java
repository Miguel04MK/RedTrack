package com.miguelalvarez.redtrack.infraestructura.notificacion;

import com.miguelalvarez.redtrack.dominio.modelo.Analisis;
import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.modelo.ResumenDiario;
import com.miguelalvarez.redtrack.dominio.modelo.Seniority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FormateadorTelegramTest {

    private final FormateadorTelegram formateador = new FormateadorTelegram();
    private static final LocalDate HOY = LocalDate.of(2026, 9, 7);

    @Nested
    @DisplayName("el dia en blanco tambien manda mensaje")
    class DiaEnBlanco {

        @Test
        @DisplayName("si no entro nada, lo dice y ensena lo mas alto que hubo")
        void loDiceYEnsenaLoMasAlto() {
            ResumenDiario resumen = new ResumenDiario(HOY, List.of(), List.of(),
                    List.of(oferta("Senior Java Engineer", "Trileuco", 40, "pide 6 anos"),
                            oferta("Full Stack Java-React", "Minsait", 31, null),
                            oferta("Odontologo", "Dentego", 12, null),
                            oferta("Otra mas", "Empresa", 5, null)));

            String texto = formateador.formatear(resumen);

            assertThat(texto)
                    .contains("hoy nada supera el umbral")
                    .contains("Revisadas 4 ofertas nuevas")
                    .contains("Lo mas alto que hubo")
                    .contains("[40%] Senior Java Engineer - Trileuco")
                    .contains("pide 6 anos");
            // Solo las tres mejores: el mensaje informa, no vuelca la tabla.
            assertThat(texto).doesNotContain("Otra mas");
        }

        @Test
        @DisplayName("una sola oferta revisada se dice en singular")
        void singular() {
            ResumenDiario resumen = new ResumenDiario(HOY, List.of(), List.of(),
                    List.of(oferta("Algo", "Empresa", 10, null)));

            assertThat(formateador.formatear(resumen)).contains("Revisadas 1 oferta nueva");
        }
    }

    @Nested
    @DisplayName("resumen con ofertas")
    class ConOfertas {

        @Test
        @DisplayName("cuenta cada seccion en la cabecera y solo pinta las que tienen algo")
        void cabeceraYSecciones() {
            ResumenDiario resumen = new ResumenDiario(HOY,
                    List.of(oferta("Desarrollador Java Junior", "Coremain", 87, null)),
                    List.of(), List.of());

            String texto = formateador.formatear(resumen);

            assertThat(texto)
                    .contains("1 para ti")
                    .contains("*PARA TI*")
                    .doesNotContain("PODRIAN INTERESARTE")
                    .doesNotContain("podrian interesarte");
        }

        @Test
        @DisplayName("avisa del stack que no tienes en vez de esconder la oferta")
        void avisaDelStackQueNoTienes() {
            OfertaAnalizada php = new OfertaAnalizada(
                    new Oferta("adzuna", "1", "Desarrollador PHP Junior", "Otra", "Vigo",
                            Modalidad.PRESENCIAL, null, null, "", "https://x", Idioma.ES,
                            Instant.now(), Instant.now(), "h"),
                    new Analisis(21, Set.of("PHP"), Set.of(), Set.of("PHP"),
                            Seniority.JUNIOR, 1, List.of(), null),
                    List.of("adzuna"));

            ResumenDiario resumen = new ResumenDiario(HOY, List.of(), List.of(php), List.of());

            assertThat(formateador.formatear(resumen))
                    .contains("PODRIAN INTERESARTE")
                    .contains("Junior de desarrollo, aunque no sea tu stack")
                    .contains("Stack que no tienes: PHP");
        }

        @Test
        @DisplayName("no se pasa del limite de Telegram")
        void respetaElLimiteDeTelegram() {
            List<OfertaAnalizada> muchas = java.util.stream.IntStream.range(0, 200)
                    .mapToObj(i -> oferta("Desarrollador Java Junior numero " + i,
                            "Empresa " + i, 80, null))
                    .toList();

            String texto = formateador.formatear(
                    new ResumenDiario(HOY, muchas, List.of(), List.of()));

            assertThat(texto.length()).isLessThanOrEqualTo(FormateadorTelegram.MAX_CARACTERES);
            assertThat(texto).contains("y mas");
        }
    }

    private OfertaAnalizada oferta(String titulo, String empresa, int encaje, String bandera) {
        return new OfertaAnalizada(
                new Oferta("adzuna", titulo, titulo, empresa, "Vigo",
                        Modalidad.HIBRIDO, null, null, "", "https://ejemplo", Idioma.ES,
                        Instant.now(), Instant.now(), titulo),
                new Analisis(encaje, Set.of(), Set.of(), Set.of(),
                        Seniority.NO_DICE, null,
                        bandera == null ? List.of() : List.of(bandera), null),
                List.of("adzuna"));
    }
}
