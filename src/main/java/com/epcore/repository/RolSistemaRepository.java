package com.epcore.repository;

import com.epcore.entity.RolSistema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolSistemaRepository extends JpaRepository<RolSistema, Long> {

    Optional<RolSistema> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<RolSistema> findAllByOrderByPredefinidoDescNombreAsc();
}
