package com.epcore.service;

import com.epcore.entity.ContratoExperiencia;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ContratoExperienciaService {

    List<ContratoExperiencia> listarPorProveedor(Long idProveedor);

    Optional<ContratoExperiencia> buscarPorId(Long idContrato);

    ContratoExperiencia guardar(ContratoExperiencia contrato);

    void eliminar(Long idContrato);

    /**
     * Contratos que acreditan experiencia para un código UNSPSC
     * (si el código es nulo, todos). Con soloCertificados = true
     * se excluyen los que no tienen soporte.
     */
    List<ContratoExperiencia> listarAcreditables(
            Long idProveedor,
            String codigoUnspsc,
            boolean soloCertificados
    );

    /** Suma del valor acreditable (valor x % de participación). */
    BigDecimal sumarExperienciaAcreditable(
            Long idProveedor,
            String codigoUnspsc,
            boolean soloCertificados
    );
}
