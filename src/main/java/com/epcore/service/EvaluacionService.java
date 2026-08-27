package com.epcore.service;

import com.epcore.dto.IndicadorSectorDTO;
import com.epcore.dto.RankingProveedorDTO;
import com.epcore.dto.RankingSectorDTO;
import com.epcore.entity.Evaluacion;

import java.util.List;
import java.util.Optional;

public interface EvaluacionService {

    List<Evaluacion> listarTodas();

    Optional<Evaluacion> buscarPorId(Long id);

    Evaluacion guardar(Evaluacion evaluacion);

    void eliminar(Long id);

    Long totalEvaluaciones();

    Double promedioGeneral();

    Long proveedoresSobresalientes();

    Long proveedoresEnRiesgo();

    List<RankingProveedorDTO> obtenerRankingProveedores();

    List<RankingSectorDTO> obtenerRankingPorSector();

    List<RankingSectorDTO> obtenerRankingPorSectorFiltrado(String sector);

    List<IndicadorSectorDTO> obtenerIndicadoresPorSector(String sector);

    List<Evaluacion> listarPorFiltro(String filtro);

    List<Evaluacion> listarPorProveedor(Long idProveedor);
}