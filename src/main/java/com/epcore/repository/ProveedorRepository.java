package com.epcore.repository;

import com.epcore.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByNit(String nit);

    @Query("""
           SELECT DISTINCT p.sectorEconomico
           FROM Proveedor p
           WHERE p.sectorEconomico IS NOT NULL
           AND p.sectorEconomico <> ''
           ORDER BY p.sectorEconomico ASC
           """)
    List<String> listarSectoresEconomicos();
}