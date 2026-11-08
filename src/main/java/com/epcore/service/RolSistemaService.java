package com.epcore.service;

import com.epcore.entity.Permiso;
import com.epcore.entity.RolSistema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface RolSistemaService {

    List<RolSistema> listar();

    RolSistema crear(String codigo, String nombre, String descripcion,
                     Set<Permiso> permisos, BigDecimal limiteAprobacion, String usuario);

    /** Cambia nombre, descripción, permisos y límite. El código no cambia nunca. */
    RolSistema actualizar(Long idRol, String nombre, String descripcion,
                          Set<Permiso> permisos, BigDecimal limiteAprobacion, String usuario);

    /** Desactiva un rol propio del cliente. Los predefinidos no se pueden desactivar. */
    RolSistema desactivar(Long idRol, String usuario);
}
