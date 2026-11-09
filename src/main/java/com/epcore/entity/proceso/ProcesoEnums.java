package com.epcore.entity.proceso;

/** Tipos enumerados del módulo de procesos de contratación. */
public final class ProcesoEnums {

    private ProcesoEnums() {
    }

    /** Modalidad del proceso: define plantilla de requisitos y reglas de puntaje. */
    public enum Modalidad {
        /** Solicitud de propuestas de una entidad financiera (SARLAFT, SARO, ciberseguridad). */
        FINANCIERA,
        /** Convocatoria entre empresas privadas. */
        PRIVADA,
        /** Licitación pública: el proceso oficial se publica en SECOP II. */
        PUBLICA
    }

    public enum EstadoProceso {
        BORRADOR, EN_APROBACION, PUBLICADO, EN_EVALUACION, ADJUDICADO, DESIERTO
    }

    /** Quién puede ver el proceso en el portal de proveedores. */
    public enum Visibilidad {
        INVITACION, ABIERTO, SECOP
    }

    public enum CanalOferta {
        PORTAL, INTERNO
    }
}
