package com.epcore.controller;

import com.epcore.entity.Usuario;
import com.epcore.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class UsuarioWebController {

    private final UsuarioService usuarioService;
    private final org.springframework.beans.factory.ObjectProvider<com.epcore.service.RolSistemaService> rolSistemaService;

    public UsuarioWebController(UsuarioService usuarioService,
                                org.springframework.beans.factory.ObjectProvider<com.epcore.service.RolSistemaService> rolSistemaService) {
        this.usuarioService = usuarioService;
        this.rolSistemaService = rolSistemaService;
    }

    /** Roles personalizados activos, para el formulario de usuario. */
    @ModelAttribute("perfiles")
    public java.util.List<com.epcore.entity.RolSistema> perfiles() {
        com.epcore.service.RolSistemaService servicio = rolSistemaService.getIfAvailable();
        if (servicio == null) {
            return java.util.List.of();
        }
        return servicio.listar().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getPredefinido()) && Boolean.TRUE.equals(r.getActivo()))
                .toList();
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "usuarios/lista";
    }

    @GetMapping("/usuarios/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "usuarios/formulario";
    }

    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario) {
        usuarioService.guardar(usuario);
        return "redirect:/usuarios";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // No se expone el hash de contraseña en el formulario.
        usuario.setPassword(null);
        model.addAttribute("usuario", usuario);
        return "usuarios/formulario";
    }

    @PostMapping("/usuarios/inactivar/{id}")
    public String inactivarUsuario(@PathVariable Long id) {
        usuarioService.inactivar(id);
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/activar/{id}")
    public String activarUsuario(@PathVariable Long id) {
        usuarioService.activar(id);
        return "redirect:/usuarios";
    }
}