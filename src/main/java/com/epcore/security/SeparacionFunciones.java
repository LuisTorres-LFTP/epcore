package com.epcore.security;

import com.epcore.entity.Permiso;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.epcore.entity.Permiso.*;

/**
 * Reglas de separación de funciones que NINGÚN cliente puede desactivar.
 * Previenen conflictos de interés y fraude: nadie concentra en un solo rol
 * dos pasos de control que deben hacer personas distintas.
 */
public final class SeparacionFunciones {

    /** Pares de permisos que no pueden estar en un mismo rol, con el motivo. */
    private record Conflicto(Permiso a, Permiso b, String motivo) { }

    private static final List<Conflicto> CONFLICTOS = List.of(
            new Conflicto(DOCUMENTOS_APROBAR_NIVEL_1, DOCUMENTOS_APROBAR_NIVEL_2,
                    "La aprobación dual de documentos exige dos personas distintas."),
            new Conflicto(PROCESOS_APROBAR_NIVEL_1, PROCESOS_APROBAR_NIVEL_2,
                    "La aprobación dual de procesos exige dos personas distintas."),
            new Conflicto(PROCESOS_CREAR, PROCESOS_ADJUDICAR,
                    "Quien estructura un proceso no puede adjudicarlo."),
            new Conflicto(PROCESOS_CREAR, PROCESOS_APROBAR_NIVEL_2,
                    "Quien estructura un proceso no puede aprobarlo como supervisor."),
            new Conflicto(ROLES_GESTIONAR, DOCUMENTOS_APROBAR_NIVEL_2,
                    "Quien administra los permisos no puede ser aprobador final."),
            new Conflicto(ROLES_GESTIONAR, PROCESOS_ADJUDICAR,
                    "Quien administra los permisos no puede adjudicar.")
    );

    private SeparacionFunciones() {
    }

    /** Devuelve los conflictos del conjunto de permisos; vacío si es válido. */
    public static List<String> validarRol(Set<Permiso> permisos) {
        List<String> errores = new ArrayList<>();
        if (permisos == null || permisos.isEmpty()) {
            errores.add("El rol debe tener al menos un permiso.");
            return errores;
        }

        for (Conflicto c : CONFLICTOS) {
            if (permisos.contains(c.a()) && permisos.contains(c.b())) {
                errores.add(c.a() + " y " + c.b() + " no pueden estar en el mismo rol: " + c.motivo());
            }
        }

        if (permisos.contains(PORTAL_PROVEEDOR) && permisos.size() > 1) {
            errores.add("Un rol de proveedor (portal) no puede tener permisos internos.");
        }

        return errores;
    }

    /**
     * Reglas por acción: se validan en el momento de aprobar, aunque los
     * roles estén bien configurados.
     *
     * @param aprobador     usuario que intenta aprobar
     * @param cargadoPor    usuario que cargó o creó el elemento
     * @param primerNivel   usuario que hizo la primera aprobación (null si es el nivel 1)
     */
    public static void validarAprobacion(String aprobador, String cargadoPor, String primerNivel) {
        if (aprobador == null || aprobador.isBlank()) {
            throw new IllegalStateException("No se identificó al usuario que aprueba.");
        }
        if (aprobador.equalsIgnoreCase(cargadoPor)) {
            throw new IllegalStateException("Nadie puede aprobar algo que cargó o creó él mismo.");
        }
        if (primerNivel != null && aprobador.equalsIgnoreCase(primerNivel)) {
            throw new IllegalStateException("La segunda aprobación debe hacerla una persona distinta a la primera.");
        }
    }
}
