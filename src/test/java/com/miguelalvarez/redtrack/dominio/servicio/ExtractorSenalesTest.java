package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Seniority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractorSenalesTest {

    private final ExtractorSenales extractor = new ExtractorSenales();

    @Nested
    class DeteccionDeModalidad {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "'Puesto 100% remoto',                REMOTO",
                "'Con teletrabajo completo',          REMOTO",
                "'Fully remote position',             REMOTO",
                "'Modelo hibrido, 2 dias en oficina', HIBRIDO",
                "'Trabajo presencial en Vigo',        PRESENCIAL",
                "'On-site in Madrid',                 PRESENCIAL",
                "'Desarrollador Java',                DESCONOCIDA"
        })
        void detectaLaModalidad(String texto, Modalidad esperada) {
            assertThat(extractor.modalidad(texto)).isEqualTo(esperada);
        }

        @Test
        @DisplayName("si dice hibrido Y remoto gana HIBRIDO: es lo que acaba pasando")
        void hibridoGanaARemoto() {
            assertThat(extractor.modalidad("Modelo hibrido con dias de teletrabajo"))
                    .isEqualTo(Modalidad.HIBRIDO);
        }
    }

    @Nested
    class AnosDeExperiencia {

        @Test
        @DisplayName("con un rango se queda con el numero MAS ALTO: el filtro real es ese")
        void seQuedaConElMasAlto() {
            assertThat(extractor.anosRequeridos("Se requieren 2-4 años de experiencia"))
                    .isEqualTo(4);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "'Al menos 3 años de experiencia',       3",
                "'Minimo 2 años en desarrollo',          2",
                "'Mas de 5 años con Java',               5",
                "'+1 año de experiencia',                1",
                "'3+ years of experience',               3",
                "'2-5 years working with Spring',        5"
        })
        void extraeLosAnos(String texto, int esperados) {
            assertThat(extractor.anosRequeridos(texto)).isEqualTo(esperados);
        }

        @Test
        void devuelveNullSiLaOfertaNoLoDice() {
            assertThat(extractor.anosRequeridos("Buscamos desarrollador Java")).isNull();
        }

        @Test
        @DisplayName("con varias menciones se queda con la mayor")
        void variasMencionesSeQuedaConLaMayor() {
            assertThat(extractor.anosRequeridos(
                    "2 años con Spring y al menos 4 años en backend")).isEqualTo(4);
        }
    }

    @Nested
    class SenalDeSeniority {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "'Desarrollador Java Junior',        JUNIOR",
                "'Programador Júnior',               JUNIOR",
                "'Recien titulado en informatica',   JUNIOR",
                "'Sin experiencia previa',           JUNIOR",
                "'Senior Backend Developer',         SENIOR",
                "'Tech Lead de plataforma',          SENIOR",
                "'Arquitecto de software',           SENIOR",
                "'Desarrollador semi-senior',        SENIOR",
                "'Desarrollador Java',               NO_DICE"
        })
        void detectaLaSenal(String texto, Seniority esperada) {
            assertThat(extractor.seniority(texto)).isEqualTo(esperada);
        }

        @Test
        @DisplayName("SENIOR gana a JUNIOR: 'Senior (no junior)' es una oferta senior")
        void seniorGanaAJunior() {
            assertThat(extractor.seniority("Senior Java Developer, no junior"))
                    .isEqualTo(Seniority.SENIOR);
        }
    }
}
