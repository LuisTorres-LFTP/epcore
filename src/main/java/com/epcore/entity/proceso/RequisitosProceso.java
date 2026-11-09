package com.epcore.entity.proceso;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

/** Requisitos habilitantes del proceso: se cumplen o no; no dan puntos. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RequisitosProceso {

    @Column(name = "req_liquidez_minima", precision = 8, scale = 2)
    private BigDecimal liquidezMinima;

    /** Nivel de endeudamiento máximo entre 0 y 1 (0,70 = 70 %). */
    @Column(name = "req_endeudamiento_maximo", precision = 5, scale = 4)
    private BigDecimal endeudamientoMaximo;

    @Column(name = "req_cobertura_minima", precision = 8, scale = 2)
    private BigDecimal coberturaInteresesMinima;

    /** Patrimonio mínimo como % del presupuesto (0 = no se exige). */
    @Column(name = "req_patrimonio_pct")
    private Integer patrimonioMinimoPct;

    @Column(name = "req_contratos_minimos")
    private Integer contratosMinimos;

    /** Experiencia acumulada mínima como % del presupuesto. */
    @Column(name = "req_experiencia_pct")
    private Integer experienciaMinimaPct;

    /** Códigos UNSPSC exigidos, separados por coma (vacío = cualquiera). */
    @Column(name = "req_codigos_unspsc", length = 300)
    private String codigosUnspsc;

    @Column(name = "req_exigir_sarlaft")
    private Boolean exigirSarlaft;

    /** Exige que todos los documentos obligatorios del proveedor estén aprobados. */
    @Column(name = "req_exigir_documentos")
    private Boolean exigirDocumentosAprobados;
}
