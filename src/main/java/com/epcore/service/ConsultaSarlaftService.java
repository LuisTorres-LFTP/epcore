package com.epcore.service;

import com.epcore.entity.ConsultaSarlaft;
import com.epcore.entity.EstadoSarlaft;
import com.epcore.entity.ListaRestrictiva;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Las consultas SARLAFT son evidencia de auditoría: se registran
 * pero no se editan ni se eliminan. Una nueva consulta reemplaza
 * a la anterior en el cálculo del estado.
 */
public interface ConsultaSarlaftService {

    /** Días que una consulta se considera vigente. */
    int DIAS_VIGENCIA = 90;

    /** Listas que deben estar consultadas para habilitar a un proveedor. */
    Set<ListaRestrictiva> LISTAS_OBLIGATORIAS = Set.of(
            ListaRestrictiva.OFAC,
            ListaRestrictiva.ONU,
            ListaRestrictiva.PROCURADURIA,
            ListaRestrictiva.CONTRALORIA,
            ListaRestrictiva.POLICIA
    );

    List<ConsultaSarlaft> listarPorProveedor(Long idProveedor);

    Optional<ConsultaSarlaft> obtenerUltimaConsulta(
            Long idProveedor,
            ListaRestrictiva lista
    );

    ConsultaSarlaft registrarConsulta(ConsultaSarlaft consulta);

    EstadoSarlaft calcularEstado(Long idProveedor);

    EstadoSarlaft calcularEstado(Long idProveedor, LocalDateTime fechaReferencia);

    /** true solo si el estado es LIMPIO. */
    boolean estaHabilitado(Long idProveedor);
}
