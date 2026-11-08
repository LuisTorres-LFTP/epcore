package com.epcore.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protección contra adivinación de contraseñas (fuerza bruta): tras
 * {@value #MAX_FALLOS} intentos fallidos dentro de la ventana, la cuenta queda
 * bloqueada temporalmente. Un inicio de sesión exitoso reinicia el conteo.
 *
 * El conteo vive en memoria; si la aplicación corre en varios servidores
 * debe moverse a una caché compartida (por ejemplo, Redis).
 */
@Service
public class LoginAttemptService {

    public static final int MAX_FALLOS = 5;
    public static final Duration VENTANA = Duration.ofMinutes(15);
    public static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Intentos(int fallos, Instant primerFallo, Instant bloqueadoHasta) { }

    private final Map<String, Intentos> intentos = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock reloj) {
        this.reloj = reloj;
    }

    public boolean estaBloqueado(String usuario) {
        Intentos actual = intentos.get(clave(usuario));
        return actual != null
                && actual.bloqueadoHasta() != null
                && reloj.instant().isBefore(actual.bloqueadoHasta());
    }

    public void registrarFallo(String usuario) {
        Instant ahora = reloj.instant();

        intentos.compute(clave(usuario), (k, actual) -> {
            if (actual == null || ahora.isAfter(actual.primerFallo().plus(VENTANA))
                    || (actual.bloqueadoHasta() != null && !ahora.isBefore(actual.bloqueadoHasta()))) {
                return new Intentos(1, ahora, null);
            }

            int fallos = actual.fallos() + 1;
            Instant bloqueo = fallos >= MAX_FALLOS ? ahora.plus(BLOQUEO) : actual.bloqueadoHasta();
            return new Intentos(fallos, actual.primerFallo(), bloqueo);
        });
    }

    public void registrarExito(String usuario) {
        intentos.remove(clave(usuario));
    }

    private static String clave(String usuario) {
        return usuario == null ? "" : usuario.trim().toLowerCase(Locale.ROOT);
    }
}
