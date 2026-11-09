package com.epcore.repository;

import com.epcore.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    /** Destinatarios internos de un aviso según su rol. */
    java.util.List<Usuario> findByRolAndActivoTrue(com.epcore.entity.Rol rol);

    /** Usuarios del portal de un proveedor. */
    java.util.List<Usuario> findByProveedorIdProveedorAndActivoTrue(Long idProveedor);
}