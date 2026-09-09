package com.miguelalvarez.redtrack.infraestructura.web;

import com.miguelalvarez.redtrack.aplicacion.AnalizarTextoUseCase;
import com.miguelalvarez.redtrack.aplicacion.GenerarResumenDiarioUseCase;
import com.miguelalvarez.redtrack.aplicacion.RecolectarOfertasUseCase;
import com.miguelalvarez.redtrack.dominio.modelo.FiltroOfertas;
import com.miguelalvarez.redtrack.dominio.modelo.Modalidad;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.ResumenDiario;
import com.miguelalvarez.redtrack.dominio.puerto.ConsultaDeOfertas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
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
 * "pegame esta oferta que acabo de ver". Sigue siendo util aunque esa fuente no
 * este integrada, y es la respuesta practica a que Adzuna recorte las
 * descripciones a 500 caracteres.
 *
 * <p>Las ofertas se referencian por su HUELLA, no por un id de base de datos:
 * es una clave natural del dominio y evita exponer la identidad de JPA.
 *
 * <p>Documentacion en /swagger-ui.html
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Ofertas", description = "Consulta y analisis de ofertas")
public class OfertaController {

    private final ConsultaDeOfertas consulta;
    private final AnalizarTextoUseCase analizarTexto;
    private final RecolectarOfertasUseCase recolectar;
    private final GenerarResumenDiarioUseCase generarResumen;
    private final Perfil perfil;

    public OfertaController(ConsultaDeOfertas consulta,
                            AnalizarTextoUseCase analizarTexto,
                            RecolectarOfertasUseCase recolectar,
                            GenerarResumenDiarioUseCase generarResumen,
                            Perfil perfil) {
        this.consulta = consulta;
        this.analizarTexto = analizarTexto;
        this.recolectar = recolectar;
        this.generarResumen = generarResumen;
        this.perfil = perfil;
    }

    @GetMapping("/ofertas")
    @Operation(summary = "Ofertas guardadas, de mayor a menor encaje")
    public List<OfertaAnalizada> listar(
            @Parameter(description = "Encaje minimo, 0-100")
            @RequestParam(required = false) Integer minEncaje,
            @RequestParam(required = false) Modalidad modalidad,
            @Parameter(description = "Publicadas a partir de esta fecha")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "adzuna, remotive...")
            @RequestParam(required = false) String fuente,
            @Parameter(description = "Maximo 200")
            @RequestParam(defaultValue = "50") int limite) {

        return consulta.buscar(
                new FiltroOfertas(minEncaje, modalidad, desde, fuente, limite));
    }

    @GetMapping("/ofertas/{huella}")
    @Operation(summary = "Una oferta con su analisis completo, por su huella")
    public ResponseEntity<OfertaAnalizada> porHuella(@PathVariable String huella) {
        return consulta.porHuella(huella)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Analiza una oferta pegada a mano, sin guardarla.
     *
     * <p>Es lo que se ensena en directo, y lo que permite analizar entera una
     * oferta de un portal que no se puede integrar por sus terminos de uso.
     */
    @PostMapping("/analizar")
    @Operation(summary = "Analiza un texto de oferta pegado a mano, sin guardarlo")
    public AnalizarTextoUseCase.Resultado analizar(@RequestBody AnalizarPeticion peticion) {
        return analizarTexto.ejecutar(peticion.titulo(), peticion.texto(), perfil);
    }

    @GetMapping("/estadisticas")
    @Operation(summary = "Tecnologias mas pedidas y salario medio por ubicacion")
    public ConsultaDeOfertas.Estadisticas estadisticas(
            @RequestParam(defaultValue = "15") int topTecnologias) {
        return consulta.estadisticas(topTecnologias);
    }

    @PostMapping("/recolectar")
    @Operation(summary = "Dispara una recoleccion manual")
    public RecolectarOfertasUseCase.Resultado recolectarAhora() {
        return recolectar.ejecutar(perfil.criterios(), perfil);
    }

    /**
     * Genera y ENVIA el resumen ahora, sin esperar al cron de las 8:00.
     *
     * <p>Evita tener que esperar a manana para comprobar un cambio en la
     * clasificacion.
     */
    @PostMapping("/resumen")
    @Operation(summary = "Genera y envia el resumen diario ahora mismo")
    public ResumenDiario resumenAhora() {
        return generarResumen.ejecutar(perfil);
    }

    /**
     * @param titulo opcional, pero conviene: la seniority y el filtro de titulos
     *               vetados solo miran ahi
     */
    public record AnalizarPeticion(String titulo, @NotBlank String texto) {
    }
}
