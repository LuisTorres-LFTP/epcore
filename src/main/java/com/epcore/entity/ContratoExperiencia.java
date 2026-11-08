package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Contrato ejecutado por el proveedor que acredita experiencia.
 * En procesos públicos se valida contra los códigos UNSPSC y el
 * valor exigido en el pliego; en privados, contra lo que defina el cliente.
 */
@Entity
@Table(name = "proveedor_contratos_experiencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratoExperiencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato")
    private Long idContrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "numero_contrato", length = 60)
    private String numeroContrato;

    @Column(name = "contratante", nullable = false, length = 150)
    private String contratante;

    @Column(name = "nit_contratante", length = 30)
    private String nitContratante;

    /** true si el contratante es una entidad estatal. */
    @Column(name = "contratante_publico", nullable = false)
    private Boolean contratantePublico;

    @Column(name = "objeto", nullable = false, length = 1000)
    private String objeto;

    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    /** Porcentaje de participación si se ejecutó en consorcio o unión temporal. */
    @Column(name = "porcentaje_participacion", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeParticipacion;

    /** Códigos UNSPSC separados por coma, por ejemplo: 76111500,47131800. */
    @Column(name = "codigos_unspsc", length = 500)
    private String codigosUnspsc;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_terminacion")
    private LocalDate fechaTerminacion;

    /** EN_EJECUCION, TERMINADO o LIQUIDADO. */
    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    /** Indica si se aportó certificación o acta de liquidación como soporte. */
    @Column(name = "tiene_certificacion", nullable = false)
    private Boolean tieneCertificacion;

    @Column(name = "ruta_soporte", length = 500)
    private String rutaSoporte;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }

        if (porcentajeParticipacion == null) {
            porcentajeParticipacion = BigDecimal.valueOf(100);
        }

        if (contratantePublico == null) {
            contratantePublico = false;
        }

        if (tieneCertificacion == null) {
            tieneCertificacion = false;
        }

        if (estado == null || estado.isBlank()) {
            estado = "TERMINADO";
        }
    }

    /** Valor que realmente acredita el proveedor según su participación. */
    @Transient
    public BigDecimal getValorAcreditable() {
        if (valor == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal participacion = porcentajeParticipacion == null
                ? BigDecimal.valueOf(100)
                : porcentajeParticipacion;

        return valor.multiply(participacion)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transient
    public List<String> getListaCodigosUnspsc() {
        if (codigosUnspsc == null || codigosUnspsc.isBlank()) {
            return List.of();
        }

        return Arrays.stream(codigosUnspsc.split(","))
                .map(String::trim)
                .filter(codigo -> !codigo.isEmpty())
                .toList();
    }

    /**
     * Coincide si alguno de los códigos del contrato empieza por el
     * código buscado (permite buscar por segmento, familia o clase).
     */
    public boolean tieneCodigoUnspsc(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return false;
        }

        String buscado = codigo.trim();

        return getListaCodigosUnspsc().stream()
                .anyMatch(c -> c.startsWith(buscado));
    }
}
