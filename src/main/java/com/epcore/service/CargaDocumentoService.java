package com.epcore.service;

import com.epcore.entity.Proveedor;
import com.epcore.entity.ProveedorDocumento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Carga segura de documentos de proveedores (usada por el portal):
 * extensión permitida, tamaño máximo, firma real del archivo, nombre interno
 * aleatorio, ruta controlada y validación automática de NIT y fechas.
 */
@Service
public class CargaDocumentoService {

    public static final long TAMANO_MAXIMO = 10L * 1024 * 1024;
    private static final Set<String> EXTENSIONES = Set.of("pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png");

    private final ProveedorDocumentoService documentoService;
    private final ValidadorDocumentoPdf validador;
    private final Clock reloj;
    private final String directorio;

    public CargaDocumentoService(ProveedorDocumentoService documentoService, ValidadorDocumentoPdf validador,
                                 Clock reloj, @Value("${epcore.archivos.directorio:uploads}") String directorio) {
        this.documentoService = documentoService;
        this.validador = validador;
        this.reloj = reloj;
        this.directorio = directorio;
    }

    public ProveedorDocumento cargar(Proveedor proveedor, String tipoDocumento, LocalDate expedicion,
                                     LocalDate vencimiento, MultipartFile archivo, String usuario) throws IOException {
        if (proveedor == null || proveedor.getIdProveedor() == null) throw new IllegalArgumentException("Proveedor no válido.");
        if (tipoDocumento == null || tipoDocumento.isBlank()) throw new IllegalArgumentException("Seleccione el tipo de documento.");
        if (archivo == null || archivo.isEmpty()) throw new IllegalArgumentException("Debe seleccionar un archivo.");
        if (archivo.getSize() > TAMANO_MAXIMO) throw new IllegalArgumentException("El archivo no puede superar 10 MB.");

        String nombre = StringUtils.cleanPath(archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename());
        if (nombre.contains("..") || nombre.isBlank()) throw new IllegalArgumentException("El nombre del archivo no es válido.");
        String ext = extension(nombre);
        if (!EXTENSIONES.contains(ext)) throw new IllegalArgumentException("Formato no permitido. Use PDF, Word, Excel, JPG o PNG.");
        if (!contenidoCoincide(archivo, ext))
            throw new IllegalArgumentException("El contenido del archivo no corresponde a un ." + ext + " válido.");

        LocalDate hoy = LocalDate.now(reloj);
        if (expedicion != null && expedicion.isAfter(hoy)) throw new IllegalArgumentException("La fecha de expedición no puede ser futura.");
        if (vencimiento != null && expedicion != null && vencimiento.isBefore(expedicion))
            throw new IllegalArgumentException("El vencimiento no puede ser anterior a la expedición.");

        Path carpeta = Paths.get(directorio, "proveedores", proveedor.getIdProveedor().toString()).toAbsolutePath().normalize();
        Files.createDirectories(carpeta);
        Path destino = carpeta.resolve(UUID.randomUUID() + "." + ext).normalize();
        if (!destino.startsWith(carpeta)) throw new IllegalArgumentException("Ruta de archivo no válida.");
        try (InputStream in = archivo.getInputStream()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        ValidadorDocumentoPdf.Resultado v = validador.validar(archivo.getBytes(), ext, proveedor.getNit(), expedicion, vencimiento, hoy);
        ProveedorDocumento d = new ProveedorDocumento();
        d.setProveedor(proveedor);
        d.setTipoDocumento(tipoDocumento.trim());
        d.setNombreArchivo(nombre);
        d.setRutaArchivo(destino.toString());
        d.setFechaExpedicion(expedicion);
        d.setFechaVencimiento(vencimiento);
        d.setTieneVencimiento(vencimiento != null);
        d.setObligatorio(true);
        d.setActivo(true);
        d.setCargadoPor(usuario);
        d.setValidacionAutomatica(v.automatico());
        d.setDetalleValidacion(v.automatico() ? "Verificado por el sistema: " + String.join("; ", v.verificaciones()) : v.motivo());
        d.setEstadoValidacion(v.automatico() ? DocumentoAprobacionService.APROBADO : DocumentoAprobacionService.PENDIENTE);
        try {
            return documentoService.guardar(d);
        } catch (RuntimeException e) {
            Files.deleteIfExists(destino);
            throw e;
        }
    }

    static String extension(String nombre) {
        int i = nombre.lastIndexOf('.');
        return i < 0 ? "" : nombre.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    /** Revisa la firma de los primeros bytes: un ejecutable renombrado como .pdf no pasa. */
    static boolean contenidoCoincide(MultipartFile archivo, String ext) {
        byte[] c = new byte[8];
        int n;
        try (InputStream in = archivo.getInputStream()) {
            n = in.readNBytes(c, 0, 8);
        } catch (IOException e) {
            return false;
        }
        return switch (ext) {
            case "pdf" -> firma(c, n, 0x25, 0x50, 0x44, 0x46);
            case "png" -> firma(c, n, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "jpg", "jpeg" -> firma(c, n, 0xFF, 0xD8, 0xFF);
            case "docx", "xlsx" -> firma(c, n, 0x50, 0x4B, 0x03, 0x04);
            case "doc", "xls" -> firma(c, n, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            default -> false;
        };
    }

    private static boolean firma(byte[] d, int n, int... f) {
        if (n < f.length) return false;
        for (int i = 0; i < f.length; i++) if ((d[i] & 0xFF) != f[i]) return false;
        return true;
    }
}
