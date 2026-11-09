package com.epcore.service.proceso;

import com.epcore.entity.proceso.FactoresPuntaje;
import com.epcore.entity.proceso.ProcesoEnums.Modalidad;
import com.epcore.entity.proceso.ProcesoEnums.Visibilidad;
import com.epcore.entity.proceso.RequisitosProceso;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Patrón Strategy: cada modalidad define su plantilla de requisitos, sus
 * factores de puntaje por defecto y sus restricciones. El ciclo de vida del
 * proceso es el mismo para todas.
 */
public interface ReglasModalidad {

    Modalidad modalidad();

    RequisitosProceso plantillaRequisitos();

    FactoresPuntaje plantillaFactores();

    /** Visibilidad obligatoria o por defecto para la modalidad. */
    Visibilidad visibilidadPorDefecto();

    /** Restricciones adicionales de la modalidad sobre los factores. */
    default List<String> validarFactores(FactoresPuntaje f) {
        List<String> errores = new ArrayList<>();
        if (f == null) {
            errores.add("Defina los factores de puntaje.");
            return errores;
        }
        if (f.total() != 100) {
            errores.add("Los factores de puntaje suman " + f.total() + "; deben sumar 100.");
        }
        if (f.getPrecio() < 0 || f.getTecnica() < 0 || f.getPlazo() < 0 || f.getExperiencia() < 0
                || f.getDesempeno() < 0 || f.getIndustriaNacional() < 0) {
            errores.add("Ningún factor puede ser negativo.");
        }
        return errores;
    }

    static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
