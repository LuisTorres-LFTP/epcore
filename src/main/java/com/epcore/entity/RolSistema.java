package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Rol configurable (perfil): un nombre y un conjunto de permisos.
 * Los predefinidos se crean al arrancar; el cliente puede ajustar sus
 * permisos o crear roles propios, siempre respetando la separación de funciones.
 */
@Entity
@Table(name = "roles_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long idRol;

    /** Identificador estable en mayúsculas, p. ej. DIRECTOR_COMPRAS o JEFE_COMPRAS_REGIONAL. */
    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rol_permisos", joinColumns = @JoinColumn(name = "id_rol"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permiso", length = 40)
    private Set<Permiso> permisos = new HashSet<>();

    /**
     * Monto máximo (COP) que este rol puede aprobar o adjudicar.
     * Null = sin límite. Por encima del límite se requiere un rol con mayor atribución.
     */
    @Column(name = "limite_aprobacion", precision = 19, scale = 2)
    private BigDecimal limiteAprobacion;

    /** Los roles predefinidos no se pueden eliminar ni cambiar de código. */
    @Column(name = "predefinido", nullable = false)
    private Boolean predefinido;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "modificado_por", length = 120)
    private String modificadoPor;

    @PrePersist
    public void prePersist() {
        if (predefinido == null) {
            predefinido = false;
        }
        if (activo == null) {
            activo = true;
        }
    }

    public Set<Permiso> permisosComoEnumSet() {
        return permisos == null || permisos.isEmpty() ? EnumSet.noneOf(Permiso.class) : EnumSet.copyOf(permisos);
    }

    public boolean puedeAprobarMonto(BigDecimal monto) {
        return limiteAprobacion == null || (monto != null && monto.compareTo(limiteAprobacion) <= 0);
    }
}
