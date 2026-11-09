package com.epcore.controller;

import com.epcore.entity.proceso.Proceso;
import com.epcore.entity.proceso.ProcesoEnums.CanalOferta;
import com.epcore.entity.proceso.ProcesoEnums.Modalidad;
import com.epcore.entity.proceso.ProcesoEnums.Visibilidad;
import com.epcore.repository.ProveedorRepository;
import com.epcore.repository.proceso.AdendaRepository;
import com.epcore.repository.proceso.OfertaRepository;
import com.epcore.repository.proceso.ProcesoRepository;
import com.epcore.service.proceso.ProcesoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

/** Pantallas internas del ciclo de vida de los procesos de contratación. */
@Controller
@RequestMapping("/procesos")
public class ProcesoWebController {

    private final ProcesoService servicio;
    private final ProcesoRepository procesos;
    private final OfertaRepository ofertas;
    private final AdendaRepository adendas;
    private final ProveedorRepository proveedores;

    public ProcesoWebController(ProcesoService servicio, ProcesoRepository procesos, OfertaRepository ofertas,
                                AdendaRepository adendas, ProveedorRepository proveedores) {
        this.servicio = servicio;
        this.procesos = procesos;
        this.ofertas = ofertas;
        this.adendas = adendas;
        this.proveedores = proveedores;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("procesos", procesos.findAllByOrderByFechaCreacionDesc());
        return "procesos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("modalidades", Modalidad.values());
        model.addAttribute("proveedores", proveedores.findAll());
        return "procesos/formulario";
    }

    @PostMapping("/nuevo")
    public String crear(@RequestParam String codigo, @RequestParam String nombre, @RequestParam String entidad,
                        @RequestParam Modalidad modalidad, @RequestParam(required = false) Visibilidad visibilidad,
                        @RequestParam BigDecimal presupuesto,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaCierre,
                        @RequestParam(required = false) List<Long> invitados,
                        Authentication auth, RedirectAttributes flash) {
        try {
            Proceso p = servicio.crearBorrador(codigo, nombre, entidad, modalidad, visibilidad, presupuesto, fechaCierre,
                    invitados == null ? null : new HashSet<>(invitados), auth.getName());
            flash.addFlashAttribute("mensajeExito", "Proceso creado como borrador con la plantilla de su modalidad.");
            return "redirect:/procesos/" + p.getIdProceso();
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/procesos/nuevo";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Proceso p = servicio.obtener(id);
        model.addAttribute("proceso", p);
        model.addAttribute("ofertas", ofertas.findByProcesoIdProceso(id));
        model.addAttribute("adendas", adendas.findByProcesoIdProcesoOrderByNumeroAsc(id));
        model.addAttribute("proveedores", proveedores.findAll());
        model.addAttribute("nombres", proveedores.findAll().stream().collect(java.util.stream.Collectors.toMap(
                com.epcore.entity.Proveedor::getIdProveedor, com.epcore.entity.Proveedor::getRazonSocial)));
        model.addAttribute("evaluacion", switch (p.getEstado()) {
            case EN_EVALUACION, ADJUDICADO, DESIERTO -> servicio.evaluar(id);
            default -> List.of();
        });
        return "procesos/detalle";
    }

    @PostMapping("/{id}/enviar")
    public String enviar(@PathVariable Long id, Authentication a, RedirectAttributes f) {
        return accion(id, f, "Enviado a aprobación del Director de compras (1/2).", () -> servicio.enviarAAprobacion(id, a.getName()));
    }

    @PostMapping("/{id}/aprobar1")
    public String aprobar1(@PathVariable Long id, Authentication a, RedirectAttributes f) {
        return accion(id, f, "Términos aprobados (1/2). Pasa al Gerente.", () -> servicio.aprobarNivel1(id, a.getName()));
    }

    @PostMapping("/{id}/aprobar2")
    public String aprobar2(@PathVariable Long id, Authentication a, RedirectAttributes f) {
        return accion(id, f, "Términos aprobados por el supervisor (2/2). Proceso publicado.", () -> servicio.aprobarNivel2YPublicar(id, a.getName()));
    }

    @PostMapping("/{id}/devolver")
    public String devolver(@PathVariable Long id, @RequestParam String motivo, Authentication a, RedirectAttributes f) {
        return accion(id, f, "Proceso devuelto a borrador.", () -> servicio.devolver(id, a.getName(), motivo));
    }

    @PostMapping("/{id}/adenda")
    public String adenda(@PathVariable Long id, @RequestParam String justificacion,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime nuevoCierre,
                         Authentication a, RedirectAttributes f) {
        return accion(id, f, "Adenda emitida.", () -> servicio.emitirAdenda(id, null, null, nuevoCierre, justificacion, a.getName()));
    }

    @PostMapping("/{id}/oferta")
    public String ofertaInterna(@PathVariable Long id, @RequestParam Long idProveedor, @RequestParam BigDecimal valor,
                                @RequestParam int plazoDias, @RequestParam(defaultValue = "false") boolean industriaNacional,
                                @RequestParam(defaultValue = "false") boolean declaracion,
                                Authentication a, RedirectAttributes f) {
        return accion(id, f, "Oferta registrada.", () -> servicio.registrarOferta(id, idProveedor, valor, plazoDias,
                industriaNacional, CanalOferta.INTERNO, declaracion, a.getName()));
    }

    @PostMapping("/{id}/cerrar")
    public String cerrar(@PathVariable Long id, Authentication a, RedirectAttributes f) {
        return accion(id, f, "Recepción de ofertas cerrada. Empieza la evaluación.", () -> servicio.cerrarRecepcion(id, a.getName()));
    }

    @PostMapping("/{id}/calificar/{idOferta}")
    public String calificar(@PathVariable Long id, @PathVariable Long idOferta, @RequestParam int calificacion,
                            Authentication a, RedirectAttributes f) {
        return accion(id, f, "Calificación técnica registrada.", () -> servicio.calificarTecnica(idOferta, calificacion, a.getName()));
    }

    @PostMapping("/{id}/adjudicar")
    public String adjudicar(@PathVariable Long id, Authentication a, RedirectAttributes f) {
        return accion(id, f, "Proceso adjudicado a la oferta habilitada con mayor puntaje.", () -> servicio.adjudicar(id, a.getName()));
    }

    @PostMapping("/{id}/desierto")
    public String desierto(@PathVariable Long id, @RequestParam String motivo, Authentication a, RedirectAttributes f) {
        return accion(id, f, "Proceso declarado desierto.", () -> servicio.declararDesierto(id, a.getName(), motivo));
    }

    private String accion(Long id, RedirectAttributes flash, String exito, Supplier<?> paso) {
        try {
            paso.get();
            flash.addFlashAttribute("mensajeExito", exito);
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/procesos/" + id;
    }
}
