package com.epcore.controller;

import com.epcore.entity.Permiso;
import com.epcore.entity.RolSistema;
import com.epcore.service.RolSistemaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** API para que el administrador del cliente configure roles y permisos. */
@RestController
@RequestMapping("/api/roles")
public class RolSistemaController {

    public record RolRequest(String codigo, String nombre, String descripcion,
                             Set<Permiso> permisos, BigDecimal limiteAprobacion) { }

    private final RolSistemaService rolSistemaService;

    public RolSistemaController(RolSistemaService rolSistemaService) {
        this.rolSistemaService = rolSistemaService;
    }

    @GetMapping
    public List<RolSistema> listar() {
        return rolSistemaService.listar();
    }

    /** Catálogo de permisos disponibles, agrupados, para armar roles. */
    @GetMapping("/permisos")
    public List<Map<String, String>> permisos() {
        return Arrays.stream(Permiso.values())
                .map(p -> Map.of("permiso", p.name(), "grupo", p.getGrupo(), "descripcion", p.getDescripcion()))
                .toList();
    }

    @PostMapping
    public RolSistema crear(@RequestBody RolRequest r, Authentication auth) {
        return rolSistemaService.crear(r.codigo(), r.nombre(), r.descripcion(),
                r.permisos(), r.limiteAprobacion(), auth.getName());
    }

    @PutMapping("/{id}")
    public RolSistema actualizar(@PathVariable Long id, @RequestBody RolRequest r, Authentication auth) {
        return rolSistemaService.actualizar(id, r.nombre(), r.descripcion(),
                r.permisos(), r.limiteAprobacion(), auth.getName());
    }

    @PostMapping("/{id}/desactivar")
    public RolSistema desactivar(@PathVariable Long id, Authentication auth) {
        return rolSistemaService.desactivar(id, auth.getName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> error(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
