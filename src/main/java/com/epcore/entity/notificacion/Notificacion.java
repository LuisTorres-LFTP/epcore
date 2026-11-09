package com.epcore.entity.notificacion;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Aviso dentro de EpCore para un usuario. Siempre se crea, aunque el correo falle. */
@Entity
@Table(name = "notificaciones", indexes = @Index(name = "ix_notif_destinatario", columnList = "destinatario, leida"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    /** Correo del usuario destinatario. */
    @Column(name = "destinatario", nullable = false, length = 120)
    private String destinatario;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "mensaje", nullable = false, length = 1000)
    private String mensaje;

    /** Ruta interna a la que lleva el aviso (p. ej. /portal/procesos/7). */
    @Column(name = "enlace", length = 300)
    private String enlace;

    @Column(name = "leida", nullable = false)
    private Boolean leida;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
