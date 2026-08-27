package com.epcore.dto;

public class IndicadorSectorDTO {

    private String sector;
    private String mejorProveedor;
    private Double mejorPromedio;
    private String proveedorEnRiesgo;
    private Double promedioRiesgo;

    public IndicadorSectorDTO(
            String sector,
            String mejorProveedor,
            Double mejorPromedio,
            String proveedorEnRiesgo,
            Double promedioRiesgo) {
        this.sector = sector;
        this.mejorProveedor = mejorProveedor;
        this.mejorPromedio = mejorPromedio;
        this.proveedorEnRiesgo = proveedorEnRiesgo;
        this.promedioRiesgo = promedioRiesgo;
    }

    public String getSector() {
        return sector;
    }

    public String getMejorProveedor() {
        return mejorProveedor;
    }

    public Double getMejorPromedio() {
        return mejorPromedio;
    }

    public String getProveedorEnRiesgo() {
        return proveedorEnRiesgo;
    }

    public Double getPromedioRiesgo() {
        return promedioRiesgo;
    }
}