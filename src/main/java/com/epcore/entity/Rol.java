package com.epcore.entity;

/**
 * Roles base de EpCore. Sus permisos por defecto están en
 * {@link com.epcore.security.PerfilesPredeterminados} y cada cliente puede
 * ajustarlos. PERSONALIZADO indica que el usuario usa un rol creado por el cliente.
 */
public enum Rol {
    ADMIN,
    ANALISTA_COMPRAS,
    AUDITOR,
    GERENTE,
    DIRECTOR_COMPRAS,
    OFICIAL_CUMPLIMIENTO,
    EVALUADOR_TECNICO,
    AREA_SOLICITANTE,
    PROVEEDOR,
    PERSONALIZADO
}
