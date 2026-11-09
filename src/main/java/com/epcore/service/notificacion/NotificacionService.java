package com.epcore.service.notificacion;

import com.epcore.entity.Rol;
import com.epcore.entity.Usuario;
import com.epcore.entity.notificacion.CorreoSaliente;
import com.epcore.entity.notificacion.Notificacion;
import com.epcore.repository.UsuarioRepository;
import com.epcore.repository.notificacion.CorreoSalienteRepository;
import com.epcore.repository.notificacion.NotificacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Crea los avisos: siempre una notificación dentro de EpCore y, si se pide,
 * un correo en la bandeja de salida (un solo correo por evento y destinatario).
 * El correo solo lleva el aviso y un enlace: nunca valores, requisitos ni resultados.
 */
@Service
@Transactional
public class NotificacionService {

    private final NotificacionRepository notificaciones;
    private final CorreoSalienteRepository correos;
    private final UsuarioRepository usuarios;
    private final Clock reloj;
    private final String urlBase;

    public NotificacionService(NotificacionRepository notificaciones, CorreoSalienteRepository correos,
                               UsuarioRepository usuarios, Clock reloj,
                               @Value("${epcore.url-base:http://localhost:8080}") String urlBase) {
        this.notificaciones = notificaciones;
        this.correos = correos;
        this.usuarios = usuarios;
        this.reloj = reloj;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    /** @param evento identificador único del hecho, p. ej. "proceso-7-publicado". */
    public void notificar(String evento, Collection<Usuario> destinatarios, String titulo, String mensaje,
                          String enlace, boolean enviarCorreo) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        for (Usuario u : destinatarios) {
            if (u == null || u.getCorreo() == null) continue;
            notificaciones.save(Notificacion.builder().destinatario(u.getCorreo()).titulo(recortar(titulo, 150))
                    .mensaje(recortar(mensaje, 1000)).enlace(enlace).leida(false).fecha(ahora).build());

            String clave = recortar(evento + "|" + u.getCorreo().toLowerCase(), 200);
            if (enviarCorreo && !correos.existsByClaveIdempotencia(clave)) {
                correos.save(CorreoSaliente.builder().claveIdempotencia(clave).destinatario(u.getCorreo())
                        .asunto("EpCore · " + recortar(titulo, 180)).cuerpoHtml(plantilla(u, titulo, mensaje, enlace))
                        .estado(CorreoSaliente.Estado.PENDIENTE).intentos(0).proximoIntento(ahora).fechaCreacion(ahora).build());
            }
        }
    }

    public void notificarRol(String evento, Rol rol, String titulo, String mensaje, String enlace, boolean correo) {
        notificar(evento, usuarios.findByRolAndActivoTrue(rol), titulo, mensaje, enlace, correo);
    }

    public void notificarProveedor(String evento, Long idProveedor, String titulo, String mensaje, String enlace, boolean correo) {
        notificar(evento, usuarios.findByProveedorIdProveedorAndActivoTrue(idProveedor), titulo, mensaje, enlace, correo);
    }

    public void notificarCorreo(String evento, String correoUsuario, String titulo, String mensaje, String enlace, boolean correo) {
        usuarios.findByCorreo(correoUsuario).ifPresent(u -> notificar(evento, List.of(u), titulo, mensaje, enlace, correo));
    }

    @Transactional(readOnly = true)
    public List<Notificacion> misNotificaciones(String correoUsuario) {
        return notificaciones.findTop50ByDestinatarioOrderByFechaDesc(correoUsuario);
    }

    @Transactional(readOnly = true)
    public long noLeidas(String correoUsuario) {
        return notificaciones.countByDestinatarioAndLeidaFalse(correoUsuario);
    }

    public void marcarLeidas(String correoUsuario) {
        notificaciones.findTop50ByDestinatarioOrderByFechaDesc(correoUsuario).forEach(n -> n.setLeida(true));
    }

    /** Correo con la marca EpCore, el nombre del destinatario y su empresa, el aviso y un enlace. */
    String plantilla(Usuario u, String titulo, String mensaje, String enlace) {
        String para = HtmlUtils.htmlEscape(u.getNombre() == null ? u.getCorreo() : u.getNombre());
        if (u.getProveedor() != null) {
            para += " · " + HtmlUtils.htmlEscape(u.getProveedor().getRazonSocial() + " · NIT " + u.getProveedor().getNit());
        }
        String url = urlBase + (enlace == null ? "/login" : enlace);
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #E5EAF2;border-radius:12px;overflow:hidden">
                  <div style="background:#0B1F3A;color:#fff;padding:16px 22px;font-size:20px;font-weight:bold">EpCore
                    <span style="font-size:12px;font-weight:normal;color:#BFD3F5"> · Enterprise Provider Core</span></div>
                  <div style="padding:22px;color:#0F172A">
                    <p style="color:#64748B;font-size:13px;margin:0 0 12px">Para: %s</p>
                    <h2 style="font-size:18px;margin:0 0 10px">%s</h2>
                    <p style="font-size:14px;line-height:1.5">%s</p>
                    <p style="margin:22px 0"><a href="%s" style="background:#2563EB;color:#fff;padding:10px 18px;border-radius:8px;text-decoration:none">Ver en EpCore</a></p>
                  </div>
                  <div style="background:#F8FAFC;padding:14px 22px;font-size:12px;color:#64748B">
                    Este es un aviso automático de EpCore. EpCore nunca le pedirá su contraseña por correo.
                    Si no esperaba este mensaje, ingrese directamente a la plataforma en lugar de usar el enlace.</div>
                </div>""".formatted(para, HtmlUtils.htmlEscape(titulo), HtmlUtils.htmlEscape(mensaje), HtmlUtils.htmlEscape(url));
    }

    private static String recortar(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max));
    }
}
