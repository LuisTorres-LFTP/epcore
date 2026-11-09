package com.epcore.repository.proceso;

import com.epcore.entity.proceso.Oferta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    List<Oferta> findByProcesoIdProceso(Long idProceso);

    Optional<Oferta> findByProcesoIdProcesoAndProveedorIdProveedor(Long idProceso, Long idProveedor);
}
