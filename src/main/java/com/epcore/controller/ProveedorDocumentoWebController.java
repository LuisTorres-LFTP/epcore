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

    private static final Set<String> ESTADOS_PERMITIDOS = Set.of(
            "PENDIENTE",
            "APROBADO",
            "RECHAZADO",
            "VENCIDO"
    );

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
        model.addAttribute("estadosDocumento", ESTADOS_PERMITIDOS);

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
            documento.setEstadoValidacion("PENDIENTE");
            documento.setObservacion(observacion);
            documento.setActivo(true);

            proveedorDocumentoService.guardar(documento);

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El documento fue registrado correctamente."
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

    @PostMapping("/{idDocumento}/estado")
    public String actualizarEstadoValidacion(
            @PathVariable Long idProveedor,
            @PathVariable Long idDocumento,
            @RequestParam String estadoValidacion,
            @RequestParam(
                    value = "observacion",
                    required = false
            )
            String observacion,
            RedirectAttributes redirectAttributes
    ) {
        try {
            obtenerProveedor(idProveedor);

            ProveedorDocumento documento =
                    obtenerDocumentoDelProveedor(
                            idDocumento,
                            idProveedor
                    );

            String estadoNormalizado = estadoValidacion
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!ESTADOS_PERMITIDOS.contains(estadoNormalizado)) {
                throw new IllegalArgumentException(
                        "El estado documental no es válido."
                );
            }

            if ("APROBADO".equals(estadoNormalizado)
                    && proveedorDocumentoService.estaVencido(documento)) {
                throw new IllegalArgumentException(
                        "No se puede aprobar un documento vencido."
                );
            }

            documento.setEstadoValidacion(estadoNormalizado);
            documento.setObservacion(observacion);

            proveedorDocumentoService.guardar(documento);

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El estado del documento fue actualizado."
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