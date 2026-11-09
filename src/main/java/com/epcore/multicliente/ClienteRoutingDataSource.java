package com.epcore.multicliente;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Enruta cada conexión a la base de datos del cliente de la petición en curso.
 * Si el cliente no existe, falla en lugar de usar otra base (nunca mezcla datos).
 */
public class ClienteRoutingDataSource extends AbstractRoutingDataSource {

    public ClienteRoutingDataSource() {
        setLenientFallback(false);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return ClienteContexto.actual();
    }
}
