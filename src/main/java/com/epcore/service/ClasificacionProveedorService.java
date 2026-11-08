package com.epcore.service;

import com.epcore.entity.ProveedorClasificacion;
import com.epcore.entity.TipoProveedor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ClasificacionProveedorService {

    /**
     * Clasifica (o reclasifica) al proveedor con sus actividades declaradas.
     *
     * @param codigosOficiales CIIU leídos del RUT o de la Cámara de Comercio;
     *                         null si el documento aún no se ha cargado.
     */
    ProveedorClasificacion clasificar(
            Long idProveedor,
            TipoProveedor tipo,
            List<ClasificadorProveedor.ActividadDeclarada> actividades,
            Set<String> codigosOficiales
    );

    Optional<ProveedorClasificacion> obtener(Long idProveedor);

    /** Bandeja de excepciones pendientes, la más antigua primero. */
    List<ProveedorClasificacion> listarEnRevision();

    /** Un analista resuelve la excepción asignando nichos con justificación obligatoria. */
    ProveedorClasificacion resolverManualmente(
            Long idProveedor,
            Set<Long> idsNichos,
            String justificacion,
            String usuario
    );

    ProveedorClasificacion rechazar(Long idProveedor, String justificacion, String usuario);
}
