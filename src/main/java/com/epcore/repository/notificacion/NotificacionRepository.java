package com.epcore.repository.notificacion;

import com.epcore.entity.notificacion.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findTop50ByDestinatarioOrderByFechaDesc(String destinatario);

    long countByDestinatarioAndLeidaFalse(String destinatario);
}
