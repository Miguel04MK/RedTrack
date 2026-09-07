package com.miguelalvarez.redtrack.dominio.servicio;

import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.modelo.Tecnologia;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detecta que tecnologias pide una oferta.
 *
 * <p>La parte critica del proyecto, y la mas facil de fallar. Los casos que hay
 * que respetar SIEMPRE:
 *
 * <pre>
 *   "Java" NO debe casar dentro de "JavaScript"   &lt;- el clasico
 *   "C"    NO debe casar dentro de "C++" ni "C#"
 *   "Go"   NO debe casar dentro de "Google" ni "Django"
 * </pre>
 *
 * <p>InfoJobs tiene exactamente este bug: empareja una "C" del perfil con ofertas
 * de "C++". Aqui esta resuelto con limites de palabra mas una comprobacion
 * explicita del caracter siguiente.
 *
 * <p>Hay un test por cada caso. No los borres.
 */
public final class DetectorTecnologias {

    /** Caracteres que, pegados al termino, lo convierten en OTRA tecnologia. */
    private static final String SUFIJOS_QUE_CAMBIAN_EL_TERMINO = "+#";

    /** Compilar regex es caro; se cachean por termino. */
    private final Map<String, Pattern> cache = new ConcurrentHashMap<>();

    /**
     * Tecnologias del perfil mencionadas en el texto, con el nombre canonico
     * del perfil (no el alias con el que aparecieron).
     */
    public Set<String> detectar(String texto, Perfil perfil) {
        Set<String> encontradas = new LinkedHashSet<>();
        if (texto == null || texto.isBlank()) {
            return encontradas;
        }
        String enMinusculas = texto.toLowerCase(Locale.ROOT);
        for (Tecnologia tecnologia : perfil.tecnologias()) {
            for (String forma : tecnologia.todasLasFormas()) {
                if (aparece(enMinusculas, forma)) {
                    encontradas.add(tecnologia.nombre());
                    break;
                }
            }
        }
        return encontradas;
    }

    /**
     * true si {@code termino} aparece en {@code texto} como tecnologia y no como
     * prefijo de otra.
     *
     * <p>Publico a proposito: es lo que ejercitan los tests de las trampas.
     */
    public boolean aparece(String texto, String termino) {
        if (texto == null || termino == null || termino.isBlank()) {
            return false;
        }
        Matcher m = patronPara(termino).matcher(texto.toLowerCase(Locale.ROOT));
        while (m.find()) {
            int siguiente = m.end();
            // "C" en "C++" o "C#" no es "C": es otra tecnologia distinta.
            if (siguiente < texto.length()
                    && SUFIJOS_QUE_CAMBIAN_EL_TERMINO.indexOf(texto.charAt(siguiente)) >= 0) {
                continue;
            }
            return true;
        }
        return false;
    }

    private Pattern patronPara(String termino) {
        return cache.computeIfAbsent(termino.toLowerCase(Locale.ROOT), t ->
                // \b no funciona con terminos que empiezan o acaban en simbolo
                // (c++, .net, c#), por eso se usan lookarounds sobre [a-z0-9].
                Pattern.compile("(?<![a-z0-9])" + Pattern.quote(t) + "(?![a-z0-9])",
                        Pattern.CASE_INSENSITIVE));
    }
}
