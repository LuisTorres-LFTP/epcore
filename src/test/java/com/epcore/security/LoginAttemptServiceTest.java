package com.epcore.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private static class RelojMovible extends Clock {
        Instant ahora = Instant.parse("2026-11-08T10:00:00Z");
        void avanzar(Duration d) { ahora = ahora.plus(d); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final RelojMovible reloj = new RelojMovible();
    private final LoginAttemptService servicio = new LoginAttemptService(reloj);

    @Test
    void bloqueaTrasCincoFallosYDesbloqueaDespuesDelTiempo() {
        for (int i = 0; i < LoginAttemptService.MAX_FALLOS - 1; i++) {
            servicio.registrarFallo("Admin@EpCore.co");
        }
        assertFalse(servicio.estaBloqueado("admin@epcore.co"));

        servicio.registrarFallo("admin@epcore.co");
        assertTrue(servicio.estaBloqueado("ADMIN@epcore.co"));

        reloj.avanzar(LoginAttemptService.BLOQUEO.plusSeconds(1));
        assertFalse(servicio.estaBloqueado("admin@epcore.co"));
    }

    @Test
    void unInicioExitosoReiniciaElConteo() {
        for (int i = 0; i < 4; i++) {
            servicio.registrarFallo("a@epcore.co");
        }
        servicio.registrarExito("a@epcore.co");
        servicio.registrarFallo("a@epcore.co");

        assertFalse(servicio.estaBloqueado("a@epcore.co"));
    }

    @Test
    void fallosEspaciadosFueraDeLaVentanaNoBloquean() {
        for (int i = 0; i < 10; i++) {
            servicio.registrarFallo("b@epcore.co");
            reloj.avanzar(LoginAttemptService.VENTANA.dividedBy(3));
        }
        assertFalse(servicio.estaBloqueado("b@epcore.co"));
    }
}
