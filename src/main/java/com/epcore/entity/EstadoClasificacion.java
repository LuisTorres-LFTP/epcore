package com.epcore.entity;

public enum EstadoClasificacion {
    /** El sistema verificó el CIIU y asignó los nichos sin intervención humana. */
    AUTOMATICA,
    /** Hay una excepción: un analista debe resolverla con justificación. */
    EN_REVISION,
    /** Resuelta por una persona; queda registrado quién y por qué. */
    MANUAL,
    /** El proveedor no puede clasificarse con la información aportada. */
    RECHAZADA
}
