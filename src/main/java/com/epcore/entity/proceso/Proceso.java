package com.epcore.entity.proceso;

import com.epcore.entity.Nicho;
import com.epcore.entity.Proveedor;
import com.epcore.entity.proceso.ProcesoEnums.EstadoProceso;
import com.epcore.entity.proceso.ProcesoEnums.Modalidad;
import com.epcore.entity.proceso.ProcesoEnums.Visibilidad;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Proceso de contratación. Un solo modelo para las tres modalidades:
 * lo que cambia entre ellas son las reglas (ver ReglasModalidad).
 */
@Entity
@Table(name = "procesos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proceso")
    private Long idProceso;

    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 300)
    private String nombre;

    @Column(name = "entidad_contratante", nullable = false, length = 150)
    private String entidadContratante;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false, length = 20)
    private Modalidad modalidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibilidad", nullable = false, length = 20)
    private Visibilidad visibilidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nicho")
    private Nicho nicho;

    @Column(name = "presupuesto", nullable = false, precision = 19, scale = 2)
    private BigDecimal presupuesto;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoProceso estado;

    @Embedded
    private RequisitosProceso requisitos;

    @Embedded
    private FactoresPuntaje factores;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "proceso_invitados",
            joinColumns = @JoinColumn(name = "id_proceso"),
            inverseJoinColumns = @JoinColumn(name = "id_proveedor"))
    private Set<Proveedor> invitados = new HashSet<>();

    // Trazabilidad del ciclo de vida
    @Column(name = "creado_por", nullable = false, length = 120)
    private String creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "aprobado_nivel1_por", length = 120)
    private String aprobadoNivel1Por;

    @Column(name = "fecha_aprobacion_nivel1")
    private LocalDateTime fechaAprobacionNivel1;

    @Column(name = "aprobado_nivel2_por", length = 120)
    private String aprobadoNivel2Por;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor_adjudicado")
    private Proveedor adjudicadoA;

    @Column(name = "adjudicado_por", length = 120)
    private String adjudicadoPor;

    @Column(name = "fecha_adjudicacion")
    private LocalDateTime fechaAdjudicacion;

    @Column(name = "observacion_cierre", length = 1000)
    private String observacionCierre;
}
