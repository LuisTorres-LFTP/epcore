package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proveedor_documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long idDocumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "tipo_documento", nullable = false, length = 100)
    private String tipoDocumento;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    @Column(name = "fecha_expedicion")
    private LocalDate fechaExpedicion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "tiene_vencimiento", nullable = false)
    private Boolean tieneVencimiento;

    @Column(name = "obligatorio", nullable = false)
    private Boolean obligatorio;

    @Column(name = "estado_validacion", nullable = false, length = 30)
    private String estadoValidacion;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    // ---------- Trazabilidad de la validación y la aprobación dual ----------

    /** Usuario que cargó el documento (no puede aprobarlo). */
    @Column(name = "cargado_por", length = 120)
    private String cargadoPor;

    /** true si el sistema verificó NIT y fechas y lo aprobó sin intervención humana. */
    @Column(name = "validacion_automatica")
    private Boolean validacionAutomatica;

    /** Resultado de la lectura automática (qué se verificó o por qué no se pudo). */
    @Column(name = "detalle_validacion", length = 1000)
    private String detalleValidacion;

    @Column(name = "aprobado_nivel1_por", length = 120)
    private String aprobadoNivel1Por;

    @Column(name = "fecha_aprobacion_nivel1")
    private LocalDateTime fechaAprobacionNivel1;

    @Column(name = "aprobado_nivel2_por", length = 120)
    private String aprobadoNivel2Por;

    @Column(name = "fecha_aprobacion_nivel2")
    private LocalDateTime fechaAprobacionNivel2;

    @Column(name = "rechazado_por", length = 120)
    private String rechazadoPor;

    @Column(name = "fecha_rechazo")
    private LocalDateTime fechaRechazo;

    @Column(name = "motivo_rechazo", length = 1000)
    private String motivoRechazo;

    @PrePersist
    public void prePersist() {
        if (fechaCarga == null) {
            fechaCarga = LocalDateTime.now();
        }

        if (tieneVencimiento == null) {
            tieneVencimiento = false;
        }

        if (obligatorio == null) {
            obligatorio = true;
        }

        if (estadoValidacion == null || estadoValidacion.isBlank()) {
            estadoValidacion = "PENDIENTE";
        }

        if (activo == null) {
            activo = true;
        }
    }
}