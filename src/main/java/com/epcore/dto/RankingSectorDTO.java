package com.epcore.dto;

public class RankingSectorDTO {

    private String sector;
    private String proveedor;
    private Double promedio;

    public RankingSectorDTO(String sector, String proveedor, Double promedio) {
        this.sector = sector;
        this.proveedor = proveedor;
        this.promedio = promedio;
    }

    public String getSector() {
        return sector;
    }

    public String getProveedor() {
        return proveedor;
    }

    public Double getPromedio() {
        return promedio;
    }
}