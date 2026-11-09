package com.epcore.service;

import com.epcore.entity.ProveedorDocumento;
import com.epcore.repository.ProveedorDocumentoRepository;
import com.epcore.security.SeparacionFunciones;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aprobación dual de documentos: primero el Director de compras (nivel 1)
 * y después el Gerente como supervisor (nivel 2). Reglas que no se pueden saltar:
 * nadie aprueba lo que cargó, el nivel 2 lo hace una persona distinta al nivel 1,
 * no se aprueba un documento vencido y el orden es obligatorio.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentoAprobacionService {

    public static final String PENDIENTE = "PENDIENTE";        // 0/2: espera al Director
    public static final String EN_APROBACION = "EN_APROBACION"; // 1/2: espera al Gerente
    public static final String APROBADO = "APROBADO";
    public static final String RECHAZADO = "RECHAZADO";

    private final ProveedorDocumentoRepository repository;
    private final com.epcore.service.notificacion.NotificacionService notificaciones;

    public ProveedorDocumento aprobarNivel1(Long idDocumento, String usuario) {
        ProveedorDocumento d = obtener(idDocumento);
        exigirEstado(d, PENDIENTE, "La primera aprobación solo aplica a documentos pendientes (0/2).");
        exigirVigente(d);
        SeparacionFunciones.validarAprobacion(usuario, d.getCargadoPor(), null);

        d.setAprobadoNivel1Por(usuario);
        d.setFechaAprobacionNivel1(LocalDateTime.now());
        d.setEstadoValidacion(EN_APROBACION);
        notificaciones.notificarRol("doc-" + d.getIdDocumento() + "-nivel1", com.epcore.entity.Rol.GERENTE,
                "Documento pendiente de su aprobación como supervisor (2/2)",
                "El Director de compras aprobó «" + d.getTipoDocumento() + "». Falta su aprobación como supervisor.",
                enlace(d), true);
        return repository.save(d);
    }

    public ProveedorDocumento aprobarNivel2(Long idDocumento, String usuario) {
        ProveedorDocumento d = obtener(idDocumento);
        exigirEstado(d, EN_APROBACION,
                "El supervisor solo puede aprobar después del Director de compras (documento en 1/2).");
        exigirVigente(d);
        SeparacionFunciones.validarAprobacion(usuario, d.getCargadoPor(), d.getAprobadoNivel1Por());

        d.setAprobadoNivel2Por(usuario);
        d.setFechaAprobacionNivel2(LocalDateTime.now());
        d.setEstadoValidacion(APROBADO);
        if (d.getProveedor() != null) {
            notificaciones.notificarProveedor("doc-" + d.getIdDocumento() + "-aprobado", d.getProveedor().getIdProveedor(),
                    "Documento aprobado", "Su documento «" + d.getTipoDocumento() + "» fue aprobado.", "/portal/documentos", false);
        }
        return repository.save(d);
    }

    public ProveedorDocumento rechazar(Long idDocumento, String usuario, String motivo) {
        ProveedorDocumento d = obtener(idDocumento);
        if (!PENDIENTE.equals(d.getEstadoValidacion()) && !EN_APROBACION.equals(d.getEstadoValidacion())) {
            throw new IllegalStateException("Solo se pueden rechazar documentos que están en aprobación.");
        }
        if (motivo == null || motivo.trim().length() < 10) {
            throw new IllegalArgumentException("Explique el motivo del rechazo (mínimo 10 caracteres).");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalStateException("No se identificó al usuario que rechaza.");
        }

        d.setRechazadoPor(usuario);
        d.setFechaRechazo(LocalDateTime.now());
        d.setMotivoRechazo(motivo.trim());
        d.setEstadoValidacion(RECHAZADO);
        if (d.getProveedor() != null) {
            notificaciones.notificarProveedor("doc-" + d.getIdDocumento() + "-rechazado", d.getProveedor().getIdProveedor(),
                    "Documento rechazado: debe cargarlo de nuevo",
                    "Su documento «" + d.getTipoDocumento() + "» fue rechazado. Motivo: " + d.getMotivoRechazo(),
                    "/portal/documentos", true);
        }
        if (d.getCargadoPor() != null) {
            notificaciones.notificarCorreo("doc-" + d.getIdDocumento() + "-rechazado-int", d.getCargadoPor(),
                    "Documento rechazado", "«" + d.getTipoDocumento() + "» fue rechazado: " + d.getMotivoRechazo(), enlace(d), false);
        }
        return repository.save(d);
    }

    private static String enlace(ProveedorDocumento d) {
        return d.getProveedor() == null ? "/proveedores" : "/proveedores/" + d.getProveedor().getIdProveedor() + "/documentos";
    }

    private ProveedorDocumento obtener(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El identificador del documento no es válido.");
        }
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el documento " + id + "."));
    }

    private static void exigirEstado(ProveedorDocumento d, String estado, String mensaje) {
        if (!estado.equals(d.getEstadoValidacion())) {
            throw new IllegalStateException(mensaje + " Estado actual: " + d.getEstadoValidacion() + ".");
        }
    }

    private static void exigirVigente(ProveedorDocumento d) {
        if (Boolean.TRUE.equals(d.getTieneVencimiento()) && d.getFechaVencimiento() != null
                && d.getFechaVencimiento().isBefore(LocalDate.now())) {
            throw new IllegalStateException("No se puede aprobar un documento vencido.");
        }
    }
}
