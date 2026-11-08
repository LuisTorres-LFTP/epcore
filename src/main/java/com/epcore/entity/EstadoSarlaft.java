package com.epcore.entity;

/**
 * Estado consolidado SARLAFT de un proveedor, a partir de sus consultas.
 */
public enum EstadoSarlaft {
    /** Falta al menos una lista obligatoria por consultar. */
    SIN_CONSULTAR,
    /** Todas las listas obligatorias consultadas y vigentes, sin coincidencias. */
    LIMPIO,
    /** Hay posibles coincidencias pendientes de revisión. */
    EN_REVISION,
    /** Hay al menos una coincidencia: el proveedor queda bloqueado. */
    BLOQUEADO,
    /** Alguna consulta obligatoria superó el periodo de vigencia. */
    VENCIDO
}
