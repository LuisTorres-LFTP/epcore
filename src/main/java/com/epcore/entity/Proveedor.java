package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Long idProveedor;

    @Column(name = "tipo_persona", nullable = false, length = 30)
    private String tipoPersona;

    @Column(nullable = false, unique = true, length = 30)
    private String nit;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 150)
    private String nombreComercial;

    @Column(name = "representante_legal", length = 120)
    private String representanteLegal;

    @Column(nullable = false, length = 120)
    private String correo;

    @Column(length = 30)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @Column(length = 80)
    private String ciudad;

    @Column(length = 80)
    private String departamento;

    @Column(length = 80)
    private String pais;

    @Column(name = "sector_economico", length = 120)
    private String sectorEconomico;

    @Column(name = "actividad_economica", length = 150)
    private String actividadEconomica;

    @Column(name = "tamano_empresa", length = 50)
    private String tamanoEmpresa;

    @Column(name = "autorizacion_datos", nullable = false)
    private Boolean autorizacionDatos;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();

        if (this.estado == null) {
            this.estado = "ACTIVO";
        }

        if (this.autorizacionDatos == null) {
            this.autorizacionDatos = false;
        }
    }
}