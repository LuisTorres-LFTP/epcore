package com.epcore.entity.notificacion;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bandeja de salida de correo. El correo se guarda primero aquí y un proceso
 * aparte lo envía con reintentos limitados; así un fallo nunca pierde el aviso
 * ni repite el envío.
 */
@Entity
@Table(name = "correos_salientes",
        uniqueConstraints = @UniqueConstraint(name = "uk_correo_idempotencia", columnNames = "clave_idempotencia"),
        indexes = @Index(name = "ix_correo_pendientes", columnList = "estado, proximo_intento"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorreoSaliente {

    public enum Estado { PENDIENTE, ENVIADO, FALLIDO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_correo")
    private Long idCorreo;

    /** Evento + destinatario: impide enviar dos veces el mismo aviso. */
    @Column(name = "clave_idempotencia", nullable = false, length = 200)
    private String claveIdempotencia;

    @Column(name = "destinatario", nullable = false, length = 120)
    private String destinatario;

    @Column(name = "asunto", nullable = false, length = 200)
    private String asunto;

    @Lob
    @Column(name = "cuerpo_html", nullable = false, columnDefinition = "TEXT")
    private String cuerpoHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private Estado estado;

    @Column(name = "intentos", nullable = false)
    private Integer intentos;

    @Column(name = "proximo_intento")
    private LocalDateTime proximoIntento;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;
}
