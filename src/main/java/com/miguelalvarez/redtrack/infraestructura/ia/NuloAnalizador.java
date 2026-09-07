package com.miguelalvarez.redtrack.infraestructura.ia;

import com.miguelalvarez.redtrack.dominio.modelo.Oferta;
import com.miguelalvarez.redtrack.dominio.modelo.Perfil;
import com.miguelalvarez.redtrack.dominio.puerto.AnalizadorSemantico;

import java.util.Optional;

/**
 * La implementacion que hace que la IA sea de verdad opcional.
 *
 * <p>Sin esta clase, "IA desactivada" significaria comprobar null en cada punto
 * de uso. Con ella, el resto del codigo no se entera de si el modelo esta o no.
 */
public class NuloAnalizador implements AnalizadorSemantico {

    @Override
    public Optional<String> resumir(Oferta oferta, Perfil perfil) {
        return Optional.empty();
    }

    @Override
    public boolean estaDisponible() {
        return false;
    }
}
