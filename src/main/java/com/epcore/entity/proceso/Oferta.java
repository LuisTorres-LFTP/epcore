package com.epcore.entity.proceso;

import com.epcore.entity.Proveedor;
import com.epcore.entity.proceso.ProcesoEnums.CanalOferta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Oferta de un proveedor en un proceso. Es confidencial frente a los demás proponentes. */
@Entity
@Table(name = "ofertas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_proceso", "id_proveedor"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_oferta")
    private Long idOferta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proceso", nullable = false)
    private Proceso proceso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "plazo_dias", nullable = false)
    private Integer plazoDias;

    @Column(name = "industria_nacional", nullable = false)
    private Boolean industriaNacional;

    /** Calificación técnica del comité, de 0 a 100 (null = pendiente). */
    @Column(name = "calificacion_tecnica")
    private Integer calificacionTecnica;

    @Column(name = "calificado_por", length = 120)
    private String calificadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 10)
    private CanalOferta canal;

    /** Declaración juramentada de ausencia de conflicto de interés. */
    @Column(name = "declaracion_conflicto", nullable = false)
    private Boolean declaracionConflicto;

    @Column(name = "registrada_por", nullable = false, length = 120)
    private String registradaPor;

    @Column(name = "fecha_presentacion", nullable = false)
    private LocalDateTime fechaPresentacion;
}
