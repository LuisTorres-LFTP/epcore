package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Nombre publicado en una lista restrictiva oficial (OFAC, ONU), para cruzar con los proveedores. */
@Entity
@Table(name = "listas_restrictivas_entradas", indexes = {
        @Index(name = "ix_lista_nombre", columnList = "nombre_normalizado"),
        @Index(name = "ix_lista_tipo", columnList = "lista")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntradaListaRestrictiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrada")
    private Long idEntrada;

    @Enumerated(EnumType.STRING)
    @Column(name = "lista", nullable = false, length = 30)
    private ListaRestrictiva lista;

    @Column(name = "nombre", nullable = false, length = 300)
    private String nombre;

    @Column(name = "nombre_normalizado", nullable = false, length = 300)
    private String nombreNormalizado;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;
}
