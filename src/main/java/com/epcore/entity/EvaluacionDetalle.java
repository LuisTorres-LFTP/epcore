package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evaluacion_detalles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    @ManyToOne
    @JoinColumn(name = "id_evaluacion", nullable = false)
    private Evaluacion evaluacion;

    @ManyToOne
    @JoinColumn(name = "id_criterio", nullable = false)
    private Criterio criterio;

    @Column(nullable = false)
    private Double calificacion;

    @Column(nullable = false)
    private Integer peso;

    @Column(name = "resultado_ponderado", nullable = false)
    private Double resultadoPonderado;

    @PrePersist
    @PreUpdate
    public void calcularPonderado() {
        if (calificacion != null && peso != null) {
            this.resultadoPonderado = (calificacion * peso) / 100;
        }
    }
}