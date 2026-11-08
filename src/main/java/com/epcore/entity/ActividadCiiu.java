package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Catálogo de actividades económicas CIIU (DANE). El código es de 4 dígitos.
 * La versión permite convivir con futuras actualizaciones de la clasificación
 * sin dañar las clasificaciones ya hechas.
 */
@Entity
@Table(name = "catalogo_ciiu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActividadCiiu {

    public static final String VERSION_VIGENTE = "CIIU Rev. 4 A.C.";

    @Id
    @Column(name = "codigo", length = 4)
    private String codigo;

    @Column(name = "descripcion", nullable = false, length = 300)
    private String descripcion;

    @Column(name = "version", nullable = false, length = 30)
    private String version;

    /** Actividad tan amplia que no dice qué vende el proveedor (p. ej. 4690, 8299). */
    @Column(name = "generica", nullable = false)
    private Boolean generica;

    /** Requisito adicional exigido por ley para esta actividad, si lo hay. */
    @Column(name = "requisito_regulatorio", length = 300)
    private String requisitoRegulatorio;

    /** Actividad con restricciones legales que debe revisar el área jurídica. */
    @Column(name = "requiere_revision_legal", nullable = false)
    private Boolean requiereRevisionLegal;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @PrePersist
    public void prePersist() {
        if (version == null) {
            version = VERSION_VIGENTE;
        }
        if (generica == null) {
            generica = false;
        }
        if (requiereRevisionLegal == null) {
            requiereRevisionLegal = false;
        }
        if (activo == null) {
            activo = true;
        }
    }
}
