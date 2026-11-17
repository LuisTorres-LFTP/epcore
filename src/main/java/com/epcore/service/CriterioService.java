package com.epcore.service;

import com.epcore.entity.Criterio;

import java.util.List;
import java.util.Optional;

public interface CriterioService {

    /** Los pesos de los criterios activos deben sumar este valor para poder evaluar. */
    int SUMA_PESOS_REQUERIDA = 100;

    List<Criterio> listarTodos();

    List<Criterio> listarActivos();

    int sumaPesosActivos();

    Optional<Criterio> buscarPorId(Long id);

    Criterio guardar(Criterio criterio);

    void eliminar(Long id);
}