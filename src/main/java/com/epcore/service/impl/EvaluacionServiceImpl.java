package com.epcore.service.impl;

import com.epcore.dto.RankingProveedorDTO;
import com.epcore.dto.RankingSectorDTO;
import com.epcore.entity.Evaluacion;
import com.epcore.repository.EvaluacionRepository;
import com.epcore.service.EvaluacionService;
import com.epcore.dto.IndicadorSectorDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EvaluacionServiceImpl implements EvaluacionService {

    private final EvaluacionRepository evaluacionRepository;

    public EvaluacionServiceImpl(EvaluacionRepository evaluacionRepository) {
        this.evaluacionRepository = evaluacionRepository;
    }

    @Override
    public List<Evaluacion> listarTodas() {
        return evaluacionRepository.findAll();
    }

    @Override
    public Optional<Evaluacion> buscarPorId(Long id) {
        return evaluacionRepository.findById(id);
    }

    @Override
    public Evaluacion guardar(Evaluacion evaluacion) {
        return evaluacionRepository.save(evaluacion);
    }

    @Override
    public void eliminar(Long id) {
        evaluacionRepository.deleteById(id);
    }

    @Override
    public Long totalEvaluaciones() {
        return evaluacionRepository.count();
    }

    @Override
    public Double promedioGeneral() {
        Double promedio = evaluacionRepository.obtenerPromedioGeneral();
        return promedio != null ? promedio : 0.0;
    }

    @Override
    public Long proveedoresSobresalientes() {
        return evaluacionRepository.contarSobresalientes();
    }

    @Override
    public Long proveedoresEnRiesgo() {
        return evaluacionRepository.contarEnRiesgo();
    }

    @Override
    public List<RankingProveedorDTO> obtenerRankingProveedores() {
        return evaluacionRepository.obtenerRankingProveedores();
    }

    @Override
    public List<RankingSectorDTO> obtenerRankingPorSector() {
        return evaluacionRepository.obtenerRankingPorSector();
    }

    @Override
    public List<RankingSectorDTO> obtenerRankingPorSectorFiltrado(String sector) {
        if (sector == null || sector.isBlank() || sector.equals("todos")) {
            return evaluacionRepository.obtenerRankingPorSector();
        }

        return evaluacionRepository.obtenerRankingPorSectorFiltrado(sector);
    }

    @Override
    public List<Evaluacion> listarPorFiltro(String filtro) {
        if (filtro == null || filtro.isBlank() || filtro.equals("todos")) {
            return evaluacionRepository.listarTodasOrdenadas();
        }

        return switch (filtro) {
            case "sobresalientes" -> evaluacionRepository.listarSobresalientes();
            case "intermedios" -> evaluacionRepository.listarIntermedios();
            case "riesgo" -> evaluacionRepository.listarEnRiesgo();
            default -> evaluacionRepository.listarTodasOrdenadas();
        };
    }

    @Override
    public List<IndicadorSectorDTO> obtenerIndicadoresPorSector(String sector) {

        List<RankingSectorDTO> ranking = obtenerRankingPorSectorFiltrado(sector);

        return ranking.stream()
                .collect(java.util.stream.Collectors.groupingBy(RankingSectorDTO::getSector))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<RankingSectorDTO> proveedores = entry.getValue();

                    RankingSectorDTO mejor = proveedores.get(0);
                    RankingSectorDTO riesgo = proveedores.get(proveedores.size() - 1);

                    return new IndicadorSectorDTO(
                            entry.getKey(),
                            mejor.getProveedor(),
                            mejor.getPromedio(),
                            riesgo.getProveedor(),
                            riesgo.getPromedio()
                    );
                })
                .toList();
    }

    @Override
    public List<Evaluacion> listarPorProveedor(Long idProveedor) {
        if (idProveedor == null) {
            return evaluacionRepository.listarTodasOrdenadas();
        }

        return evaluacionRepository.listarPorProveedor(idProveedor);
    }
}