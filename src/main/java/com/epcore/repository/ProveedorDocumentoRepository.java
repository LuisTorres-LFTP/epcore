package com.epcore.repository;

import com.epcore.entity.ProveedorDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProveedorDocumentoRepository
        extends JpaRepository<ProveedorDocumento, Long> {

    List<ProveedorDocumento> findByProveedorIdProveedorOrderByFechaCargaDesc(
            Long idProveedor
    );

    List<ProveedorDocumento> findByProveedorIdProveedorAndActivoTrueOrderByFechaCargaDesc(
            Long idProveedor
    );

    @Query("""
           SELECT d
           FROM ProveedorDocumento d
           WHERE d.activo = true
             AND d.tieneVencimiento = true
             AND d.fechaVencimiento IS NOT NULL
             AND d.fechaVencimiento < :fechaActual
           ORDER BY d.fechaVencimiento ASC
           """)
    List<ProveedorDocumento> listarDocumentosVencidos(
            @Param("fechaActual") LocalDate fechaActual
    );

    @Query("""
           SELECT d
           FROM ProveedorDocumento d
           WHERE d.activo = true
             AND d.tieneVencimiento = true
             AND d.fechaVencimiento IS NOT NULL
             AND d.fechaVencimiento BETWEEN :fechaActual AND :fechaLimite
           ORDER BY d.fechaVencimiento ASC
           """)
    List<ProveedorDocumento> listarDocumentosProximosAVencer(
            @Param("fechaActual") LocalDate fechaActual,
            @Param("fechaLimite") LocalDate fechaLimite
    );

    @Query("""
           SELECT COUNT(d)
           FROM ProveedorDocumento d
           WHERE d.proveedor.idProveedor = :idProveedor
             AND d.activo = true
             AND d.obligatorio = true
           """)
    Long contarDocumentosObligatorios(
            @Param("idProveedor") Long idProveedor
    );

    @Query("""
           SELECT COUNT(d)
           FROM ProveedorDocumento d
           WHERE d.proveedor.idProveedor = :idProveedor
             AND d.activo = true
             AND d.obligatorio = true
             AND d.estadoValidacion = 'APROBADO'
           """)
    Long contarDocumentosObligatoriosAprobados(
            @Param("idProveedor") Long idProveedor
    );

    @Query("""
           SELECT d
           FROM ProveedorDocumento d
           WHERE d.proveedor.idProveedor = :idProveedor
             AND d.activo = true
             AND d.estadoValidacion = :estado
           ORDER BY d.fechaCarga DESC
           """)
    List<ProveedorDocumento> listarPorProveedorYEstado(
            @Param("idProveedor") Long idProveedor,
            @Param("estado") String estado
    );
}