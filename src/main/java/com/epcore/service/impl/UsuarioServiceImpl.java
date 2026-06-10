package com.epcore.service.impl;

import com.epcore.entity.Usuario;
import com.epcore.repository.UsuarioRepository;
import com.epcore.service.UsuarioService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    @Override
    public Usuario guardar(Usuario usuario) {

        if (usuario.getIdUsuario() != null) {
            Usuario usuarioExistente = usuarioRepository.findById(usuario.getIdUsuario())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            usuario.setFechaCreacion(usuarioExistente.getFechaCreacion());
        }

        return usuarioRepository.save(usuario);
    }

    @Override
    public void eliminar(Long id) {
        inactivar(id);
    }

    @Override
    public void inactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Override
    public void activar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }
}