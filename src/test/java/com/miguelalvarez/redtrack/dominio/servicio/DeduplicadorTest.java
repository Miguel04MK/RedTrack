package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El test que hay que saber contar en una entrevista.
 *
 * <p>Jaro-Winkler da 0.93 entre "Desarrollador Java Junior" y "Desarrollador
 * Java Senior": por encima del umbral de 0.90. Si solo se mira la similitud, el
 * sistema fusiona una oferta junior con una senior y pierde la buena.
 *
 * <p>La salvaguarda de seniority es lo que lo evita.
 */
class DeduplicadorTest {

    private final Normalizador normalizador = new Normalizador();
    private final Deduplicador deduplicador =
            new Deduplicador(normalizador, new ExtractorSenales());

    @Test
    @DisplayName("misma empresa, misma ubicacion y titulo casi identico -> duplicado")
    void tildesYBarrasNoCreanOfertasDistintas() {
        Oferta a = oferta("Desarrollador Java Junior", "Coremain", "Santiago");
        Oferta b = oferta("Desarrollador/a Java Júnior", "Coremain", "Santiago");

        assertThat(deduplicador.similitudDeTitulos(a, b)).isGreaterThan(0.90);
        assertThat(deduplicador.sonLaMisma(a, b)).isTrue();
    }

    @Test
    @DisplayName("LA TRAMPA: Junior vs Senior se parecen al 93% pero NO son la misma")
    void seniorityDistintaNuncaEsLaMismaOferta() {
        Oferta junior = oferta("Desarrollador Java Junior", "Coremain", "Santiago");
        Oferta senior = oferta("Desarrollador Java Senior", "Coremain", "Santiago");

        // La similitud pura las daria por iguales...
        assertThat(deduplicador.similitudDeTitulos(junior, senior))
                .isGreaterThan(Deduplicador.UMBRAL_SIMILITUD);

        // ...pero la salvaguarda de seniority lo impide.
        assertThat(deduplicador.sonLaMisma(junior, senior)).isFalse();
    }

    @Test
    @DisplayName("huella identica -> duplicado sin necesidad de comparar titulos")
    void huellaExactaBasta() {
        Oferta a = oferta("Desarrollador Java", "Coremain", "Santiago");
        Oferta b = oferta("desarrollador java", "COREMAIN", "santiago");

        assertThat(normalizador.huellaDe(a)).isEqualTo(normalizador.huellaDe(b));
        assertThat(deduplicador.sonLaMisma(a, b)).isTrue();
    }

    @Test
    @DisplayName("titulos parecidos en empresas distintas NO son la misma oferta")
    void empresaDistintaNoSeCompara() {
        Oferta a = oferta("Desarrollador Java Junior", "Coremain", "Santiago");
        Oferta b = oferta("Desarrollador Java Junior", "Altia", "Santiago");

        assertThat(deduplicador.sonLaMisma(a, b)).isFalse();
    }

    @Test
    @DisplayName("misma empresa en ciudades distintas son dos vacantes distintas")
    void ubicacionDistintaNoSeCompara() {
        Oferta a = oferta("Desarrollador Java Junior", "Coremain", "Santiago");
        Oferta b = oferta("Desarrollador Java Junior", "Coremain", "Madrid");

        assertThat(deduplicador.sonLaMisma(a, b)).isFalse();
    }

    @Test
    @DisplayName("titulos distintos en la misma empresa no se fusionan")
    void titulosDistintosNoSeFusionan() {
        Oferta a = oferta("Desarrollador Java Junior", "Coremain", "Santiago");
        Oferta b = oferta("Tecnico de sistemas", "Coremain", "Santiago");

        assertThat(deduplicador.sonLaMisma(a, b)).isFalse();
    }

    private Oferta oferta(String titulo, String empresa, String ubicacion) {
        return new Oferta("adzuna", "1", titulo, empresa, ubicacion, Modalidad.HIBRIDO,
                null, null, "", "https://ejemplo", Idioma.ES,
                Instant.now(), Instant.now(), null);
    }
}
