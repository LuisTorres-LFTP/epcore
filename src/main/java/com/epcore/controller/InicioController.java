package com.epcore.controller;

import com.epcore.entity.Permiso;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Puerta de entrada de la aplicación web: la raíz lleva a la pantalla que
 * corresponde según los permisos del usuario y el login es propio de EpCore.
 */
@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio(Authentication authentication) {
        return "redirect:" + paginaInicial(authentication);
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "login";
    }

    /** Pantalla inicial según permisos: proveedores al portal, compras a evaluaciones. */
    public static String paginaInicial(Authentication authentication) {
        boolean esProveedor = tienePermiso(authentication, Permiso.PORTAL_PROVEEDOR);
        boolean veEvaluaciones = tienePermiso(authentication, Permiso.EVALUACIONES_VER);
        return esProveedor ? "/portal" : veEvaluaciones ? "/evaluaciones" : "/proveedores";
    }

    private static boolean tienePermiso(Authentication authentication, Permiso permiso) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> permiso.name().equals(a.getAuthority()));
    }
}
