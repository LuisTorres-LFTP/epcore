package com.epcore.entity;

/**
 * Tipo de proveedor. Define contra qué documento oficial se verifica la
 * actividad económica (CIIU) y qué excepciones aplican.
 */
public enum TipoProveedor {
    PERSONA_JURIDICA("Persona jurídica (sociedad)", true, true, false),
    PERSONA_NATURAL_COMERCIANTE("Persona natural comerciante", true, true, false),
    /** Profesionales independientes: tienen RUT pero no matrícula mercantil. */
    PERSONA_NATURAL_NO_COMERCIANTE("Persona natural no comerciante (profesional independiente)", true, false, false),
    ENTIDAD_SIN_ANIMO_DE_LUCRO("Entidad sin ánimo de lucro", true, true, false),
    COOPERATIVA("Cooperativa", true, true, false),
    /** No tiene actividad propia: se clasifica por sus integrantes. */
    CONSORCIO("Consorcio", false, false, true),
    UNION_TEMPORAL("Unión temporal", false, false, true),
    /** Sin RUT colombiano: clasificación manual con el código internacional ISIC. */
    EXTRANJERA_SIN_SUCURSAL("Empresa extranjera sin sucursal en Colombia", false, false, false);

    private final String descripcion;
    private final boolean tieneRut;
    private final boolean requiereCamaraComercio;
    private final boolean esPluralidad;

    TipoProveedor(String descripcion, boolean tieneRut, boolean requiereCamaraComercio, boolean esPluralidad) {
        this.descripcion = descripcion;
        this.tieneRut = tieneRut;
        this.requiereCamaraComercio = requiereCamaraComercio;
        this.esPluralidad = esPluralidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isTieneRut() {
        return tieneRut;
    }

    public boolean isRequiereCamaraComercio() {
        return requiereCamaraComercio;
    }

    public boolean isEsPluralidad() {
        return esPluralidad;
    }

    /** Documento contra el que se verifican los códigos CIIU declarados. */
    public String getDocumentoVerificacion() {
        if (requiereCamaraComercio) {
            return "Certificado de Cámara de Comercio";
        }
        return tieneRut ? "RUT" : "Verificación manual";
    }
}
