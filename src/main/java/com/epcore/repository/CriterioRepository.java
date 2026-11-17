package com.epcore.repository;

import com.epcore.entity.Criterio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CriterioRepository extends JpaRepository<Criterio, Long> {

    List<Criterio> findByActivoTrue();
}