package com.epcore.entity;

/**
 * Resultado de una consulta en una lista restrictiva.
 */
public enum ResultadoSarlaft {
    /** No aparece en la lista. */
    SIN_COINCIDENCIA,
    /** Homónimo o dato parcial: requiere revisión del oficial de cumplimiento. */
    POSIBLE_COINCIDENCIA,
    /** Aparece en la lista: el proveedor no puede ser habilitado. */
    COINCIDENCIA
}
