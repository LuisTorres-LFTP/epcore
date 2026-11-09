package com.epcore.controller;

import com.epcore.entity.Proveedor;
import com.epcore.entity.ProveedorDocumento;
import com.epcore.repository.ProveedorRepository;
import com.epcore.service.ProveedorDocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/proveedores/{idProveedor}/documentos")
@RequiredArgsConstructor
public class ProveedorDocumentoWebController {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(
            "pdf",
            "doc",
            "docx",
            "xls",
            "xlsx",
            "jpg",
            "jpeg",
            "png"
    );

    private static final long TAMANO_MAXIMO_ARCHIVO = 10 * 1024 * 1024;

    private final ProveedorDocumentoService proveedorDocumentoService;
    private final ProveedorRepository proveedorRepository;
    private final com.epcore.service.DocumentoAprobacionService aprobacionService;
    private final com.epcore.service.ValidadorDocumentoPdf validadorPdf;

    @Value("${epcore.archivos.directorio:uploads}")
    private String directorioArchivos;

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
        Path archivoGuardado = null;

        try {
            Proveedor proveedor = obtenerProveedor(idProveedor);

            validarArchivo(archivo);

            archivoGuardado = guardarArchivoFisico(
                    idProveedor,
                    archivo
            );

            ProveedorDocumento documento = new ProveedorDocumento();

            documento.setProveedor(proveedor);
            documento.setTipoDocumento(tipoDocumento);
            documento.setNombreArchivo(
                    limpiarNombreArchivo(
                            archivo.getOriginalFilename()
                    )
            );
            documento.setRutaArchivo(
                    archivoGuardado.toString()
            );
            documento.setFechaExpedicion(fechaExpedicion);
            documento.setFechaVencimiento(fechaVencimiento);
            documento.setTieneVencimiento(tieneVencimiento);
            documento.setObligatorio(obligatorio);
            documento.setObservacion(observacion);
            documento.setActivo(true);
            documento.setCargadoPor(autenticacion.getName());

            // Validación automática: si NIT y fechas coinciden, queda aprobado por el sistema;
            // si no, pasa a la aprobación dual (Director de compras -> Gerente).
            com.epcore.service.ValidadorDocumentoPdf.Resultado validacion = validadorPdf.validar(
                    archivo.getBytes(),
                    obtenerExtension(limpiarNombreArchivo(archivo.getOriginalFilename())),
                    proveedor.getNit(),
                    fechaExpedicion,
                    Boolean.TRUE.equals(tieneVencimiento) ? fechaVencimiento : null,
                    LocalDate.now()
            );
            documento.setValidacionAutomatica(validacion.automatico());
            documento.setDetalleValidacion(validacion.automatico()
                    ? "Verificado por el sistema: " + String.join("; ", validacion.verificaciones())
                    : validacion.motivo());
            documento.setEstadoValidacion(validacion.automatico()
                    ? com.epcore.service.DocumentoAprobacionService.APROBADO
                    : com.epcore.service.DocumentoAprobacionService.PENDIENTE);

            proveedorDocumentoService.guardar(documento);

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    validacion.automatico()
                            ? "Documento aprobado por el sistema: NIT y fechas verificados."
                            : "Documento registrado. El sistema no pudo validarlo y pasa a aprobación dual (Director de compras y luego Gerente)."
            );

        } catch (IllegalArgumentException exception) {
            eliminarArchivoSiExiste(archivoGuardado);

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    exception.getMessage()
            );

            return "redirect:/proveedores/"
                    + idProveedor
                    + "/documentos/nuevo";

        } catch (IOException exception) {
            eliminarArchivoSiExiste(archivoGuardado);

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No fue posible almacenar el archivo."
            );

            return "redirect:/proveedores/"
                    + idProveedor
                    + "/documentos/nuevo";

        } catch (Exception exception) {
            eliminarArchivoSiExiste(archivoGuardado);

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "Ocurrió un error al registrar el documento."
            );

            return "redirect:/proveedores/"
                    + idProveedor
                    + "/documentos/nuevo";
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

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un archivo."
            );
        }

        if (archivo.getSize() > TAMANO_MAXIMO_ARCHIVO) {
            throw new IllegalArgumentException(
                    "El archivo no puede superar los 10 MB."
            );
        }

        String nombreOriginal = archivo.getOriginalFilename();

        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new IllegalArgumentException(
                    "El archivo no tiene un nombre válido."
            );
        }

        String extension = obtenerExtension(nombreOriginal);

        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido. "
                            + "Formatos permitidos: PDF, Word, Excel, JPG y PNG."
            );
        }

        if (!contenidoCoincideConExtension(archivo, extension)) {
            throw new IllegalArgumentException(
                    "El contenido del archivo no corresponde a un ."
                            + extension
                            + " válido. Por seguridad no se acepta."
            );
        }
    }

    /**
     * Revisa la "firma" de los primeros bytes del archivo: así no se puede
     * subir, por ejemplo, un ejecutable renombrado como .pdf.
     */
    static boolean contenidoCoincideConExtension(MultipartFile archivo, String extension) {
        byte[] cabecera = new byte[8];
        int leidos;

        try (java.io.InputStream entrada = archivo.getInputStream()) {
            leidos = entrada.readNBytes(cabecera, 0, cabecera.length);
        } catch (IOException e) {
            return false;
        }

        return switch (extension) {
            case "pdf" -> empiezaCon(cabecera, leidos, 0x25, 0x50, 0x44, 0x46);              // %PDF
            case "png" -> empiezaCon(cabecera, leidos, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "jpg", "jpeg" -> empiezaCon(cabecera, leidos, 0xFF, 0xD8, 0xFF);
            case "docx", "xlsx" -> empiezaCon(cabecera, leidos, 0x50, 0x4B, 0x03, 0x04);     // ZIP (Office moderno)
            case "doc", "xls" -> empiezaCon(cabecera, leidos, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            default -> false;
        };
    }

    private static boolean empiezaCon(byte[] datos, int leidos, int... firma) {
        if (leidos < firma.length) {
            return false;
        }

        for (int i = 0; i < firma.length; i++) {
            if ((datos[i] & 0xFF) != firma[i]) {
                return false;
            }
        }

        return true;
    }

    private Path guardarArchivoFisico(
            Long idProveedor,
            MultipartFile archivo
    ) throws IOException {

        String nombreOriginal = limpiarNombreArchivo(
                archivo.getOriginalFilename()
        );

        String extension = obtenerExtension(nombreOriginal);

        String nombreInterno = UUID.randomUUID()
                + "."
                + extension;

        Path directorioProveedor = Paths.get(
                directorioArchivos,
                "proveedores",
                idProveedor.toString()
        ).toAbsolutePath().normalize();

        Files.createDirectories(directorioProveedor);

        Path rutaDestino = directorioProveedor
                .resolve(nombreInterno)
                .normalize();

        if (!rutaDestino.startsWith(directorioProveedor)) {
            throw new IllegalArgumentException(
                    "La ruta del archivo no es válida."
            );
        }

        Files.copy(
                archivo.getInputStream(),
                rutaDestino,
                StandardCopyOption.REPLACE_EXISTING
        );

        return rutaDestino;
    }

    private String limpiarNombreArchivo(String nombreArchivo) {
        if (nombreArchivo == null) {
            return "archivo";
        }

        String nombreLimpio =
                StringUtils.cleanPath(nombreArchivo);

        if (nombreLimpio.contains("..")) {
            throw new IllegalArgumentException(
                    "El nombre del archivo no es válido."
            );
        }

        return nombreLimpio;
    }

    private String obtenerExtension(String nombreArchivo) {
        String nombreLimpio = limpiarNombreArchivo(nombreArchivo);

        int posicionPunto = nombreLimpio.lastIndexOf('.');

        if (posicionPunto < 0
                || posicionPunto == nombreLimpio.length() - 1) {
            throw new IllegalArgumentException(
                    "El archivo debe tener una extensión válida."
            );
        }

        return nombreLimpio
                .substring(posicionPunto + 1)
                .toLowerCase(Locale.ROOT);
    }

    private void eliminarArchivoSiExiste(Path rutaArchivo) {
        if (rutaArchivo == null) {
            return;
        }

        try {
            Files.deleteIfExists(rutaArchivo);
        } catch (IOException ignored) {
            // El error de limpieza no debe ocultar el error principal.
        }
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