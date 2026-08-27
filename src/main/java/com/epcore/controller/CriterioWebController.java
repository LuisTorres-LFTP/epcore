package com.epcore.controller;

import com.epcore.entity.Criterio;
import com.epcore.service.CriterioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CriterioWebController {

    private final CriterioService criterioService;

    public CriterioWebController(CriterioService criterioService) {
        this.criterioService = criterioService;
    }

    @GetMapping("/criterios")
    public String listarCriterios(Model model) {
        model.addAttribute("criterios", criterioService.listarTodos());
        return "proveedores/criterios/lista";
    }

    @GetMapping("/criterios/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("criterio", new Criterio());
        return "proveedores/criterios/formulario";
    }

    @PostMapping("/criterios/guardar")
    public String guardarCriterio(@ModelAttribute Criterio criterio) {
        criterioService.guardar(criterio);
        return "redirect:/criterios";
    }

    @GetMapping("/criterios/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Criterio criterio = criterioService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Criterio no encontrado"));

        model.addAttribute("criterio", criterio);
        return "proveedores/criterios/formulario";
    }

    @GetMapping("/criterios/eliminar/{id}")
    public String eliminarCriterio(@PathVariable Long id) {
        criterioService.eliminar(id);
        return "redirect:/criterios";
    }
}