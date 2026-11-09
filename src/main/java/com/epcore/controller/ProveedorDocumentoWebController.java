package com.epcore.controller;

import com.epcore.entity.Proveedor;
import com.epcore.entity.ProveedorDocumento;
import com.epcore.repository.ProveedorRepository;
import com.epcore.service.CargaDocumentoService;
import com.epcore.service.ProveedorDocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/proveedores/{idProveedor}/documentos")
@RequiredArgsConstructor
public class ProveedorDocumentoWebController {

    private final ProveedorDocumentoService proveedorDocumentoService;
    private final ProveedorRepository proveedorRepository;
    private final com.epcore.service.DocumentoAprobacionService aprobacionService;
    private final CargaDocumentoService cargaDocumentoService;

    @GetMapping
    public String listarDocumentos(
            @PathVariable Long idProveedor,
            Model model
    ) {
        Proveedor proveedor = obtenerProveedor(idProveedor);

        proveedorDocumentoService.actualizarEstadosPorVencimiento();

        List<ProveedorDocumento> documentos =
                proveedorDocumentoService.listarActivosPorProveedor(idProveedor);

        Double porcentajeCumplimiento =
                proveedorDocumentoService.calcularPorcentajeCumplimiento(
                        idProveedor
                );

        model.addAttribute("proveedor", proveedor);
        model.addAttribute("documentos", documentos);
        model.addAttribute(
                "porcentajeCumplimiento",
                porcentajeCumplimiento
        );

        model.addAttribute(
                "totalObligatorios",
                proveedorDocumentoService
                        .contarDocumentosObligatorios(idProveedor)
        );

        model.addAttribute(
                "totalAprobados",
                proveedorDocumentoService
                        .contarDocumentosObligatoriosAprobados(idProveedor)
        );

        model.addAttribute(
                "documentosVencidos",
                documentos.stream()
                        .filter(proveedorDocumentoService::estaVencido)
                        .count()
        );

        model.addAttribute(
                "documentosProximosAVencer",
                documentos.stream()
                        .filter(documento ->
                                proveedorDocumentoService
                                        .estaProximoAVencer(documento, 30)
                        )
                        .count()
        );

        return "documentos/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(
            @PathVariable Long idProveedor,
            Model model
    ) {
        Proveedor proveedor = obtenerProveedor(idProveedor);

        ProveedorDocumento documento = new ProveedorDocumento();
        documento.setProveedor(proveedor);
        documento.setTieneVencimiento(false);
        documento.setObligatorio(true);
        documento.setEstadoValidacion("PENDIENTE");
        documento.setActivo(true);

        model.addAttribute("proveedor", proveedor);
        model.addAttribute("documento", documento);
        model.addAttribute("tiposDocumento", obtenerTiposDocumento());
        // El estado ya no se elige al cargar: lo decide la validación automática o la aprobación dual.
        model.addAttribute("estadosDocumento", java.util.List.of("PENDIENTE"));

        return "documentos/formulario";
    }

    @PostMapping("/guardar")
    public String guardarDocumento(
            @PathVariable Long idProveedor,

            @RequestParam("tipoDocumento")
            String tipoDocumento,

            @RequestParam(
                    value = "fechaExpedicion",
                    required = false
            )
            LocalDate fechaExpedicion,

            @RequestParam(
                    value = "fechaVencimiento",
                    required = false
            )
            LocalDate fechaVencimiento,

            @RequestParam(
                    value = "tieneVencimiento",
                    defaultValue = "false"
            )
            Boolean tieneVencimiento,

            @RequestParam(
                    value = "obligatorio",
                    defaultValue = "false"
            )
            Boolean obligatorio,

            @RequestParam(
                    value = "observacion",
                    required = false
            )
            String observacion,

            @RequestParam("archivo")
            MultipartFile archivo,

            org.springframework.security.core.Authentication autenticacion,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ProveedorDocumento documento = cargaDocumentoService.cargar(
                    obtenerProveedor(idProveedor),
                    new CargaDocumentoService.Datos(tipoDocumento, fechaExpedicion,
                            Boolean.TRUE.equals(tieneVencimiento) ? fechaVencimiento : null,
                            Boolean.TRUE.equals(obligatorio), observacion),
                    archivo, autenticacion.getName());

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    Boolean.TRUE.equals(documento.getValidacionAutomatica())
                            ? "Documento aprobado por el sistema: NIT y fechas verificados."
                            : "Documento registrado. El sistema no pudo validarlo y pasa a aprobación dual (Director de compras y luego Gerente)."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("mensajeError", exception.getMessage());
            return "redirect:/proveedores/" + idProveedor + "/documentos/nuevo";
        } catch (IOException exception) {
            redirectAttributes.addFlashAttribute("mensajeError", "No fue posible almacenar el archivo.");
            return "redirect:/proveedores/" + idProveedor + "/documentos/nuevo";
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("mensajeError", "Ocurrió un error al registrar el documento.");
            return "redirect:/proveedores/" + idProveedor + "/documentos/nuevo";
        }

        return "redirect:/proveedores/"
                + idProveedor
                + "/documentos";
    }

    /**
     * Aprobación dual: el nivel lo decide el estado del documento y los permisos
     * del usuario. 0/2 -> Director de compras (nivel 1); 1/2 -> Gerente (nivel 2).
     */
    @PostMapping("/{idDocumento}/aprobar")
    public String aprobarDocumento(
            @PathVariable Long idProveedor,
            @PathVariable Long idDocumento,
            org.springframework.security.core.Authentication autenticacion,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ProveedorDocumento documento = obtenerDocumentoDelProveedor(idDocumento, idProveedor);
            boolean nivel1 = tienePermiso(autenticacion, com.epcore.entity.Permiso.DOCUMENTOS_APROBAR_NIVEL_1);
            boolean nivel2 = tienePermiso(autenticacion, com.epcore.entity.Permiso.DOCUMENTOS_APROBAR_NIVEL_2);

            if (com.epcore.service.DocumentoAprobacionService.PENDIENTE.equals(documento.getEstadoValidacion()) && nivel1) {
                aprobacionService.aprobarNivel1(idDocumento, autenticacion.getName());
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Primera aprobación registrada (1/2). Pasa al Gerente como supervisor.");
            } else if (com.epcore.service.DocumentoAprobacionService.EN_APROBACION.equals(documento.getEstadoValidacion()) && nivel2) {
                aprobacionService.aprobarNivel2(idDocumento, autenticacion.getName());
                redirectAttributes.addFlashAttribute("mensajeExito", "Documento aprobado por supervisor (2/2).");
            } else {
                throw new IllegalStateException(
                        "Su rol no corresponde al paso de aprobación en que está este documento.");
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensajeError", exception.getMessage());
        }

        return "redirect:/proveedores/" + idProveedor + "/documentos";
    }

    @PostMapping("/{idDocumento}/rechazar")
    public String rechazarDocumento(
            @PathVariable Long idProveedor,
            @PathVariable Long idDocumento,
            @RequestParam String motivo,
            org.springframework.security.core.Authentication autenticacion,
            RedirectAttributes redirectAttributes
    ) {
        try {
            obtenerDocumentoDelProveedor(idDocumento, idProveedor);
            aprobacionService.rechazar(idDocumento, autenticacion.getName(), motivo);
            redirectAttributes.addFlashAttribute("mensajeExito", "Documento rechazado. Queda registrado el motivo.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("mensajeError", exception.getMessage());
        }

        return "redirect:/proveedores/" + idProveedor + "/documentos";
    }

    private static boolean tienePermiso(org.springframework.security.core.Authentication auth,
                                        com.epcore.entity.Permiso permiso) {
        return auth.getAuthorities().stream().anyMatch(a -> permiso.name().equals(a.getAuthority()));
    }

    @PostMapping("/{idDocumento}/inactivar")
    public String inactivarDocumento(
            @PathVariable Long idProveedor,
            @PathVariable Long idDocumento,
            RedirectAttributes redirectAttributes
    ) {
        try {
            obtenerProveedor(idProveedor);

            ProveedorDocumento documento =
                    obtenerDocumentoDelProveedor(
                            idDocumento,
                            idProveedor
                    );

            proveedorDocumentoService.inactivar(
                    documento.getIdDocumento()
            );

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El documento fue inactivado correctamente."
            );

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    exception.getMessage()
            );
        }

        return "redirect:/proveedores/"
                + idProveedor
                + "/documentos";
    }

    private Proveedor obtenerProveedor(Long idProveedor) {
        if (idProveedor == null || idProveedor <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del proveedor no es válido."
            );
        }

        return proveedorRepository.findById(idProveedor)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el proveedor solicitado."
                ));
    }

    private ProveedorDocumento obtenerDocumentoDelProveedor(
            Long idDocumento,
            Long idProveedor
    ) {
        ProveedorDocumento documento =
                proveedorDocumentoService.buscarPorId(idDocumento)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No existe el documento solicitado."
                                )
                        );

        if (documento.getProveedor() == null
                || documento.getProveedor().getIdProveedor() == null
                || !documento.getProveedor()
                .getIdProveedor()
                .equals(idProveedor)) {

            throw new IllegalArgumentException(
                    "El documento no pertenece al proveedor indicado."
            );
        }

        return documento;
    }

    private List<String> obtenerTiposDocumento() {
        return List.of(
                "Cámara de Comercio",
                "RUT",
                "Estados financieros",
                "Balance general",
                "Certificación bancaria",
                "Cédula del representante legal",
                "Certificación tributaria",
                "Certificación de experiencia",
                "Certificado de seguridad y salud en el trabajo",
                "Certificado de calidad",
                "Póliza de responsabilidad civil",
                "Otro"
        );
    }
}