package com.epcore.repository;

import com.epcore.entity.EvaluacionDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluacionDetalleRepository extends JpaRepository<EvaluacionDetalle, Long> {

    void deleteByEvaluacionIdEvaluacion(Long idEvaluacion);

    List<EvaluacionDetalle> findByEvaluacionIdEvaluacion(Long idEvaluacion);
}