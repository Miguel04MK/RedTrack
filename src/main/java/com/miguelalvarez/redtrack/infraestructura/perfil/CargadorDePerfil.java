package com.miguelalvarez.redtrack.infraestructura.perfil;

import com.miguelalvarez.redtrack.dominio.modelo.Busquedas;
import com.miguelalvarez.redtrack.dominio.modelo.Nivel;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.Preferencias;
import com.miguelalvarez.redtrack.dominio.modelo.Tecnologia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lee {@code perfil.yaml}.
 *
 * <p>El perfil vive fuera del codigo para poder ajustar pesos y preferencias sin
 * recompilar. Se carga una vez al arrancar.
 */
public class CargadorDePerfil {

    private static final Logger log = LoggerFactory.getLogger(CargadorDePerfil.class);

    private final Resource recurso;

    public CargadorDePerfil(Resource recurso) {
        this.recurso = recurso;
    }

    public Perfil cargar() {
        try (InputStream in = recurso.getInputStream()) {
            Map<String, Object> raiz = new Yaml().load(in);
            if (raiz == null) {
                throw new IllegalStateException("perfil.yaml esta vacio");
            }
            Perfil perfil = new Perfil(
                    leerTecnologias(raiz),
                    leerPreferencias(raiz),
                    leerBusquedas(raiz));

            log.info("Perfil cargado: {} tecnologias, umbral de encaje {}, {} criterios",
                    perfil.tecnologias().size(), perfil.preferencias().umbralEncaje(),
                    perfil.criterios().size());

            if (perfil.criterios().isEmpty()) {
                log.warn("El perfil no define busquedas: no se va a recolectar nada. "
                        + "Revisa la seccion 'busquedas' de perfil.yaml");
            }
            return perfil;
        } catch (IOException e) {
            throw new IllegalStateException("No se ha podido leer perfil.yaml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Tecnologia> leerTecnologias(Map<String, Object> raiz) {
        List<Map<String, Object>> crudas =
                (List<Map<String, Object>>) raiz.getOrDefault("tecnologias", List.of());
        List<Tecnologia> tecnologias = new ArrayList<>();
        for (Map<String, Object> cruda : crudas) {
            tecnologias.add(new Tecnologia(
                    texto(cruda.get("nombre")),
                    nivel(cruda.get("nivel")),
                    entero(cruda.get("peso"), 1),
                    (List<String>) cruda.getOrDefault("alias", List.of())));
        }
        return tecnologias;
    }

    @SuppressWarnings("unchecked")
    private Preferencias leerPreferencias(Map<String, Object> raiz) {
        Map<String, Object> p =
                (Map<String, Object>) raiz.getOrDefault("preferencias", Map.of());
        return new Preferencias(
                (List<String>) p.getOrDefault("ubicaciones_deseadas", List.of()),
                entero(p.get("salario_objetivo"), 0),
                texto(p.getOrDefault("idioma_maximo", "B1")),
                (List<String>) p.getOrDefault("descartar_si_titulo_contiene", List.of()),
                entero(p.get("anos_maximos_aceptables"), 3),
                entero(p.get("umbral_encaje"), 55));
    }

    @SuppressWarnings("unchecked")
    private Busquedas leerBusquedas(Map<String, Object> raiz) {
        Map<String, Object> b = (Map<String, Object>) raiz.get("busquedas");
        if (b == null) {
            return Busquedas.vacias();
        }
        return new Busquedas(
                (List<String>) b.getOrDefault("terminos", List.of()),
                (List<String>) b.getOrDefault("terminos_exploratorios", List.of()),
                (List<String>) b.getOrDefault("ubicaciones", List.of()),
                entero(b.get("max_dias_antiguedad"), 7),
                entero(b.get("max_resultados_por_fuente"), 50));
    }

    private Nivel nivel(Object valor) {
        if (valor == null) {
            return Nivel.NINGUNO;
        }
        try {
            return Nivel.valueOf(valor.toString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("Nivel desconocido en perfil.yaml: '{}'. Se usa NINGUNO", valor);
            return Nivel.NINGUNO;
        }
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private int entero(Object valor, int porDefecto) {
        if (valor instanceof Number n) {
            return n.intValue();
        }
        if (valor == null) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(valor.toString().trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
