package com.epcore.service;

import com.epcore.entity.Criterio;

import java.util.List;
import java.util.Optional;

public interface CriterioService {

    List<Criterio> listarTodos();

    Optional<Criterio> buscarPorId(Long id);

    Criterio guardar(Criterio criterio);

    void eliminar(Long id);
}