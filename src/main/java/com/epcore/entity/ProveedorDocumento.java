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