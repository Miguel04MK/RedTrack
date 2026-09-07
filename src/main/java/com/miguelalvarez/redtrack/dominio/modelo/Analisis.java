package com.miguelalvarez.redtrack.dominio.modelo;

import java.util.List;
import java.util.Set;

/**
 * Resultado de analizar una oferta contra el perfil.
 *
 * <p>Las banderas rojas NO restan puntos: se muestran aparte. La maquina informa,
 * la persona decide.
 *
 * @param encaje    0-100
 * @param senal     seniority detectada en la oferta
 * @param resumenIA null si el analizador semantico no esta disponible
 */
public record Analisis(
        int encaje,
        Set<String> tecnologiasPedidas,
        Set<String> lasTengo,
        Set<String> meFaltan,
        Seniority senal,
        Integer anosRequeridos,
        List<String> banderasRojas,
        String resumenIA
) {

    public Analisis {
        tecnologiasPedidas = tecnologiasPedidas == null ? Set.of() : Set.copyOf(tecnologiasPedidas);
        lasTengo = lasTengo == null ? Set.of() : Set.copyOf(lasTengo);
        meFaltan = meFaltan == null ? Set.of() : Set.copyOf(meFaltan);
        banderasRojas = banderasRojas == null ? List.of() : List.copyOf(banderasRojas);
    }

    /** true si entra en el resumen diario. */
    public boolean superaUmbral(int umbral) {
        return encaje >= umbral;
    }
}
