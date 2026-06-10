package com.epcore.dto;

public class RankingProveedorDTO {

    private String proveedor;
    private Double promedio;

    public RankingProveedorDTO(String proveedor, Double promedio) {
        this.proveedor = proveedor;
        this.promedio = promedio;
    }

    public String getProveedor() {
        return proveedor;
    }

    public Double getPromedio() {
        return promedio;
    }
}
