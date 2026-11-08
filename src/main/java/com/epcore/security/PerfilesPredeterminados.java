package com.epcore.security;

import com.epcore.entity.Permiso;
import com.epcore.entity.Rol;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.epcore.entity.Permiso.*;

/**
 * Permisos con los que nace cada rol base. Se copian a la base de datos
 * la primera vez; desde ahí el cliente los puede ajustar.
 */
public final class PerfilesPredeterminados {

    public record Perfil(String nombre, String descripcion, Set<Permiso> permisos) { }

    private static final Map<Rol, Perfil> PERFILES = new EnumMap<>(Rol.class);

    static {
        PERFILES.put(Rol.ADMIN, new Perfil("Administrador",
                "Configura usuarios, roles y catálogos. No aprueba documentos ni adjudica (separación de funciones).",
                EnumSet.of(USUARIOS_GESTIONAR, ROLES_GESTIONAR, CATALOGOS_GESTIONAR, AUDITORIA_VER,
                        PROVEEDORES_VER, PROVEEDORES_GESTIONAR, PROVEEDORES_ELIMINAR, DOCUMENTOS_CARGAR,
                        EVALUACIONES_VER, EVALUACIONES_REGISTRAR, EVALUACIONES_ELIMINAR, CLASIFICACION_RESOLVER)));
        PERFILES.put(Rol.ANALISTA_COMPRAS, new Perfil("Analista de compras",
                "Registra proveedores, carga documentos, evalúa y estructura procesos.",
                EnumSet.of(PROVEEDORES_VER, PROVEEDORES_GESTIONAR, DOCUMENTOS_CARGAR, CLASIFICACION_RESOLVER,
                        EVALUACIONES_VER, EVALUACIONES_REGISTRAR, PROCESOS_CREAR)));
        PERFILES.put(Rol.DIRECTOR_COMPRAS, new Perfil("Director de compras",
                "Primera aprobación de documentos y de términos de procesos; califica técnicamente.",
                EnumSet.of(PROVEEDORES_VER, EVALUACIONES_VER, DOCUMENTOS_APROBAR_NIVEL_1,
                        PROCESOS_APROBAR_NIVEL_1, PROCESOS_EVALUAR_TECNICO)));
        PERFILES.put(Rol.GERENTE, new Perfil("Gerente (supervisor)",
                "Segunda aprobación como supervisor, publica procesos y adjudica.",
                EnumSet.of(PROVEEDORES_VER, EVALUACIONES_VER, DOCUMENTOS_APROBAR_NIVEL_2,
                        PROCESOS_APROBAR_NIVEL_2, PROCESOS_EVALUAR_TECNICO, PROCESOS_ADJUDICAR)));
        PERFILES.put(Rol.AUDITOR, new Perfil("Auditor",
                "Consulta y trazabilidad, sin modificar nada.",
                EnumSet.of(PROVEEDORES_VER, EVALUACIONES_VER, AUDITORIA_VER)));
        PERFILES.put(Rol.OFICIAL_CUMPLIMIENTO, new Perfil("Oficial de cumplimiento",
                "Debida diligencia SARLAFT, independiente del área de compras.",
                EnumSet.of(PROVEEDORES_VER, SARLAFT_CONSULTAR, AUDITORIA_VER)));
        PERFILES.put(Rol.EVALUADOR_TECNICO, new Perfil("Evaluador técnico",
                "Experto de otra área que califica técnicamente las ofertas.",
                EnumSet.of(PROVEEDORES_VER, PROCESOS_EVALUAR_TECNICO)));
        PERFILES.put(Rol.AREA_SOLICITANTE, new Perfil("Área solicitante",
                "Área que necesita el bien o servicio y califica el desempeño del proveedor.",
                EnumSet.of(PROVEEDORES_VER, EVALUACIONES_VER, EVALUACIONES_REGISTRAR)));
        PERFILES.put(Rol.PROVEEDOR, new Perfil("Proveedor (portal)",
                "Usuario externo: solo ve y gestiona la información de su propia empresa.",
                EnumSet.of(PORTAL_PROVEEDOR)));
    }

    private PerfilesPredeterminados() {
    }

    public static Map<Rol, Perfil> todos() {
        return Collections.unmodifiableMap(PERFILES);
    }

    /** Permisos por defecto de un rol base (vacío para PERSONALIZADO). */
    public static Set<Permiso> permisosDe(Rol rol) {
        Perfil perfil = rol == null ? null : PERFILES.get(rol);
        return perfil == null ? EnumSet.noneOf(Permiso.class) : EnumSet.copyOf(perfil.permisos());
    }
}
