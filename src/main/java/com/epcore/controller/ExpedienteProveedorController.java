package com.epcore.controller;

import com.epcore.entity.*;
import com.epcore.repository.NichoRepository;
import com.epcore.repository.ProveedorRepository;
import com.epcore.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Expediente del proveedor: información financiera, experiencia, SARLAFT y
 * clasificación CIIU/nicho. Cada acción exige su permiso (ver SecurityConfig).
 */
@Controller
@RequestMapping("/proveedores/{id}/expediente")
public class ExpedienteProveedorController {

    private final ProveedorRepository proveedores;
    private final InformacionFinancieraService financiera;
    private final ContratoExperienciaService contratos;
    private final ConsultaSarlaftService sarlaft;
    private final ClasificacionProveedorService clasificacion;
    private final NichoRepository nichos;

    public ExpedienteProveedorController(ProveedorRepository proveedores, InformacionFinancieraService financiera,
                                         ContratoExperienciaService contratos, ConsultaSarlaftService sarlaft,
                                         ClasificacionProveedorService clasificacion, NichoRepository nichos) {
        this.proveedores = proveedores;
        this.financiera = financiera;
        this.contratos = contratos;
        this.sarlaft = sarlaft;
        this.clasificacion = clasificacion;
        this.nichos = nichos;
    }

    @GetMapping
    public String ver(@PathVariable Long id, Model model) {
        model.addAttribute("proveedor", proveedor(id));
        model.addAttribute("financiera", financiera.listarPorProveedor(id));
        model.addAttribute("contratos", contratos.listarPorProveedor(id));
        model.addAttribute("experienciaTotal", contratos.sumarExperienciaAcreditable(id, null, false));
        model.addAttribute("estadoSarlaft", sarlaft.calcularEstado(id));
        model.addAttribute("consultas", sarlaft.listarPorProveedor(id));
        model.addAttribute("listas", ListaRestrictiva.values());
        model.addAttribute("resultados", ResultadoSarlaft.values());
        model.addAttribute("clasificacion", clasificacion.obtener(id).orElse(null));
        model.addAttribute("tipos", TipoProveedor.values());
        model.addAttribute("nichos", nichos.findByActivoTrueOrderByNombreAsc());
        return "proveedores/expediente";
    }

    @PostMapping("/financiera")
    public String guardarFinanciera(@PathVariable Long id,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCorte,
                                    @RequestParam BigDecimal activoCorriente, @RequestParam BigDecimal pasivoCorriente,
                                    @RequestParam BigDecimal activoTotal, @RequestParam BigDecimal pasivoTotal,
                                    @RequestParam BigDecimal patrimonio,
                                    @RequestParam(required = false) BigDecimal ingresosOperacionales,
                                    @RequestParam(required = false) BigDecimal utilidadOperacional,
                                    @RequestParam(required = false) BigDecimal gastosIntereses,
                                    @RequestParam(required = false) String fuente, RedirectAttributes f) {
        return accion(id, f, "Estados financieros registrados.", () -> financiera.guardar(InformacionFinanciera.builder()
                .proveedor(proveedor(id)).fechaCorte(fechaCorte).activoCorriente(activoCorriente)
                .pasivoCorriente(pasivoCorriente).activoTotal(activoTotal).pasivoTotal(pasivoTotal).patrimonio(patrimonio)
                .ingresosOperacionales(ingresosOperacionales).utilidadOperacional(utilidadOperacional)
                .gastosIntereses(gastosIntereses).fuente(fuente).build()));
    }

    @PostMapping("/contratos")
    public String guardarContrato(@PathVariable Long id, @RequestParam String contratante,
                                  @RequestParam(defaultValue = "false") boolean contratantePublico,
                                  @RequestParam String objeto, @RequestParam BigDecimal valor,
                                  @RequestParam(required = false) BigDecimal porcentajeParticipacion,
                                  @RequestParam(required = false) String codigosUnspsc,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaTerminacion,
                                  @RequestParam(defaultValue = "false") boolean tieneCertificacion, RedirectAttributes f) {
        return accion(id, f, "Contrato de experiencia registrado.", () -> contratos.guardar(ContratoExperiencia.builder()
                .proveedor(proveedor(id)).contratante(contratante).contratantePublico(contratantePublico).objeto(objeto)
                .valor(valor).porcentajeParticipacion(porcentajeParticipacion).codigosUnspsc(codigosUnspsc)
                .fechaInicio(fechaInicio).fechaTerminacion(fechaTerminacion).tieneCertificacion(tieneCertificacion).build()));
    }

    @PostMapping("/sarlaft")
    public String registrarSarlaft(@PathVariable Long id, @RequestParam ListaRestrictiva lista,
                                   @RequestParam String documentoConsultado, @RequestParam String nombreConsultado,
                                   @RequestParam ResultadoSarlaft resultado, @RequestParam(required = false) String referencia,
                                   @RequestParam(required = false) String observacion, Authentication auth, RedirectAttributes f) {
        return accion(id, f, "Consulta SARLAFT registrada como evidencia.", () -> sarlaft.registrarConsulta(ConsultaSarlaft.builder()
                .proveedor(proveedor(id)).lista(lista).documentoConsultado(documentoConsultado)
                .nombreConsultado(nombreConsultado).resultado(resultado).referencia(referencia)
                .observacion(observacion).usuarioResponsable(auth.getName()).build()));
    }

    @PostMapping("/clasificar")
    public String clasificar(@PathVariable Long id, @RequestParam TipoProveedor tipo,
                             @RequestParam(required = false) List<String> codigo,
                             @RequestParam(required = false) List<TipoActividad> tipoActividad,
                             @RequestParam(required = false) String codigosOficiales, RedirectAttributes f) {
        List<ClasificadorProveedor.ActividadDeclarada> actividades = new ArrayList<>();
        if (codigo != null && tipoActividad != null) {
            for (int i = 0; i < Math.min(codigo.size(), tipoActividad.size()); i++) {
                if (codigo.get(i) != null && !codigo.get(i).isBlank()) {
                    actividades.add(new ClasificadorProveedor.ActividadDeclarada(codigo.get(i).trim(), tipoActividad.get(i)));
                }
            }
        }
        Set<String> oficiales = codigosOficiales == null || codigosOficiales.isBlank() ? null
                : new LinkedHashSet<>(Arrays.stream(codigosOficiales.split("[,\\s]+")).filter(s -> !s.isBlank()).toList());
        return accion(id, f, "Clasificación calculada.", () -> clasificacion.clasificar(id, tipo, actividades, oficiales));
    }

    @PostMapping("/clasificacion/resolver")
    public String resolver(@PathVariable Long id, @RequestParam(required = false) Set<Long> nichosAsignados,
                           @RequestParam String justificacion, Authentication auth, RedirectAttributes f) {
        return accion(id, f, "Excepción resuelta manualmente.",
                () -> clasificacion.resolverManualmente(id, nichosAsignados, justificacion, auth.getName()));
    }

    @PostMapping("/clasificacion/rechazar")
    public String rechazar(@PathVariable Long id, @RequestParam String justificacion, Authentication auth, RedirectAttributes f) {
        return accion(id, f, "Clasificación rechazada.", () -> clasificacion.rechazar(id, justificacion, auth.getName()));
    }

    private Proveedor proveedor(Long id) {
        return proveedores.findById(id).orElseThrow(() -> new IllegalArgumentException("No existe el proveedor " + id + "."));
    }

    private String accion(Long id, RedirectAttributes flash, String exito, java.util.function.Supplier<?> paso) {
        try {
            paso.get();
            flash.addFlashAttribute("mensajeExito", exito);
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/proveedores/" + id + "/expediente";
    }
}
