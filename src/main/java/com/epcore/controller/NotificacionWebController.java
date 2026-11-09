package com.epcore.controller;

import com.epcore.service.notificacion.NotificacionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/** Avisos del usuario autenticado (internos y del portal). Cada uno ve solo los suyos. */
@Controller
public class NotificacionWebController {

    private final NotificacionService notificaciones;

    public NotificacionWebController(NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping("/notificaciones")
    public String ver(Authentication auth, Model model) {
        model.addAttribute("notificaciones", notificaciones.misNotificaciones(auth.getName()));
        model.addAttribute("noLeidas", notificaciones.noLeidas(auth.getName()));
        return "notificaciones/lista";
    }

    @PostMapping("/notificaciones/leidas")
    public String marcarLeidas(Authentication auth) {
        notificaciones.marcarLeidas(auth.getName());
        return "redirect:/notificaciones";
    }
}
