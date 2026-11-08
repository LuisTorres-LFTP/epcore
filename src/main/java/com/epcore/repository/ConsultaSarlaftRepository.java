package com.epcore.repository;

import com.epcore.entity.ConsultaSarlaft;
import com.epcore.entity.ListaRestrictiva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultaSarlaftRepository
        extends JpaRepository<ConsultaSarlaft, Long> {

    List<ConsultaSarlaft> findByProveedorIdProveedorOrderByFechaConsultaDesc(
            Long idProveedor
    );

    Optional<ConsultaSarlaft> findFirstByProveedorIdProveedorAndListaOrderByFechaConsultaDesc(
            Long idProveedor,
            ListaRestrictiva lista
    );
}
