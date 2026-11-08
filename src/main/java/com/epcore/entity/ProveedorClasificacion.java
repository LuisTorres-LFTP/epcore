package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Clasificación vigente de un proveedor: tipo de empresa, nichos asignados,
 * estado y, si hubo excepción, quién la resolvió y con qué justificación.
 */
@Entity
@Table(name = "proveedor_clasificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorClasificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clasificacion")
    private Long idClasificacion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false, unique = true)
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_proveedor", nullable = false, length = 40)
    private TipoProveedor tipoProveedor;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "proveedor_clasificacion_nichos",
            joinColumns = @JoinColumn(name = "id_clasificacion"),
            inverseJoinColumns = @JoinColumn(name = "id_nicho")
    )
    private Set<Nicho> nichos = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoClasificacion estado;

    /** Excepciones detectadas, separadas por " | ". */
    @Column(name = "motivos_revision", length = 2000)
    private String motivosRevision;

    /** Documentos que la ley exige por la actividad (licencias, habilitaciones), separados por " | ". */
    @Column(name = "requisitos_adicionales", length = 1000)
    private String requisitosAdicionales;

    @Column(name = "version_ciiu", nullable = false, length = 30)
    private String versionCiiu;

    @Column(name = "justificacion", length = 1000)
    private String justificacion;

    @Column(name = "resuelto_por", length = 120)
    private String resueltoPor;

    @Column(name = "fecha_clasificacion", nullable = false)
    private LocalDateTime fechaClasificacion;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
}
