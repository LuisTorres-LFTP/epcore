package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

/** Actividad CIIU declarada por el proveedor y si se verificó en el documento oficial. */
@Entity
@Table(name = "proveedor_actividades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad")
    private Long idActividad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "codigo_ciiu", nullable = false, length = 4)
    private String codigoCiiu;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoActividad tipo;

    @Column(name = "verificada", nullable = false)
    private Boolean verificada;
}
