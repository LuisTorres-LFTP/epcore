package com.epcore.repository;

import com.epcore.entity.InformacionFinanciera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InformacionFinancieraRepository
        extends JpaRepository<InformacionFinanciera, Long> {

    List<InformacionFinanciera> findByProveedorIdProveedorOrderByAnioCorteDesc(
            Long idProveedor
    );

    Optional<InformacionFinanciera> findFirstByProveedorIdProveedorOrderByAnioCorteDesc(
            Long idProveedor
    );

    Optional<InformacionFinanciera> findByProveedorIdProveedorAndAnioCorte(
            Long idProveedor,
            Integer anioCorte
    );
}
