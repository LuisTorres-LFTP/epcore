package com.epcore.service.notificacion;

import com.epcore.entity.Proveedor;
import com.epcore.entity.Usuario;
import com.epcore.entity.notificacion.CorreoSaliente;
import com.epcore.multicliente.ClientesConfigurados;
import com.epcore.repository.UsuarioRepository;
import com.epcore.repository.notificacion.CorreoSalienteRepository;
import com.epcore.repository.notificacion.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.net.ConnectException;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificacionesYCorreoTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);

    private final CorreoSalienteRepository correos = mock(CorreoSalienteRepository.class);
    private final NotificacionRepository notificaciones = mock(NotificacionRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);

    @SuppressWarnings("unchecked")
    private final ProcesadorCorreos procesador = new ProcesadorCorreos(correos, mock(ClientesConfigurados.class), RELOJ,
            mock(ObjectProvider.class), "EpCore <notificaciones@epcore.co>");

    private CorreoSaliente pendiente(int intentos) {
        return CorreoSaliente.builder().idCorreo(1L).destinatario("a@b.co").asunto("x").cuerpoHtml("<p>x</p>")
                .estado(CorreoSaliente.Estado.PENDIENTE).intentos(intentos).proximoIntento(AHORA).build();
    }

    private void bandeja(CorreoSaliente... cs) {
        when(correos.findTop50ByEstadoAndProximoIntentoLessThanEqualOrderByProximoIntentoAsc(any(), any()))
                .thenReturn(List.of(cs));
    }

    @Test
    void enviaYMarcaComoEnviado() {
        CorreoSaliente c = pendiente(0);
        bandeja(c);
        assertEquals(1, procesador.procesar(AHORA, x -> { }));
        assertEquals(CorreoSaliente.Estado.ENVIADO, c.getEstado());
    }

    @Test
    void errorTemporalReintentaConEsperaCreciente() {
        CorreoSaliente c = pendiente(0);
        bandeja(c);
        procesador.procesar(AHORA, x -> { throw new RuntimeException("421 servicio temporalmente no disponible"); });
        assertEquals(CorreoSaliente.Estado.PENDIENTE, c.getEstado());
        assertEquals(1, c.getIntentos());
        assertEquals(AHORA.plusMinutes(1), c.getProximoIntento());

        procesador.procesar(AHORA, x -> { throw new RuntimeException("421 temporal"); });
        assertEquals(AHORA.plusMinutes(5), c.getProximoIntento(), "la segunda espera es más larga");
    }

    @Test
    void despuesDeCincoIntentosQuedaFallidoYNoSeReintentaMas() {
        CorreoSaliente c = pendiente(4);
        bandeja(c);
        procesador.procesar(AHORA, x -> { throw new RuntimeException("421 temporal"); });
        assertEquals(CorreoSaliente.Estado.FALLIDO, c.getEstado());
        assertNull(c.getProximoIntento());
    }

    @Test
    void direccionInexistenteNoSeReintenta() {
        CorreoSaliente c = pendiente(0);
        bandeja(c);
        procesador.procesar(AHORA, x -> { throw new RuntimeException("550 5.1.1 User unknown"); });
        assertEquals(CorreoSaliente.Estado.FALLIDO, c.getEstado());
        assertEquals(1, c.getIntentos());
    }

    @Test
    void siElServidorEstaCaidoSePausaElLote() {
        CorreoSaliente c1 = pendiente(0), c2 = pendiente(0);
        bandeja(c1, c2);
        List<CorreoSaliente> intentados = new ArrayList<>();
        procesador.procesar(AHORA, x -> {
            intentados.add(x);
            throw new RuntimeException("no conecta", new ConnectException("Connection refused"));
        });
        assertEquals(1, intentados.size(), "no sigue intentando con el resto del lote");
        assertEquals(CorreoSaliente.Estado.PENDIENTE, c2.getEstado());
    }

    @Test
    void unSoloCorreoPorEventoYDestinatarioPeroElAvisoInternoSiempreQueda() {
        NotificacionService servicio = new NotificacionService(notificaciones, correos, usuarios, RELOJ, "https://epcore.co");
        Usuario u = new Usuario();
        u.setCorreo("prov@empresa.co");
        u.setNombre("Andrés Ríos");
        when(correos.existsByClaveIdempotencia(anyString())).thenReturn(false, true);

        servicio.notificar("proc-7-invitacion", List.of(u), "Invitación", "Lo invitaron", "/portal/procesos/7", true);
        servicio.notificar("proc-7-invitacion", List.of(u), "Invitación", "Lo invitaron", "/portal/procesos/7", true);

        verify(notificaciones, times(2)).save(any());
        verify(correos, times(1)).save(any());
    }

    @Test
    void laPlantillaLlevaMarcaEpCoreDatosDelProveedorYEscapaHtml() {
        NotificacionService servicio = new NotificacionService(notificaciones, correos, usuarios, RELOJ, "https://epcore.co/");
        Usuario u = new Usuario();
        u.setCorreo("prov@empresa.co");
        u.setNombre("Andrés Ríos");
        u.setProveedor(Proveedor.builder().razonSocial("Brillo Express Ltda").nit("900.640.118-0").build());

        String html = servicio.plantilla(u, "Invitación <script>alert(1)</script>", "Mensaje", "/portal/procesos/7");

        assertTrue(html.contains("EpCore"));
        assertTrue(html.contains("Brillo Express Ltda"));
        assertTrue(html.contains("900.640.118-0"));
        assertTrue(html.contains("https://epcore.co/portal/procesos/7"));
        assertTrue(html.contains("nunca le pedirá su contraseña"));
        assertFalse(html.contains("<script>"), "el contenido se escapa: no se inyecta HTML en el correo");
    }
}
