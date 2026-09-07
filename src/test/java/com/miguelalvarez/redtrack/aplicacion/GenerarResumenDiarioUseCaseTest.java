package com.miguelalvarez.redtrack.aplicacion;

import com.miguelalvarez.redtrack.dominio.modelo.Analisis;
import com.miguelalvarez.redtrack.dominio.modelo.Idioma;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.Preferencias;
import com.miguelalvarez.redtrack.dominio.modelo.ResumenDiario;
import com.miguelalvarez.redtrack.dominio.modelo.Seniority;
import com.miguelalvarez.redtrack.dominio.puerto.Notificador;
import com.miguelalvarez.redtrack.dominio.puerto.RepositorioOfertas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El comportamiento que protege este test es el que evita dos desastres
 * simetricos: que una oferta se pierda, y que te llegue todos los dias.
 */
class GenerarResumenDiarioUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-09-07T06:00:00Z");
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final RepositorioFalso repositorio = new RepositorioFalso();
    private final NotificadorFalso notificador = new NotificadorFalso();

    private final GenerarResumenDiarioUseCase useCase =
            new GenerarResumenDiarioUseCase(repositorio, notificador, reloj);

    @Test
    @DisplayName("si el envio se confirma, marca las ofertas por su huella")
    void envioCorrectoMarcaLasOfertas() {
        repositorio.pendientes = List.of(analizada("huella-a", 87), analizada("huella-b", 62));
        notificador.vaAFuncionar = true;

        ResumenDiario resumen = useCase.ejecutar(perfil());

        assertThat(resumen.cuantas()).isEqualTo(2);
        assertThat(notificador.enviados).hasSize(1);
        assertThat(repositorio.marcadas).containsExactlyInAnyOrder("huella-a", "huella-b");
        assertThat(repositorio.cuando).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("si el envio falla NO marca nada: manana se reintentan")
    void envioFallidoNoMarcaNada() {
        repositorio.pendientes = List.of(analizada("huella-a", 87));
        notificador.vaAFuncionar = false;

        useCase.ejecutar(perfil());

        // Lo importante: no se pierden. Siguen pendientes para la proxima vez.
        assertThat(repositorio.marcadas).isEmpty();
    }

    @Test
    @DisplayName("sin ofertas nuevas no se envia nada")
    void sinOfertasNoSeEnvia() {
        repositorio.pendientes = List.of();

        ResumenDiario resumen = useCase.ejecutar(perfil());

        assertThat(resumen.estaVacio()).isTrue();
        assertThat(notificador.enviados).isEmpty();
        assertThat(repositorio.marcadas).isEmpty();
    }

    @Test
    @DisplayName("el resumen sale ordenado de mayor a menor encaje")
    void ordenadoPorEncaje() {
        repositorio.pendientes = List.of(
                analizada("huella-baja", 58),
                analizada("huella-alta", 91),
                analizada("huella-media", 70));
        notificador.vaAFuncionar = true;

        ResumenDiario resumen = useCase.ejecutar(perfil());

        assertThat(resumen.ofertas())
                .extracting(OfertaAnalizada::encaje)
                .containsExactly(91, 70, 58);
    }

    @Test
    @DisplayName("consulta las pendientes con el umbral del perfil")
    void usaElUmbralDelPerfil() {
        repositorio.pendientes = List.of();

        useCase.ejecutar(perfil());

        assertThat(repositorio.umbralPedido).isEqualTo(55);
    }

    // ------------------------------------------------------------------

    private Perfil perfil() {
        return new Perfil(List.of(),
                new Preferencias(List.of(), 24000, "B1", List.of(), 3, 55));
    }

    private OfertaAnalizada analizada(String huella, int encaje) {
        Oferta oferta = new Oferta("adzuna", "id-" + huella, "Desarrollador Java Junior",
                "Coremain", "Santiago", Modalidad.HIBRIDO, 21000, 25000,
                "descripcion", "https://ejemplo", Idioma.ES,
                AHORA, AHORA, huella);
        Analisis analisis = new Analisis(encaje, java.util.Set.of("Java"),
                java.util.Set.of("Java"), java.util.Set.of(),
                Seniority.JUNIOR, 1, List.of(), null);
        return OfertaAnalizada.de(oferta, analisis);
    }

    /** Doble de prueba: guarda lo que le piden en vez de tocar base de datos. */
    private static final class RepositorioFalso implements RepositorioOfertas {
        List<OfertaAnalizada> pendientes = List.of();
        List<String> marcadas = new ArrayList<>();
        Instant cuando;
        Integer umbralPedido;

        @Override
        public List<OfertaAnalizada> pendientesDeNotificar(int umbralEncaje) {
            this.umbralPedido = umbralEncaje;
            return pendientes;
        }

        @Override
        public void marcarNotificadas(List<String> huellas, Instant cuando) {
            this.marcadas.addAll(huellas);
            this.cuando = cuando;
        }

        @Override
        public void guardar(OfertaAnalizada analizada) {
        }

        @Override
        public Optional<Oferta> porHuella(String huella) {
            return Optional.empty();
        }

        @Override
        public boolean existe(String fuente, String idExterno) {
            return false;
        }

        @Override
        public void registrarAparicion(String huellaCanonica, String fuente,
                                       String idExterno, String url) {
        }

        @Override
        public List<Oferta> candidatasParaCotejar(String empresa, String ubicacion) {
            return List.of();
        }
    }

    private static final class NotificadorFalso implements Notificador {
        boolean vaAFuncionar = true;
        List<ResumenDiario> enviados = new ArrayList<>();

        @Override
        public String nombre() {
            return "falso";
        }

        @Override
        public boolean enviar(ResumenDiario resumen) {
            enviados.add(resumen);
            return vaAFuncionar;
        }
    }
}
