package com.epcore.controller;

import com.epcore.service.portal.PortalProveedorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Registro público de proveedores en el portal. */
@Controller
public class RegistroProveedorController {

    private final PortalProveedorService portal;

    public RegistroProveedorController(PortalProveedorService portal) {
        this.portal = portal;
    }

    @GetMapping("/registro")
    public String formulario() {
        return "portal/registro";
    }

    @PostMapping("/registro")
    public String registrar(@RequestParam String razonSocial, @RequestParam String nit, @RequestParam String ciudad,
                            @RequestParam String representante, @RequestParam(required = false) String telefono,
                            @RequestParam String correo, @RequestParam String clave, @RequestParam String claveConfirmacion,
                            @RequestParam(defaultValue = "false") boolean autorizacionDatos,
                            Model model, RedirectAttributes flash) {
        try {
            portal.registrar(new PortalProveedorService.Registro(razonSocial, nit, ciudad, representante, telefono,
                    correo, clave, claveConfirmacion, autorizacionDatos));
            flash.addFlashAttribute("mensajeExito", "Cuenta creada. Inicie sesión con su correo.");
            return "redirect:/login?registrado";
        } catch (IllegalArgumentException e) {
            // Se conserva lo escrito (excepto las contraseñas) y se muestra qué corregir.
            model.addAttribute("mensajeError", e.getMessage());
            model.addAttribute("razonSocial", razonSocial);
            model.addAttribute("nit", nit);
            model.addAttribute("ciudad", ciudad);
            model.addAttribute("representante", representante);
            model.addAttribute("telefono", telefono);
            model.addAttribute("correo", correo);
            return "portal/registro";
        }
    }
}
