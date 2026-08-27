package com.epcore.repository;

import com.epcore.dto.RankingProveedorDTO;
import com.epcore.dto.RankingSectorDTO;
import com.epcore.entity.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    @Query("SELECT AVG(e.resultadoFinal) FROM Evaluacion e")
    Double obtenerPromedioGeneral();

    @Query("SELECT COUNT(e) FROM Evaluacion e WHERE e.resultadoFinal >= 80")
    Long contarSobresalientes();

    @Query("SELECT COUNT(e) FROM Evaluacion e WHERE e.resultadoFinal < 60")
    Long contarEnRiesgo();

    @Query("""
           SELECT new com.epcore.dto.RankingProveedorDTO(
               e.proveedor.razonSocial,
               AVG(e.resultadoFinal)
           )
           FROM Evaluacion e
           GROUP BY e.proveedor.razonSocial
           ORDER BY AVG(e.resultadoFinal) DESC
           """)
    List<RankingProveedorDTO> obtenerRankingProveedores();

    @Query("""
           SELECT new com.epcore.dto.RankingSectorDTO(
               e.proveedor.sectorEconomico,
               e.proveedor.razonSocial,
               AVG(e.resultadoFinal)
           )
           FROM Evaluacion e
           WHERE e.proveedor.sectorEconomico IS NOT NULL
           GROUP BY e.proveedor.sectorEconomico, e.proveedor.razonSocial
           ORDER BY e.proveedor.sectorEconomico ASC, AVG(e.resultadoFinal) DESC
           """)
    List<RankingSectorDTO> obtenerRankingPorSector();

    @Query("""
           SELECT new com.epcore.dto.RankingSectorDTO(
               e.proveedor.sectorEconomico,
               e.proveedor.razonSocial,
               AVG(e.resultadoFinal)
           )
           FROM Evaluacion e
           WHERE e.proveedor.sectorEconomico = :sector
           GROUP BY e.proveedor.sectorEconomico, e.proveedor.razonSocial
           ORDER BY AVG(e.resultadoFinal) DESC
           """)
    List<RankingSectorDTO> obtenerRankingPorSectorFiltrado(String sector);

    @Query("SELECT e FROM Evaluacion e ORDER BY e.fechaEvaluacion DESC")
    List<Evaluacion> listarTodasOrdenadas();

    @Query("SELECT e FROM Evaluacion e WHERE e.resultadoFinal >= 80 ORDER BY e.fechaEvaluacion DESC")
    List<Evaluacion> listarSobresalientes();

    @Query("SELECT e FROM Evaluacion e WHERE e.resultadoFinal >= 60 AND e.resultadoFinal < 80 ORDER BY e.fechaEvaluacion DESC")
    List<Evaluacion> listarIntermedios();

    @Query("SELECT e FROM Evaluacion e WHERE e.resultadoFinal < 60 ORDER BY e.fechaEvaluacion DESC")
    List<Evaluacion> listarEnRiesgo();

    @Query("""
           SELECT e
           FROM Evaluacion e
           WHERE e.proveedor.idProveedor = :idProveedor
           ORDER BY e.fechaEvaluacion DESC
           """)
    List<Evaluacion> listarPorProveedor(Long idProveedor);
}