package com.epcore.repository;

import com.epcore.entity.ContratoExperiencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContratoExperienciaRepository
        extends JpaRepository<ContratoExperiencia, Long> {

    List<ContratoExperiencia> findByProveedorIdProveedorOrderByFechaInicioDesc(
            Long idProveedor
    );
}
