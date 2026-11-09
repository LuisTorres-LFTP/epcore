package com.epcore.controller;

import com.epcore.entity.Permiso;
import com.epcore.entity.RolSistema;
import com.epcore.service.RolSistemaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;

/** Pantalla para que el administrador de accesos configure roles y permisos. */
@Controller
@RequestMapping("/roles")
public class RolSistemaWebController {

    private final RolSistemaService rolSistemaService;

    public RolSistemaWebController(RolSistemaService rolSistemaService) {
        this.rolSistemaService = rolSistemaService;
    }

    /** Permisos agrupados para mostrarlos como casillas en el formulario. */
    @ModelAttribute("gruposPermisos")
    public Map<String, List<Permiso>> gruposPermisos() {
        Map<String, List<Permiso>> grupos = new LinkedHashMap<>();
        for (Permiso p : Permiso.values()) {
            grupos.computeIfAbsent(p.getGrupo(), g -> new ArrayList<>()).add(p);
        }
        return grupos;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("roles", rolSistemaService.listar());
        return "roles/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("rol", RolSistema.builder().permisos(new HashSet<>()).build());
        return "roles/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes flash) {
        Optional<RolSistema> rol = rolSistemaService.listar().stream()
                .filter(r -> r.getIdRol().equals(id)).findFirst();
        if (rol.isEmpty()) {
            flash.addFlashAttribute("mensajeError", "El rol no existe.");
            return "redirect:/roles";
        }
        model.addAttribute("rol", rol.get());
        return "roles/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Long idRol,
                          @RequestParam(required = false) String codigo,
                          @RequestParam String nombre,
                          @RequestParam(required = false) String descripcion,
                          @RequestParam(required = false) BigDecimal limiteAprobacion,
                          @RequestParam(required = false) List<Permiso> permisos,
                          Authentication auth,
                          Model model,
                          RedirectAttributes flash) {
        Set<Permiso> seleccion = permisos == null ? EnumSet.noneOf(Permiso.class) : EnumSet.copyOf(permisos);
        try {
            if (idRol == null) {
                rolSistemaService.crear(codigo, nombre, descripcion, seleccion, limiteAprobacion, auth.getName());
                flash.addFlashAttribute("mensajeExito", "Rol creado correctamente.");
            } else {
                rolSistemaService.actualizar(idRol, nombre, descripcion, seleccion, limiteAprobacion, auth.getName());
                flash.addFlashAttribute("mensajeExito", "Rol actualizado. Los cambios aplican en el próximo inicio de sesión de cada usuario.");
            }
            return "redirect:/roles";
        } catch (IllegalArgumentException ex) {
            // Se vuelve a mostrar el formulario con lo que escribió el usuario y el motivo.
            model.addAttribute("rol", RolSistema.builder().idRol(idRol).codigo(codigo).nombre(nombre)
                    .descripcion(descripcion).limiteAprobacion(limiteAprobacion).permisos(seleccion)
                    .predefinido(false).build());
            model.addAttribute("mensajeError", ex.getMessage());
            return "roles/formulario";
        }
    }

    @PostMapping("/desactivar/{id}")
    public String desactivar(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        try {
            rolSistemaService.desactivar(id, auth.getName());
            flash.addFlashAttribute("mensajeExito", "Rol desactivado. Sus usuarios pierden esos permisos.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/roles";
    }
}
