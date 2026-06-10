package com.epcore.service;

import com.epcore.entity.EvaluacionDetalle;

import java.util.List;

public interface EvaluacionDetalleService {

    List<EvaluacionDetalle> listarTodos();

    EvaluacionDetalle guardar(EvaluacionDetalle detalle);

    void eliminarPorEvaluacion(Long idEvaluacion);

    List<EvaluacionDetalle> listarPorEvaluacion(Long idEvaluacion);
}