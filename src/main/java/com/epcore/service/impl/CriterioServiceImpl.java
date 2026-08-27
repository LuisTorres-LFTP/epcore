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
    public Optional<Criterio> buscarPorId(Long id) {
        return criterioRepository.findById(id);
    }

    @Override
    public Criterio guardar(Criterio criterio) {
        return criterioRepository.save(criterio);
    }

    @Override
    public void eliminar(Long id) {
        criterioRepository.deleteById(id);
    }
}