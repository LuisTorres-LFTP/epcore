package com.epcore.entity;

/**
 * Listas que se consultan en la debida diligencia SARLAFT y de antecedentes.
 */
public enum ListaRestrictiva {
    OFAC("Lista OFAC (Clinton) - EE. UU."),
    ONU("Lista del Consejo de Seguridad de la ONU"),
    PROCURADURIA("Antecedentes disciplinarios - Procuraduría"),
    CONTRALORIA("Responsables fiscales - Contraloría"),
    POLICIA("Antecedentes judiciales - Policía Nacional"),
    PEP("Personas Expuestas Políticamente");

    private final String descripcion;

    ListaRestrictiva(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
