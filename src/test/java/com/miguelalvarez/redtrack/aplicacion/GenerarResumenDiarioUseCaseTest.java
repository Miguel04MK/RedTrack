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
import com.miguelalvarez.redtrack.dominio.servicio.Accesibilidad;
import com.miguelalvarez.redtrack.dominio.servicio.ClasificadorDeOfertas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    private final GenerarResumenDiarioUseCase useCase = new GenerarResumenDiarioUseCase(
            repositorio,
            new ClasificadorDeOfertas(new Accesibilidad()),
            notificador,
            reloj);

    @Test
    @DisplayName("reparte las ofertas entre las dos secciones")
    void reparteEnDosSecciones() {
        repositorio.pendientes = List.of(
                deTuStack("huella-a", 87),
                juniorDeOtroStack("huella-b"),
                senior("huella-c"));
        notificador.vaAFuncionar = true;

        ResumenDiario resumen = useCase.ejecutar(perfil());

        assertThat(resumen.paraTi()).extracting(o -> o.oferta().huella())
                .containsExactly("huella-a");
        assertThat(resumen.podrianInteresarte()).extracting(o -> o.oferta().huella())
                .containsExactly("huella-b");
    }

    @Test
    @DisplayName("una oferta senior no entra en ninguna de las dos secciones")
    void laSeniorNoEntraEnNinguna() {
        repositorio.pendientes = List.of(senior("huella-c"));

        ResumenDiario resumen = useCase.ejecutar(perfil());

        assertThat(resumen.estaVacio()).isTrue();
    }

    @Test
    @DisplayName("si el envio se confirma, marca TODAS las evaluadas, entren o no")
    void marcaTodasLasEvaluadas() {
        repositorio.pendientes = List.of(
                deTuStack("huella-a", 87),
                juniorDeOtroStack("huella-b"),
                senior("huella-c"));
        notificador.vaAFuncionar = true;

        useCase.ejecutar(perfil());

        // Tambien la senior: el veredicto es determinista, evaluarla otra vez
        // manana daria lo mismo y el conjunto de pendientes creceria sin fin.
        assertThat(repositorio.marcadas)
                .containsExactlyInAnyOrder("huella-a", "huella-b", "huella-c");
        assertThat(repositorio.cuando).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("si el envio falla NO marca nada: manana se reintentan")
    void envioFallidoNoMarcaNada() {
        repositorio.pendientes = List.of(deTuStack("huella-a", 87));
        notificador.vaAFuncionar = false;

        useCase.ejecutar(perfil());

        assertThat(repositorio.marcadas).isEmpty();
    }

    @Test
    @DisplayName("sin nada que enviar no se llama al notificador")
    void sinOfertasNoSeEnvia() {
        repositorio.pendientes = List.of();

        ResumenDiario resumen = useCase.ejecutar(perfil());

        assertThat(resumen.estaVacio()).isTrue();
        assertThat(notificador.enviados).isEmpty();
    }

    @Test
    @DisplayName("cada seccion sale ordenada de mayor a menor encaje")
    void ordenadoPorEncaje() {
        repositorio.pendientes = List.of(
                deTuStack("baja", 58), deTuStack("alta", 91), deTuStack("media", 70));
        notificador.vaAFuncionar = true;

        ResumenDiario resumen = useCase.ejecutar(perfil());

        assertThat(resumen.paraTi())
                .extracting(OfertaAnalizada::encaje)
                .containsExactly(91, 70, 58);
    }

    // ------------------------------------------------------------------

    private Perfil perfil() {
        return new Perfil(List.of(),
                new Preferencias(List.of(), 24000, "B1", List.of(), 3, 55));
    }

    /** Encaje por encima del umbral: seccion "para ti". */
    private OfertaAnalizada deTuStack(String huella, int encaje) {
        return analizada(huella, "Desarrollador Java Junior", encaje,
                Seniority.JUNIOR, 1, null);
    }

    /** Junior de desarrollo con encaje bajo: seccion "podrian interesarte". */
    private OfertaAnalizada juniorDeOtroStack(String huella) {
        return analizada(huella, "Desarrollador PHP Junior", 21,
                Seniority.JUNIOR, 1, "PHP");
    }

    /** Ni una cosa ni la otra. */
    private OfertaAnalizada senior(String huella) {
        return analizada(huella, "Senior Java Software Engineer", 0,
                Seniority.SENIOR, 5, null);
    }

    private OfertaAnalizada analizada(String huella, String titulo, int encaje,
                                      Seniority senal, Integer anos, String falta) {
        Oferta oferta = new Oferta("adzuna", "id-" + huella, titulo,
                "Coremain", "Santiago", Modalidad.HIBRIDO, null, null,
                "descripcion", "https://ejemplo", Idioma.ES, AHORA, AHORA, huella);
        Analisis analisis = new Analisis(encaje, Set.of(), Set.of(),
                falta == null ? Set.of() : Set.of(falta),
                senal, anos, List.of(), null);
        return OfertaAnalizada.de(oferta, analisis);
    }

    /** Doble de prueba: guarda lo que le piden en vez de tocar base de datos. */
    private static final class RepositorioFalso implements RepositorioOfertas {
        List<OfertaAnalizada> pendientes = List.of();
        List<String> marcadas = new ArrayList<>();
        Instant cuando;

        @Override
        public List<OfertaAnalizada> pendientes() {
            return pendientes;
        }

        @Override
        public void marcarProcesadas(List<String> huellas, Instant cuando) {
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
