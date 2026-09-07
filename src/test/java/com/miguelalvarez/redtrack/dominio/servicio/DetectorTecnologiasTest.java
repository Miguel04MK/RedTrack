package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Nivel;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.Preferencias;
import com.miguelalvarez.redtrack.dominio.modelo.Tecnologia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los tests de las trampas. NO los borres: son la razon de que el detector no
 * tenga el bug que si tiene InfoJobs.
 *
 * <p>Sin Spring: el dominio es Java puro y estos tests corren en milisegundos.
 */
class DetectorTecnologiasTest {

    private final DetectorTecnologias detector = new DetectorTecnologias();

    @Nested
    @DisplayName("no confunde una tecnologia con otra que la contiene")
    class Trampas {

        @Test
        @DisplayName("'Java' NO casa dentro de 'JavaScript' -- el clasico")
        void javaNoCasaEnJavaScript() {
            assertThat(detector.aparece("Buscamos perfil con JavaScript y React", "java"))
                    .isFalse();
        }

        @Test
        @DisplayName("'Java' SI casa cuando esta de verdad")
        void javaCasaCuandoEsta() {
            assertThat(detector.aparece("Desarrollador Java con Spring Boot", "java"))
                    .isTrue();
        }

        @Test
        @DisplayName("'Java' casa aunque en la misma frase haya JavaScript")
        void javaCasaAunqueHayaJavaScript() {
            assertThat(detector.aparece("Java 21 en backend y JavaScript en front", "java"))
                    .isTrue();
        }

        @ParameterizedTest(name = "\"C\" no casa en \"{0}\"")
        @CsvSource({
                "Se requiere C++ avanzado",
                "Programacion en C#",
                "Stack C++/Qt"
        })
        @DisplayName("'C' NO casa dentro de 'C++' ni 'C#' -- el bug real de InfoJobs")
        void ceNoCasaEnCeMasMasNiCeAlmohadilla(String texto) {
            assertThat(detector.aparece(texto, "c")).isFalse();
        }

        @Test
        @DisplayName("'C' SI casa cuando la oferta pide C de verdad")
        void ceCasaCuandoEsta() {
            assertThat(detector.aparece("Desarrollo embebido en C sobre microcontrolador", "c"))
                    .isTrue();
        }

        @ParameterizedTest(name = "\"Go\" no casa en \"{0}\"")
        @CsvSource({
                "Experiencia con Google Cloud",
                "Backend en Django",
                "Trabajamos con MongoDB"
        })
        @DisplayName("'Go' NO casa dentro de otras palabras")
        void goNoCasaDentroDeOtrasPalabras(String texto) {
            assertThat(detector.aparece(texto, "go")).isFalse();
        }

        @Test
        void goCasaCuandoEsta() {
            assertThat(detector.aparece("Microservicios en Go y Kubernetes", "go")).isTrue();
        }

        @Test
        @DisplayName("los terminos con simbolo se detectan enteros")
        void terminosConSimbolo() {
            assertThat(detector.aparece("Se requiere C++ avanzado", "c++")).isTrue();
            assertThat(detector.aparece("Backend en .NET 8", ".net")).isTrue();
            assertThat(detector.aparece("Programacion en C#", "c#")).isTrue();
        }
    }

    @Nested
    @DisplayName("deteccion contra el perfil")
    class ContraElPerfil {

        @Test
        void devuelveElNombreCanonicoAunqueAparezcaComoAlias() {
            Set<String> encontradas = detector.detectar(
                    "Buscamos springboot y postgres", perfilDePrueba());

            assertThat(encontradas).containsExactlyInAnyOrder("Spring Boot", "PostgreSQL");
        }

        @Test
        void noSeRepiteUnaTecnologiaQueAparezcaVariasVeces() {
            Set<String> encontradas = detector.detectar(
                    "Java, java 21 y JDK", perfilDePrueba());

            assertThat(encontradas).containsExactly("Java");
        }

        @Test
        void textoVacioNoDetectaNada() {
            assertThat(detector.detectar("", perfilDePrueba())).isEmpty();
            assertThat(detector.detectar(null, perfilDePrueba())).isEmpty();
        }
    }

    private Perfil perfilDePrueba() {
        return new Perfil(
                List.of(
                        new Tecnologia("Java", Nivel.ALTO, 3,
                                List.of("java 21", "jdk")),
                        new Tecnologia("Spring Boot", Nivel.ALTO, 3,
                                List.of("springboot", "spring-boot")),
                        new Tecnologia("PostgreSQL", Nivel.MEDIO, 2,
                                List.of("postgres")),
                        new Tecnologia("Angular", Nivel.NINGUNO, 0, List.of())),
                new Preferencias(List.of("vigo"), 24000, "B1", List.of(), 3, 55));
    }
}
