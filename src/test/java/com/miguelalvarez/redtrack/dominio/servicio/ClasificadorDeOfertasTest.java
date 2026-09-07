package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Analisis;
import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.Preferencias;
import com.miguelalvarez.redtrack.dominio.modelo.Seccion;
import com.miguelalvarez.redtrack.dominio.modelo.Seniority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClasificadorDeOfertasTest {

    private static final int UMBRAL = 55;

    private final ClasificadorDeOfertas clasificador =
            new ClasificadorDeOfertas(new Accesibilidad());

    @Nested
    @DisplayName("filtra el ruido de buscar 'junior' a secas")
    class FiltroDeRuido {

        // Titulos reales devueltos por Adzuna al buscar "junior" en Galicia.
        @ParameterizedTest(name = "\"{0}\" NO es un puesto de desarrollo")
        @ValueSource(strings = {
                "Junior Territory Manager Spain",
                "Comercial Tecnico Junior Con Ingles",
                "Tecnico/A Automatista Junior - H/M",
                "PRACTICAS RRHH (BECAS Y TALENTO) - (Ourense)",
                "Auxiliar Administrativo Junior"
        })
        void elRuidoNoPasa(String titulo) {
            assertThat(clasificador.pareceDesarrollo(oferta(titulo, null, null))).isFalse();
        }

        @ParameterizedTest(name = "\"{0}\" SI es un puesto de desarrollo")
        @ValueSource(strings = {
                "Desarrollador PHP Junior",
                "Programadora Junior",
                "Software Engineer",
                "Fullstack Developer - 25.000 Al Ano - Remoto",
                "Junior Data Analyst",
                "Backend Developer",
                "Junior QA Tester",
                "Becario de desarrollo web"
        })
        void losDeDesarrolloPasan(String titulo) {
            assertThat(clasificador.pareceDesarrollo(oferta(titulo, null, null))).isTrue();
        }

        @Test
        @DisplayName("'tecnico' a secas no cuenta: es lo que arrastra el ruido")
        void tecnicoASecasNoCuenta() {
            assertThat(clasificador.pareceDesarrollo(
                    oferta("Tecnico de mantenimiento", null, null))).isFalse();
        }
    }

    @Nested
    @DisplayName("reparte en secciones")
    class Reparto {

        @Test
        @DisplayName("si supera el umbral y es de desarrollo, va a PARA_TI")
        void superarElUmbralMandaAParaTi() {
            assertThat(clasificador.clasificar(
                    oferta("Desarrollador Java Junior", null, null),
                    analisis(87, Seniority.JUNIOR, 1),
                    perfil()))
                    .isEqualTo(Seccion.PARA_TI);
        }

        @Test
        @DisplayName("REGRESION: superar el umbral NO basta si no es un puesto de desarrollo")
        void elUmbralNoAbrePorSiSoloLaPrimeraSeccion() {
            // Caso real de Remotive: "SaaS Product Support Jedi" saco 61 y entro
            // en "para ti" porque menciona JavaScript, es remota y pide 2 anos.
            // Es soporte a cliente. Citar una tecnologia que tienes no convierte
            // una oferta en una oferta de programador.
            assertThat(clasificador.clasificar(
                    oferta("SaaS Product Support Jedi", null, null),
                    analisis(61, Seniority.NO_DICE, 2),
                    perfil()))
                    .isEqualTo(Seccion.NINGUNA);
        }

        @Test
        @DisplayName("y tampoco entra por la segunda puerta")
        void tampocoEntraPorLaSegunda() {
            assertThat(clasificador.clasificar(
                    oferta("Remote Office Assistant", null, null),
                    analisis(32, Seniority.JUNIOR, 1),
                    perfil()))
                    .isEqualTo(Seccion.NINGUNA);
        }

        @Test
        @DisplayName("un junior de otro stack con encaje bajo va a PODRIA_INTERESARTE")
        void juniorDeOtroStackVaALaSegunda() {
            assertThat(clasificador.clasificar(
                    oferta("Desarrollador PHP Junior", null, null),
                    analisis(21, Seniority.JUNIOR, 1),
                    perfil()))
                    .isEqualTo(Seccion.PODRIA_INTERESARTE);
        }

        @Test
        @DisplayName("el encaje NO cuenta para la segunda seccion: si contase, seria la primera con el liston bajo")
        void elEncajeNoAbreLaSegundaSeccion() {
            // Encaje 3, practicamente nulo, pero es un junior de desarrollo.
            assertThat(clasificador.clasificar(
                    oferta("Programador Junior", null, null),
                    analisis(3, Seniority.NO_DICE, null),
                    perfil()))
                    .isEqualTo(Seccion.PODRIA_INTERESARTE);
        }

        @Test
        @DisplayName("una senior no entra en ninguna, aunque el titulo sea de desarrollo")
        void laSeniorNoEntraEnNinguna() {
            assertThat(clasificador.clasificar(
                    oferta("Senior Java Software Engineer", null, null),
                    analisis(0, Seniority.SENIOR, null),
                    perfil()))
                    .isEqualTo(Seccion.NINGUNA);
        }

        @Test
        @DisplayName("pedir mas de 2 anos la deja fuera de la segunda seccion")
        void masDeDosAnosLaDejaFuera() {
            assertThat(clasificador.clasificar(
                    oferta("Desarrollador Python", null, null),
                    analisis(30, Seniority.NO_DICE, 4),
                    perfil()))
                    .isEqualTo(Seccion.NINGUNA);
        }

        @Test
        @DisplayName("REGRESION: la banda de 60.000 tambien cierra la segunda seccion")
        void laBandaDeSeniorTambienCierraLaSegunda() {
            // Sin esto, la oferta de Minsait que expulsamos de "para ti" por su
            // banda salarial se colaria por la puerta de atras.
            assertThat(clasificador.clasificar(
                    oferta("Full Stack Java-React", 60000, 90000),
                    analisis(31, Seniority.NO_DICE, null),
                    perfil()))
                    .isEqualTo(Seccion.NINGUNA);
        }

        @Test
        @DisplayName("una banda razonable no cierra nada")
        void bandaRazonableNoCierraNada() {
            assertThat(clasificador.clasificar(
                    oferta("Fullstack Developer - Remoto", 25000, 25000),
                    analisis(22, Seniority.NO_DICE, null),
                    perfil()))
                    .isEqualTo(Seccion.PODRIA_INTERESARTE);
        }
    }

    // ------------------------------------------------------------------

    private Oferta oferta(String titulo, Integer salarioMin, Integer salarioMax) {
        return new Oferta("adzuna", "1", titulo, "Empresa", "Vigo",
                Modalidad.DESCONOCIDA, salarioMin, salarioMax, "", "https://ejemplo",
                Idioma.ES, Instant.now(), Instant.now(), "huella");
    }

    private Analisis analisis(int encaje, Seniority senal, Integer anos) {
        return new Analisis(encaje, Set.of(), Set.of(), Set.of(), senal, anos, List.of(), null);
    }

    private Perfil perfil() {
        return new Perfil(List.of(),
                new Preferencias(List.of("vigo"), 24000, "B1", List.of(), 3, UMBRAL));
    }
}
