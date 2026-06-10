package com.epcore.controller;

import com.epcore.entity.Usuario;
import com.epcore.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class UsuarioWebController {

    private final UsuarioService usuarioService;

    public UsuarioWebController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
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

        model.addAttribute("usuario", usuario);
        return "usuarios/formulario";
    }

    @GetMapping("/usuarios/inactivar/{id}")
    public String inactivarUsuario(@PathVariable Long id) {
        usuarioService.inactivar(id);
        return "redirect:/usuarios";
    }

    @GetMapping("/usuarios/activar/{id}")
    public String activarUsuario(@PathVariable Long id) {
        usuarioService.activar(id);
        return "redirect:/usuarios";
    }
}