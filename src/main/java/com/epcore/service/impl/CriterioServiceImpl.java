package com.epcore.service.impl;

import com.epcore.entity.Criterio;
import com.epcore.repository.CriterioRepository;
import com.epcore.service.CriterioService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CriterioServiceImpl implements CriterioService {

    private final CriterioRepository criterioRepository;

    public CriterioServiceImpl(CriterioRepository criterioRepository) {
        this.criterioRepository = criterioRepository;
    }

    @Override
    public List<Criterio> listarTodos() {
        return criterioRepository.findAll();
    }

    @Override
    public List<Criterio> listarActivos() {
        return criterioRepository.findByActivoTrue();
    }

    @Override
    public int sumaPesosActivos() {
        return listarActivos().stream()
                .mapToInt(Criterio::getPeso)
                .sum();
    }

    @Override
    public Optional<Criterio> buscarPorId(Long id) {
        return criterioRepository.findById(id);
    }

    @Override
    public Criterio guardar(Criterio criterio) {
        if (criterio.getNombreCriterio() == null || criterio.getNombreCriterio().isBlank()) {
            throw new IllegalArgumentException("El nombre del criterio es obligatorio.");
        }
        if (criterio.getPeso() == null || criterio.getPeso() < 1 || criterio.getPeso() > 100) {
            throw new IllegalArgumentException("El peso debe estar entre 1 y 100.");
        }
        return criterioRepository.save(criterio);
    }

    @Override
    public void eliminar(Long id) {
        criterioRepository.deleteById(id);
    }
}