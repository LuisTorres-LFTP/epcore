package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Estados financieros de un proveedor con corte a un año fiscal.
 * Sirve para calcular los indicadores de capacidad financiera que se
 * exigen como requisito habilitante en procesos privados y públicos
 * (liquidez, endeudamiento, cobertura de intereses y rentabilidad).
 */
@Entity
@Table(
        name = "proveedor_informacion_financiera",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_info_financiera_proveedor_anio",
                columnNames = {"id_proveedor", "anio_corte"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformacionFinanciera {

    private static final int ESCALA = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_info_financiera")
    private Long idInfoFinanciera;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "anio_corte", nullable = false)
    private Integer anioCorte;

    @Column(name = "fecha_corte", nullable = false)
    private LocalDate fechaCorte;

    @Column(name = "activo_corriente", nullable = false, precision = 19, scale = 2)
    private BigDecimal activoCorriente;

    @Column(name = "pasivo_corriente", nullable = false, precision = 19, scale = 2)
    private BigDecimal pasivoCorriente;

    @Column(name = "activo_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal activoTotal;

    @Column(name = "pasivo_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal pasivoTotal;

    @Column(name = "patrimonio", nullable = false, precision = 19, scale = 2)
    private BigDecimal patrimonio;

    @Column(name = "ingresos_operacionales", precision = 19, scale = 2)
    private BigDecimal ingresosOperacionales;

    @Column(name = "utilidad_operacional", precision = 19, scale = 2)
    private BigDecimal utilidadOperacional;

    @Column(name = "gastos_intereses", precision = 19, scale = 2)
    private BigDecimal gastosIntereses;

    @Column(name = "utilidad_neta", precision = 19, scale = 2)
    private BigDecimal utilidadNeta;

    /** Origen de la información: ESTADOS_FINANCIEROS, RUP o DECLARACION_RENTA. */
    @Column(name = "fuente", nullable = false, length = 40)
    private String fuente;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }

        if (fuente == null || fuente.isBlank()) {
            fuente = "ESTADOS_FINANCIEROS";
        }
    }

    /** Índice de liquidez = activo corriente / pasivo corriente. */
    @Transient
    public BigDecimal getIndiceLiquidez() {
        return dividir(activoCorriente, pasivoCorriente);
    }

    /** Nivel de endeudamiento = pasivo total / activo total (0 a 1). */
    @Transient
    public BigDecimal getNivelEndeudamiento() {
        return dividir(pasivoTotal, activoTotal);
    }

    /** Razón de cobertura de intereses = utilidad operacional / gastos de intereses. */
    @Transient
    public BigDecimal getRazonCoberturaIntereses() {
        return dividir(utilidadOperacional, gastosIntereses);
    }

    /** Rentabilidad del patrimonio = utilidad operacional / patrimonio. */
    @Transient
    public BigDecimal getRentabilidadPatrimonio() {
        return dividir(utilidadOperacional, patrimonio);
    }

    /** Rentabilidad del activo = utilidad operacional / activo total. */
    @Transient
    public BigDecimal getRentabilidadActivo() {
        return dividir(utilidadOperacional, activoTotal);
    }

    /**
     * Devuelve null cuando el divisor es cero o falta un dato; en
     * contratación eso se interpreta como "indeterminado" (por ejemplo,
     * una empresa sin pasivo corriente o sin gastos de intereses).
     */
    private static BigDecimal dividir(BigDecimal dividendo, BigDecimal divisor) {
        if (dividendo == null || divisor == null || divisor.signum() == 0) {
            return null;
        }

        return dividendo.divide(divisor, ESCALA, RoundingMode.HALF_UP);
    }
}
