package com.epcore.repository.proceso;

import com.epcore.entity.proceso.Proceso;
import com.epcore.entity.proceso.ProcesoEnums.EstadoProceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcesoRepository extends JpaRepository<Proceso, Long> {

    boolean existsByCodigo(String codigo);

    List<Proceso> findByEstadoOrderByFechaCierreAsc(EstadoProceso estado);

    List<Proceso> findAllByOrderByFechaCreacionDesc();
}
