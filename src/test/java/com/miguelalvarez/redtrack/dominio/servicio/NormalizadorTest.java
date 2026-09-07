package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizadorTest {

    private final Normalizador normalizador = new Normalizador();

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "'Desarrollador/a Java Júnior', 'desarrollador a java junior'",
            "'  MADRID,  Comunidad de Madrid ', 'madrid comunidad de madrid'",
            "'Backend Developer (m/f/d)', 'backend developer'",
            "'Java Dev - Ref. 12345', 'java dev'",
            "'Programador Jr.', 'programador'"
    })
    @DisplayName("minusculas, sin tildes, sin puntuacion y sin coletillas")
    void normalizaTexto(String entrada, String esperado) {
        assertThat(normalizador.normalizar(entrada)).isEqualTo(esperado);
    }

    @Test
    void textoNuloOVacioDaCadenaVacia() {
        assertThat(normalizador.normalizar(null)).isEmpty();
        assertThat(normalizador.normalizar("   ")).isEmpty();
    }

    @Test
    @DisplayName("la huella es estable frente a mayusculas, tildes y espacios")
    void huellaEstable() {
        Oferta a = oferta("Desarrollador Java", "Coremain", "A Coruña");
        Oferta b = oferta("  desarrollador   JAVA ", "coremain", "A Coruna");

        assertThat(normalizador.huellaDe(a)).isEqualTo(normalizador.huellaDe(b));
    }

    @Test
    @DisplayName("ofertas distintas dan huellas distintas")
    void huellaDiscrimina() {
        Oferta a = oferta("Desarrollador Java", "Coremain", "Santiago");
        Oferta b = oferta("Desarrollador Java", "Altia", "Santiago");

        assertThat(normalizador.huellaDe(a)).isNotEqualTo(normalizador.huellaDe(b));
    }

    @Test
    void laHuellaEsUnSha256Hexadecimal() {
        assertThat(normalizador.huellaDe(oferta("t", "e", "u")))
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }

    private Oferta oferta(String titulo, String empresa, String ubicacion) {
        return new Oferta("adzuna", "1", titulo, empresa, ubicacion, Modalidad.DESCONOCIDA,
                null, null, "", "https://ejemplo", Idioma.ES,
                Instant.now(), Instant.now(), null);
    }
}
