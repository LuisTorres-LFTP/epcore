package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Integrante de un consorcio o unión temporal, con su porcentaje de participación. */
@Entity
@Table(
        name = "pluralidad_integrantes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_pluralidad", "id_integrante"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrantePluralidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pluralidad", nullable = false)
    private Proveedor pluralidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_integrante", nullable = false)
    private Proveedor integrante;

    @Column(name = "porcentaje_participacion", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeParticipacion;
}
