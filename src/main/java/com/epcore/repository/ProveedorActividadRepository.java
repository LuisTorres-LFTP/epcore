package com.epcore.repository;

import com.epcore.entity.ProveedorActividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorActividadRepository extends JpaRepository<ProveedorActividad, Long> {

    List<ProveedorActividad> findByProveedorIdProveedor(Long idProveedor);

    void deleteByProveedorIdProveedor(Long idProveedor);
}
