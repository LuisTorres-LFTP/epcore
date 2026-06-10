package com.epcore.controller;

import com.epcore.entity.Proveedor;
import com.epcore.service.ProveedorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProveedorWebController {

    private final ProveedorService proveedorService;

    public ProveedorWebController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping("/proveedores")
    public String listarProveedores(Model model) {
        model.addAttribute("proveedores", proveedorService.listarProveedores());
        return "proveedores/lista";
    }

    @GetMapping("/proveedores/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        return "proveedores/formulario";
    }

    @PostMapping("/proveedores/guardar")
    public String guardarProveedor(@ModelAttribute Proveedor proveedor) {
        proveedorService.guardarProveedor(proveedor);
        return "redirect:/proveedores";
    }

    @GetMapping("/proveedores/editar/{idProveedor}")
    public String mostrarFormularioEditar(@PathVariable Long idProveedor, Model model) {
        Proveedor proveedor = proveedorService.buscarPorId(idProveedor)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));

        model.addAttribute("proveedor", proveedor);
        return "proveedores/formulario";
    }

    @GetMapping("/proveedores/eliminar/{idProveedor}")
    public String eliminarProveedor(@PathVariable Long idProveedor) {
        proveedorService.eliminarProveedor(idProveedor);
        return "redirect:/proveedores";
    }
}