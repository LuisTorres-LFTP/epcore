package com.epcore.service;

import com.epcore.entity.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    List<Usuario> listarTodos();

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorCorreo(String correo);

    Usuario guardar(Usuario usuario);

    void eliminar(Long id);

    void inactivar(Long id);

    void activar(Long id);
}