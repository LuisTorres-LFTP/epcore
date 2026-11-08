package com.epcore.repository;

import com.epcore.entity.IntegrantePluralidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntegrantePluralidadRepository extends JpaRepository<IntegrantePluralidad, Long> {

    List<IntegrantePluralidad> findByPluralidadIdProveedor(Long idPluralidad);
}
