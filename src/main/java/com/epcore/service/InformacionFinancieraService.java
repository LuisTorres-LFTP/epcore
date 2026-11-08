package com.epcore.service;

import com.epcore.entity.InformacionFinanciera;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InformacionFinancieraService {

    List<InformacionFinanciera> listarPorProveedor(Long idProveedor);

    Optional<InformacionFinanciera> buscarPorId(Long idInfoFinanciera);

    /** Estados financieros del año de corte más reciente. */
    Optional<InformacionFinanciera> obtenerMasReciente(Long idProveedor);

    InformacionFinanciera guardar(InformacionFinanciera informacion);

    void eliminar(Long idInfoFinanciera);

    /**
     * Verifica la capacidad financiera contra los mínimos de un proceso.
     * Los parámetros nulos no se evalúan. Devuelve la lista de
     * incumplimientos; vacía significa que el proveedor cumple.
     */
    List<String> verificarCapacidadFinanciera(
            InformacionFinanciera informacion,
            BigDecimal liquidezMinima,
            BigDecimal endeudamientoMaximo,
            BigDecimal coberturaInteresesMinima,
            BigDecimal patrimonioMinimo
    );
}
