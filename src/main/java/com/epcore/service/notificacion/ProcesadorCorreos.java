package com.epcore.service.notificacion;

import com.epcore.entity.notificacion.CorreoSaliente;
import com.epcore.multicliente.ClientesConfigurados;
import com.epcore.repository.notificacion.CorreoSalienteRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Envía la bandeja de salida con reintentos limitados (1 min, 5 min, 30 min, 2 h, 12 h).
 * Errores permanentes (dirección inexistente) no se reintentan. Si el servidor
 * de correo no responde, se pausa el lote en lugar de fallar uno por uno.
 * Se ejecuta en la base de cada cliente.
 */
@Component
public class ProcesadorCorreos {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorCorreos.class);
    static final List<Duration> ESPERAS = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5),
            Duration.ofMinutes(30), Duration.ofHours(2), Duration.ofHours(12));
    static final int MAX_INTENTOS = ESPERAS.size();

    /** Abstracción del envío, para poder probar sin servidor de correo. */
    @FunctionalInterface
    public interface Enviador {
        void enviar(CorreoSaliente correo) throws Exception;
    }

    private final CorreoSalienteRepository repo;
    private final ClientesConfigurados clientes;
    private final Clock reloj;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;

    public ProcesadorCorreos(CorreoSalienteRepository repo, ClientesConfigurados clientes, Clock reloj,
                             ObjectProvider<JavaMailSender> mailSender,
                             @Value("${epcore.correo.remitente:EpCore Notificaciones <notificaciones@epcore.co>}") String remitente) {
        this.repo = repo;
        this.clientes = clientes;
        this.reloj = reloj;
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Scheduled(fixedDelayString = "${epcore.correo.intervalo-ms:60000}", initialDelayString = "${epcore.correo.inicio-ms:30000}")
    public void ejecutar() {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            return; // sin servidor configurado: los avisos quedan en EpCore y en la bandeja como PENDIENTE
        }
        clientes.paraCadaCliente(id -> procesar(LocalDateTime.now(reloj), c -> enviarCon(sender, c)));
    }

    /** Procesa un lote. Devuelve cuántos se enviaron. */
    public int procesar(LocalDateTime ahora, Enviador enviador) {
        int enviados = 0;
        for (CorreoSaliente c : repo.findTop50ByEstadoAndProximoIntentoLessThanEqualOrderByProximoIntentoAsc(
                CorreoSaliente.Estado.PENDIENTE, ahora)) {
            try {
                enviador.enviar(c);
                c.setEstado(CorreoSaliente.Estado.ENVIADO);
                c.setFechaEnvio(ahora);
                c.setUltimoError(null);
                enviados++;
            } catch (Exception e) {
                c.setIntentos(c.getIntentos() + 1);
                c.setUltimoError(recortar(e.getClass().getSimpleName() + ": " + e.getMessage()));
                if (esPermanente(e) || c.getIntentos() >= MAX_INTENTOS) {
                    c.setEstado(CorreoSaliente.Estado.FALLIDO);
                    c.setProximoIntento(null);
                } else {
                    c.setProximoIntento(ahora.plus(ESPERAS.get(c.getIntentos() - 1)));
                }
                repo.save(c);
                if (servidorCaido(e)) {
                    log.warn("Servidor de correo no disponible: se pausa el lote.");
                    break;
                }
                continue;
            }
            repo.save(c);
        }
        return enviados;
    }

    private void enviarCon(JavaMailSender sender, CorreoSaliente c) throws Exception {
        MimeMessage m = sender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(m, false, "UTF-8");
        h.setFrom(remitente);
        h.setTo(c.getDestinatario());
        h.setSubject(c.getAsunto());
        h.setText(c.getCuerpoHtml(), true);
        sender.send(m);
    }

    /** Dirección inválida o rechazada definitivamente (códigos SMTP 5xx de destinatario). */
    static boolean esPermanente(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String msg = String.valueOf(t.getMessage());
            if (t instanceof jakarta.mail.internet.AddressException || msg.contains("550") || msg.contains("553")
                    || msg.contains("Invalid Addresses") || msg.contains("User unknown")) {
                return true;
            }
        }
        return false;
    }

    static boolean servidorCaido(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConnectException || t instanceof UnknownHostException) return true;
        }
        return false;
    }

    private static String recortar(String s) {
        return s.length() <= 500 ? s : s.substring(0, 500);
    }
}
