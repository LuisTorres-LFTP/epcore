package com.epcore.entity;

/**
 * Permisos atómicos de EpCore. Son fijos en el código porque cada uno
 * protege una acción concreta; los roles (perfiles) se arman combinándolos
 * y esos sí los configura cada cliente.
 */
public enum Permiso {

    // Administración
    USUARIOS_GESTIONAR("Administración", "Crear, editar, activar e inactivar usuarios"),
    ROLES_GESTIONAR("Administración", "Crear y modificar roles y sus permisos"),
    CATALOGOS_GESTIONAR("Administración", "Gestionar criterios, nichos y catálogos de clasificación"),
    AUDITORIA_VER("Administración", "Consultar la trazabilidad de acciones"),

    // Proveedores
    PROVEEDORES_VER("Proveedores", "Consultar proveedores, documentos e indicadores"),
    PROVEEDORES_GESTIONAR("Proveedores", "Registrar y editar proveedores"),
    PROVEEDORES_ELIMINAR("Proveedores", "Eliminar proveedores"),
    CLASIFICACION_RESOLVER("Proveedores", "Resolver excepciones de clasificación (CIIU / nicho)"),

    // Documentos (aprobación dual)
    DOCUMENTOS_CARGAR("Documentos", "Cargar documentos de proveedores"),
    DOCUMENTOS_APROBAR_NIVEL_1("Documentos", "Primera aprobación de documentos (Director de compras)"),
    DOCUMENTOS_APROBAR_NIVEL_2("Documentos", "Segunda aprobación como supervisor (Gerente)"),

    // Cumplimiento
    SARLAFT_CONSULTAR("Cumplimiento", "Consultar listas restrictivas y registrar el resultado"),

    // Evaluaciones
    EVALUACIONES_VER("Evaluaciones", "Consultar evaluaciones y rankings"),
    EVALUACIONES_REGISTRAR("Evaluaciones", "Registrar evaluaciones de desempeño"),
    EVALUACIONES_ELIMINAR("Evaluaciones", "Eliminar evaluaciones"),

    // Procesos de contratación
    PROCESOS_CREAR("Procesos", "Crear procesos y ajustar sus requisitos"),
    PROCESOS_APROBAR_NIVEL_1("Procesos", "Primera aprobación de términos"),
    PROCESOS_APROBAR_NIVEL_2("Procesos", "Segunda aprobación y publicación"),
    PROCESOS_EVALUAR_TECNICO("Procesos", "Calificar técnicamente las ofertas"),
    PROCESOS_ADJUDICAR("Procesos", "Adjudicar o declarar desierto un proceso"),

    // Externo
    PORTAL_PROVEEDOR("Portal", "Acceso al portal de proveedores (solo su propia información)");

    private final String grupo;
    private final String descripcion;

    Permiso(String grupo, String descripcion) {
        this.grupo = grupo;
        this.descripcion = descripcion;
    }

    public String getGrupo() {
        return grupo;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
