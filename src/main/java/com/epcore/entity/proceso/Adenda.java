package com.epcore.entity.proceso;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Modificación numerada y justificada de los términos de un proceso publicado. */
@Entity
@Table(name = "adendas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_proceso", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adenda")
    private Long idAdenda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proceso", nullable = false)
    private Proceso proceso;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "justificacion", nullable = false, length = 1000)
    private String justificacion;

    /** Lista legible de cambios, uno por línea. */
    @Column(name = "cambios", nullable = false, length = 3000)
    private String cambios;

    @Column(name = "emitida_por", nullable = false, length = 120)
    private String emitidaPor;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;
}
