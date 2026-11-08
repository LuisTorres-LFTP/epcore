package com.epcore.entity;

/**
 * Posición de la actividad en el RUT: una principal, una secundaria
 * y hasta dos "otras actividades".
 */
public enum TipoActividad {
    PRINCIPAL(1),
    SECUNDARIA(1),
    OTRA(2);

    private final int maximo;

    TipoActividad(int maximo) {
        this.maximo = maximo;
    }

    public int getMaximo() {
        return maximo;
    }
}
