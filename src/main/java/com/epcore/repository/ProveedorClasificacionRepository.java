package com.epcore.repository;

import com.epcore.entity.EstadoClasificacion;
import com.epcore.entity.ProveedorClasificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProveedorClasificacionRepository extends JpaRepository<ProveedorClasificacion, Long> {

    Optional<ProveedorClasificacion> findByProveedorIdProveedor(Long idProveedor);

    List<ProveedorClasificacion> findByEstadoOrderByFechaClasificacionAsc(EstadoClasificacion estado);
}
