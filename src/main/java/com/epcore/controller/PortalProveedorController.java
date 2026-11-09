package com.epcore.controller;

import com.epcore.entity.Proveedor;
import com.epcore.entity.ProveedorDocumento;
import com.epcore.entity.proceso.Oferta;
import com.epcore.entity.proceso.Proceso;
import com.epcore.entity.proceso.ProcesoEnums.CanalOferta;
import com.epcore.entity.proceso.ProcesoEnums.EstadoProceso;
import com.epcore.entity.proceso.ProcesoEnums.Modalidad;
import com.epcore.repository.proceso.AdendaRepository;
import com.epcore.service.CargaDocumentoService;
import com.epcore.service.ProveedorDocumentoService;
import com.epcore.service.portal.PortalProveedorService;
import com.epcore.service.proceso.ProcesoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Portal de proveedores: el proveedor solo ve y gestiona la información de su propia empresa. */
@Controller
@RequestMapping("/portal")
public class PortalProveedorController {

    private final PortalProveedorService portal;
    private final ProveedorDocumentoService documentos;
    private final CargaDocumentoService carga;
    private final ProcesoService procesos;
    private final AdendaRepository adendas;

    public PortalProveedorController(PortalProveedorService portal, ProveedorDocumentoService documentos,
                                     CargaDocumentoService carga, ProcesoService procesos, AdendaRepository adendas) {
        this.portal = portal;
        this.documentos = documentos;
        this.carga = carga;
        this.procesos = procesos;
        this.adendas = adendas;
    }

    @GetMapping
    public String inicio(Authentication auth, Model model) {
        Proveedor p = portal.proveedorDe(auth.getName());
        List<ProveedorDocumento> docs = documentos.listarActivosPorProveedor(p.getIdProveedor());
        List<Proceso> visibles = portal.procesosVisibles(p.getIdProveedor());
        model.addAttribute("proveedor", p);
        model.addAttribute("cumplimiento", documentos.calcularPorcentajeCumplimiento(p.getIdProveedor()));
        model.addAttribute("porCorregir", docs.stream().filter(d -> "RECHAZADO".equals(d.getEstadoValidacion())
                || "VENCIDO".equals(d.getEstadoValidacion())).toList());
        model.addAttribute("abiertos", visibles.stream().filter(pr -> pr.getEstado() == EstadoProceso.PUBLICADO).toList());
        return "portal/inicio";
    }

    @GetMapping("/documentos")
    public String misDocumentos(Authentication auth, Model model) {
        Proveedor p = portal.proveedorDe(auth.getName());
        model.addAttribute("proveedor", p);
        model.addAttribute("documentos", documentos.listarActivosPorProveedor(p.getIdProveedor()));
        model.addAttribute("tipos", List.of("Cámara de Comercio", "RUT", "Estados financieros", "Certificación bancaria",
                "Certificado SG-SST", "RUP", "Póliza", "Formulario SARLAFT", "Política de tratamiento de datos", "Otro"));
        return "portal/documentos";
    }

    @PostMapping("/documentos")
    public String subirDocumento(Authentication auth,
                                 @RequestParam String tipoDocumento,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaExpedicion,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVencimiento,
                                 @RequestParam("archivo") MultipartFile archivo,
                                 RedirectAttributes flash) {
        Proveedor p = portal.proveedorDe(auth.getName());
        try {
            ProveedorDocumento d = carga.cargar(p, tipoDocumento, fechaExpedicion, fechaVencimiento, archivo,
                    auth.getName() + " (portal)");
            flash.addFlashAttribute("mensajeExito", Boolean.TRUE.equals(d.getValidacionAutomatica())
                    ? "Documento aprobado automáticamente: NIT y fechas verificados."
                    : "Documento recibido. Queda en revisión por el comprador.");
        } catch (IllegalArgumentException | IOException e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/portal/documentos";
    }

    @GetMapping("/procesos")
    public String procesos(Authentication auth, Model model) {
        Proveedor p = portal.proveedorDe(auth.getName());
        model.addAttribute("procesos", portal.procesosVisibles(p.getIdProveedor()));
        return "portal/procesos";
    }

    @GetMapping("/procesos/{id}")
    public String proceso(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes flash) {
        Proveedor p = portal.proveedorDe(auth.getName());
        Proceso pr;
        try {
            pr = portal.procesoVisible(id, p.getIdProveedor());
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/portal/procesos";
        }
        Optional<Oferta> mia = portal.miOferta(id, p.getIdProveedor());
        // La verificación SARLAFT es confidencial: no se muestra al proveedor.
        List<String> requisitosPendientes = procesos.verificarHabilitacion(pr, p.getIdProveedor()).stream()
                .filter(t -> !t.startsWith("SARLAFT")).toList();
        model.addAttribute("proceso", pr);
        model.addAttribute("miOferta", mia.orElse(null));
        model.addAttribute("resultado", portal.miResultado(pr, p.getIdProveedor(), mia.isPresent()));
        model.addAttribute("pendientes", requisitosPendientes);
        model.addAttribute("adendas", adendas.findByProcesoIdProcesoOrderByNumeroAsc(id));
        model.addAttribute("puedeOfertar", pr.getEstado() == EstadoProceso.PUBLICADO && pr.getModalidad() != Modalidad.PUBLICA);
        return "portal/proceso";
    }

    @PostMapping("/procesos/{id}/oferta")
    public String ofertar(@PathVariable Long id, Authentication auth,
                          @RequestParam BigDecimal valor, @RequestParam int plazoDias,
                          @RequestParam(defaultValue = "false") boolean declaracion,
                          RedirectAttributes flash) {
        Proveedor p = portal.proveedorDe(auth.getName());
        try {
            portal.procesoVisible(id, p.getIdProveedor());
            procesos.registrarOferta(id, p.getIdProveedor(), valor, plazoDias, false, CanalOferta.PORTAL, declaracion,
                    auth.getName());
            flash.addFlashAttribute("mensajeExito", "Oferta enviada. Puede reemplazarla hasta la fecha de cierre.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/portal/procesos/" + id;
    }
}
