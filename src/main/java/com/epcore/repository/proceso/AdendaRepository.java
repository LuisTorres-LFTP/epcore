package com.epcore.repository.proceso;

import com.epcore.entity.proceso.Adenda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdendaRepository extends JpaRepository<Adenda, Long> {

    List<Adenda> findByProcesoIdProcesoOrderByNumeroAsc(Long idProceso);

    long countByProcesoIdProceso(Long idProceso);
}
