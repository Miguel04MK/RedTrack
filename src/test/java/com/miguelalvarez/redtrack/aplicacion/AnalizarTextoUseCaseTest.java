package com.miguelalvarez.redtrack.aplicacion;

import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Nivel;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.Preferencias;
import com.miguelalvarez.redtrack.dominio.modelo.Seniority;
import com.miguelalvarez.redtrack.dominio.modelo.Tecnologia;
import com.miguelalvarez.redtrack.dominio.servicio.Accesibilidad;
import com.miguelalvarez.redtrack.dominio.servicio.DetectorTecnologias;
import com.miguelalvarez.redtrack.dominio.servicio.ExtractorSenales;
import com.miguelalvarez.redtrack.dominio.servicio.Normalizador;
import com.miguelalvarez.redtrack.dominio.servicio.Puntuador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El modo "pegame esta oferta". Es la respuesta practica a que Adzuna recorte
 * las descripciones: aqui llega el texto entero porque lo trae una persona.
 */
class AnalizarTextoUseCaseTest {

    private final Normalizador normalizador = new Normalizador();
    private final ExtractorSenales extractor = new ExtractorSenales();
    private final AnalizarTextoUseCase useCase = new AnalizarTextoUseCase(
            new Puntuador(new DetectorTecnologias(), extractor, normalizador,
                    new Accesibilidad()),
            extractor,
            normalizador);

    private static final String OFERTA_HTML = """
            <p>Buscamos un <b>Desarrollador Backend Junior</b> en Vigo.</p>
            <ul><li>Java 21 y Spring Boot</li><li>PostgreSQL y Docker</li>
            <li>Se requiere al menos 1 ano de experiencia</li>
            <li>Se valora ingles avanzado</li></ul><p>Modelo hibrido.</p>
            """;

    @Test
    @DisplayName("analiza el texto completo, HTML incluido")
    void analizaElTextoCompleto() {
        var r = useCase.ejecutar("Desarrollador Backend Junior", OFERTA_HTML, perfil());

        assertThat(r.analisis().senal()).isEqualTo(Seniority.JUNIOR);
        assertThat(r.analisis().anosRequeridos()).isEqualTo(1);
        assertThat(r.modalidad()).isEqualTo(Modalidad.HIBRIDO);
        assertThat(r.analisis().tecnologiasPedidas())
                .containsExactlyInAnyOrder("Java", "Spring Boot", "PostgreSQL", "Docker");
        assertThat(r.analisis().encaje()).isPositive();
    }

    @Test
    @DisplayName("las tecnologias se detectan dentro del HTML, no de las etiquetas")
    void noSeConfundeConElMarcado() {
        var r = useCase.ejecutar("Desarrollador",
                "<div class=\"go-container\" style=\"color: red\">Buscamos perfil con Python</div>",
                perfil());

        // "go" aparece en el nombre de clase, y no es una tecnologia pedida.
        assertThat(r.analisis().tecnologiasPedidas()).containsExactly("Python");
    }

    @Test
    @DisplayName("avisa de que el encaje no es comparable, porque falta contexto")
    void avisaDeQueFaltaContexto() {
        var r = useCase.ejecutar("Desarrollador Java", OFERTA_HTML, perfil());

        assertThat(r.aviso()).contains("no es comparable");
    }

    @Test
    @DisplayName("el titulo cuenta para la seniority")
    void elTituloCuenta() {
        var conTitulo = useCase.ejecutar("Senior Java Developer",
                "Trabajaras con Java y Spring Boot.", perfil());

        assertThat(conTitulo.analisis().senal()).isEqualTo(Seniority.SENIOR);
        // "senior" esta en descartar_si_titulo_contiene: filtro duro.
        assertThat(conTitulo.analisis().encaje()).isZero();
    }

    @Test
    @DisplayName("sin titulo tambien funciona, y lo advierte")
    void sinTituloTambienFunciona() {
        var r = useCase.ejecutar(null, OFERTA_HTML, perfil());

        assertThat(r.analisis().tecnologiasPedidas()).contains("Java");
        assertThat(r.aviso()).contains("sin titulo");
    }

    @Test
    @DisplayName("sin texto no hay nada que analizar")
    void sinTextoFalla() {
        assertThatThrownBy(() -> useCase.ejecutar("Titulo", "   ", perfil()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("texto");
    }

    private Perfil perfil() {
        return new Perfil(
                List.of(
                        new Tecnologia("Java", Nivel.ALTO, 3, List.of("java 21")),
                        new Tecnologia("Spring Boot", Nivel.ALTO, 3, List.of("springboot")),
                        new Tecnologia("PostgreSQL", Nivel.MEDIO, 2, List.of("postgres")),
                        new Tecnologia("Docker", Nivel.MEDIO, 2, List.of()),
                        new Tecnologia("Python", Nivel.BAJO, 1, List.of()),
                        new Tecnologia("Go", Nivel.NINGUNO, 2, List.of())),
                new Preferencias(List.of("vigo"), 24000, "B1",
                        List.of("senior", "lead", "arquitecto", "manager"), 3, 55));
    }
}
