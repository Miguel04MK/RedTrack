package com.miguelalvarez.redtrack.aplicacion;

import com.miguelalvarez.redtrack.dominio.modelo.Analisis;
import com.miguelalvarez.redtrack.dominio.modelo.CriterioBusqueda;
import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.OfertaAnalizada;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.puerto.AnalizadorSemantico;
import com.miguelalvarez.redtrack.dominio.puerto.FuenteDeOfertas;
import com.miguelalvarez.redtrack.dominio.puerto.RepositorioOfertas;
import com.miguelalvarez.redtrack.dominio.servicio.Deduplicador;
import com.miguelalvarez.redtrack.dominio.servicio.Normalizador;
import com.miguelalvarez.redtrack.dominio.servicio.Puntuador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Recorre las fuentes, normaliza, deduplica, puntua y guarda.
 *
 * <p>Es el flujo de los pasos 1 a 3 del resumen diario. El envio va aparte,
 * en {@link GenerarResumenDiarioUseCase}, para poder recolectar sin notificar
 * (util al depurar y al rellenar la base de datos por primera vez).
 */
public class RecolectarOfertasUseCase {

    private static final Logger log = LoggerFactory.getLogger(RecolectarOfertasUseCase.class);

    private final List<FuenteDeOfertas> fuentes;
    private final RepositorioOfertas repositorio;
    private final Normalizador normalizador;
    private final Deduplicador deduplicador;
    private final Puntuador puntuador;
    private final AnalizadorSemantico analizadorSemantico;

    public RecolectarOfertasUseCase(List<FuenteDeOfertas> fuentes,
                                    RepositorioOfertas repositorio,
                                    Normalizador normalizador,
                                    Deduplicador deduplicador,
                                    Puntuador puntuador,
                                    AnalizadorSemantico analizadorSemantico) {
        this.fuentes = fuentes;
        this.repositorio = repositorio;
        this.normalizador = normalizador;
        this.deduplicador = deduplicador;
        this.puntuador = puntuador;
        this.analizadorSemantico = analizadorSemantico;
    }

    /**
     * Ejecuta una recoleccion completa.
     *
     * @return cuantas ofertas nuevas se han guardado
     */
    public Resultado ejecutar(List<CriterioBusqueda> criterios, Perfil perfil) {
        int vistas = 0;
        int nuevas = 0;
        int duplicadas = 0;

        for (FuenteDeOfertas fuente : fuentes) {
            if (!fuente.estaActiva()) {
                log.info("Fuente {} desactivada, se salta", fuente.nombre());
                continue;
            }
            for (CriterioBusqueda criterio : criterios) {
                // El contrato del puerto dice que una fuente caida devuelve lista
                // vacia y no propaga. El try es el cinturon por si alguna se lo salta.
                List<Oferta> encontradas;
                try {
                    encontradas = fuente.buscar(criterio);
                } catch (RuntimeException e) {
                    log.error("La fuente {} ha fallado con el criterio {}: {}",
                            fuente.nombre(), criterio, e.getMessage());
                    continue;
                }

                for (Oferta enBruto : encontradas) {
                    vistas++;
                    Oferta oferta = enBruto.conHuella(normalizador.huellaDe(enBruto));

                    if (repositorio.existe(oferta.fuente(), oferta.idExterno())) {
                        duplicadas++;
                        continue;
                    }
                    Optional<Oferta> canonica = buscarCanonica(oferta);
                    if (canonica.isPresent()) {
                        duplicadas++;
                        // TODO(fase-3): recuperar el id de la canonica y llamar a
                        //               repositorio.registrarAparicion(...) para
                        //               alimentar la tabla oferta_fuente.
                        continue;
                    }

                    Analisis analisis = puntuador.analizar(oferta, perfil);
                    analisis = conResumenDeIa(oferta, perfil, analisis);
                    repositorio.guardar(OfertaAnalizada.de(oferta, analisis));
                    nuevas++;
                }
            }
        }

        log.info("Recoleccion terminada: {} vistas, {} nuevas, {} duplicadas",
                vistas, nuevas, duplicadas);
        return new Resultado(vistas, nuevas, duplicadas);
    }

    /** Paso 1 huella exacta, paso 2 similitud sobre las candidatas de la misma empresa. */
    private Optional<Oferta> buscarCanonica(Oferta oferta) {
        Optional<Oferta> porHuella = repositorio.porHuella(oferta.huella());
        if (porHuella.isPresent()) {
            return porHuella;
        }
        return repositorio.candidatasParaCotejar(
                        normalizador.normalizar(oferta.empresa()),
                        normalizador.normalizar(oferta.ubicacion()))
                .stream()
                .filter(conocida -> deduplicador.sonLaMisma(oferta, conocida))
                .findFirst();
    }

    /**
     * La IA es opcional y degrada: si no esta disponible o falla, el analisis se
     * queda igual y no pasa nada.
     */
    private Analisis conResumenDeIa(Oferta oferta, Perfil perfil, Analisis analisis) {
        return analizadorSemantico.resumir(oferta, perfil)
                .map(resumen -> new Analisis(
                        analisis.encaje(), analisis.tecnologiasPedidas(), analisis.lasTengo(),
                        analisis.meFaltan(), analisis.senal(), analisis.anosRequeridos(),
                        analisis.banderasRojas(), resumen))
                .orElse(analisis);
    }

    /** Cifras de una recoleccion, para el log y para el endpoint POST /api/recolectar. */
    public record Resultado(int vistas, int nuevas, int duplicadas) {
    }
}
