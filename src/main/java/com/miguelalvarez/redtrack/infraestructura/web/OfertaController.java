package com.miguelalvarez.redtrack.infraestructura.web;

import com.miguelalvarez.redtrack.aplicacion.RecolectarOfertasUseCase;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.infraestructura.planificacion.TareaDiaria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API pequena, para poder ensenar el proyecto en directo.
 *
 * <p>El endpoint que importa es {@code POST /api/analizar}: es el modo
 * "pegame esta oferta que acabo de ver en InfoJobs". Sigue siendo util aunque
 * esa fuente no este integrada, y es lo que se ensena en una entrevista.
 *
 * <p>Documentacion en /swagger-ui.html
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Ofertas", description = "Consulta y analisis de ofertas")
public class OfertaController {

    private final RecolectarOfertasUseCase recolectar;
    private final TareaDiaria tareaDiaria;
    private final Perfil perfil;

    public OfertaController(RecolectarOfertasUseCase recolectar,
                            TareaDiaria tareaDiaria,
                            Perfil perfil) {
        this.recolectar = recolectar;
        this.tareaDiaria = tareaDiaria;
        this.perfil = perfil;
    }

    @GetMapping("/ofertas")
    @Operation(summary = "Ofertas guardadas, filtrables por encaje, modalidad y fecha")
    public List<OfertaAnalizada> listar(
            @RequestParam(defaultValue = "0") int minEncaje,
            @RequestParam(required = false) Modalidad modalidad,
            @RequestParam(required = false) LocalDate desde) {
        // TODO(fase-3): consulta con Specification sobre OfertaJpaRepository.
        throw new UnsupportedOperationException("TODO(fase-3): GET /api/ofertas");
    }

    @GetMapping("/ofertas/{id}")
    @Operation(summary = "Una oferta con su analisis completo")
    public ResponseEntity<OfertaAnalizada> porId(@PathVariable Long id) {
        // TODO(fase-3)
        throw new UnsupportedOperationException("TODO(fase-3): GET /api/ofertas/{id}");
    }

    @PostMapping("/analizar")
    @Operation(summary = "Analiza un texto de oferta pegado a mano, sin guardarlo")
    public AnalisisRespuesta analizar(@RequestBody AnalizarPeticion peticion) {
        // TODO(fase-2): construir una Oferta sintetica con el texto y pasarla
        //               por el Puntuador. No toca base de datos.
        throw new UnsupportedOperationException("TODO(fase-2): POST /api/analizar");
    }

    @GetMapping("/estadisticas")
    @Operation(summary = "Agregados por tecnologia y salario")
    public EstadisticasRespuesta estadisticas() {
        // TODO(fase-4): alimenta los dos graficos del README:
        //   - las 15 tecnologias mas pedidas en ofertas junior
        //   - salario medio junior por provincia
        throw new UnsupportedOperationException("TODO(fase-4): GET /api/estadisticas");
    }

    @PostMapping("/recolectar")
    @Operation(summary = "Dispara una recoleccion manual")
    public RecolectarOfertasUseCase.Resultado recolectarAhora() {
        // TODO(fase-4): proteger este endpoint antes de exponerlo en AWS.
        return recolectar.ejecutar(tareaDiaria.criterios(), perfil);
    }

    public record AnalizarPeticion(String texto) {
    }

    public record AnalisisRespuesta(int encaje, List<String> pide, List<String> teFalta,
                                    List<String> avisos) {
    }

    public record EstadisticasRespuesta(List<ConteoTecnologia> tecnologias,
                                        List<SalarioPorProvincia> salarios) {
        public record ConteoTecnologia(String tecnologia, long ofertas) {
        }

        public record SalarioPorProvincia(String provincia, int salarioMedio, long ofertas) {
        }
    }
}
