package com.epcore.service.impl;

import com.epcore.entity.EvaluacionDetalle;
import com.epcore.repository.EvaluacionDetalleRepository;
import com.epcore.service.EvaluacionDetalleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EvaluacionDetalleServiceImpl implements EvaluacionDetalleService {

    private final EvaluacionDetalleRepository evaluacionDetalleRepository;

    public EvaluacionDetalleServiceImpl(EvaluacionDetalleRepository evaluacionDetalleRepository) {
        this.evaluacionDetalleRepository = evaluacionDetalleRepository;
    }

    @Override
    public List<EvaluacionDetalle> listarTodos() {
        return evaluacionDetalleRepository.findAll();
    }

    @Override
    public EvaluacionDetalle guardar(EvaluacionDetalle detalle) {
        return evaluacionDetalleRepository.save(detalle);
    }

    @Override
    @Transactional
    public void eliminarPorEvaluacion(Long idEvaluacion) {
        evaluacionDetalleRepository.deleteByEvaluacionIdEvaluacion(idEvaluacion);
    }

    @Override
    public List<EvaluacionDetalle> listarPorEvaluacion(Long idEvaluacion) {
        return evaluacionDetalleRepository.findByEvaluacionIdEvaluacion(idEvaluacion);
    }
}