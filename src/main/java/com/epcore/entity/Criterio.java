package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "criterios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Criterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_criterio")
    private Long idCriterio;

    @Column(name = "nombre_criterio", nullable = false, length = 120)
    private String nombreCriterio;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private Integer peso;

    @Column(nullable = false)
    private Boolean activo = true;
}