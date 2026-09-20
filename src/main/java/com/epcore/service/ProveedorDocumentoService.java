package com.epcore.service;

import com.epcore.entity.ProveedorDocumento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProveedorDocumentoService {

    List<ProveedorDocumento> listarTodos();

    List<ProveedorDocumento> listarPorProveedor(Long idProveedor);

    List<ProveedorDocumento> listarActivosPorProveedor(Long idProveedor);

    Optional<ProveedorDocumento> buscarPorId(Long idDocumento);

    ProveedorDocumento guardar(ProveedorDocumento documento);

    void eliminar(Long idDocumento);

    void inactivar(Long idDocumento);

    List<ProveedorDocumento> listarDocumentosVencidos();

    List<ProveedorDocumento> listarDocumentosProximosAVencer(int dias);

    Long contarDocumentosObligatorios(Long idProveedor);

    Long contarDocumentosObligatoriosAprobados(Long idProveedor);

    Double calcularPorcentajeCumplimiento(Long idProveedor);

    List<ProveedorDocumento> listarPorProveedorYEstado(
            Long idProveedor,
            String estado
    );

    boolean estaVencido(ProveedorDocumento documento);

    boolean estaProximoAVencer(
            ProveedorDocumento documento,
            int dias
    );

    void actualizarEstadosPorVencimiento();
}