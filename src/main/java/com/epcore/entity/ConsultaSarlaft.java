package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de una consulta en listas restrictivas o de antecedentes
 * (SARLAFT). Cada consulta queda como evidencia auditable: qué lista,
 * a quién se consultó, el resultado y quién la hizo.
 */
@Entity
@Table(name = "proveedor_consultas_sarlaft")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaSarlaft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consulta")
    private Long idConsulta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "lista", nullable = false, length = 30)
    private ListaRestrictiva lista;

    /** NIT de la empresa o cédula del representante legal / accionista consultado. */
    @Column(name = "documento_consultado", nullable = false, length = 30)
    private String documentoConsultado;

    @Column(name = "nombre_consultado", nullable = false, length = 150)
    private String nombreConsultado;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 30)
    private ResultadoSarlaft resultado;

    /** Número de certificado o referencia que entrega la fuente consultada. */
    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Column(name = "usuario_responsable", nullable = false, length = 120)
    private String usuarioResponsable;

    @Column(name = "fecha_consulta", nullable = false)
    private LocalDateTime fechaConsulta;

    @PrePersist
    public void prePersist() {
        if (fechaConsulta == null) {
            fechaConsulta = LocalDateTime.now();
        }
    }
}
