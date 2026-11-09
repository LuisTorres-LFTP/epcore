package com.epcore.repository.notificacion;

import com.epcore.entity.notificacion.CorreoSaliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CorreoSalienteRepository extends JpaRepository<CorreoSaliente, Long> {

    boolean existsByClaveIdempotencia(String clave);

    List<CorreoSaliente> findTop50ByEstadoAndProximoIntentoLessThanEqualOrderByProximoIntentoAsc(
            CorreoSaliente.Estado estado, LocalDateTime ahora);
}
