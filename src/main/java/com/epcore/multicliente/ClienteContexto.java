package com.epcore.multicliente;

/**
 * Cliente (empresa que contrata EpCore) de la petición en curso. Cada cliente
 * tiene su propia base de datos; este valor decide a cuál se conecta EpCore.
 */
public final class ClienteContexto {

    /** Base de datos por defecto (instalación de un solo cliente). */
    public static final String PRINCIPAL = "principal";

    private static final ThreadLocal<String> ACTUAL = new ThreadLocal<>();

    private ClienteContexto() {
    }

    public static String actual() {
        String c = ACTUAL.get();
        return c == null ? PRINCIPAL : c;
    }

    public static void establecer(String cliente) {
        ACTUAL.set(cliente);
    }

    public static void limpiar() {
        ACTUAL.remove();
    }

    /** Ejecuta una tarea dentro del contexto de un cliente y restaura el anterior. */
    public static void ejecutarComo(String cliente, Runnable tarea) {
        String anterior = ACTUAL.get();
        try {
            ACTUAL.set(cliente);
            tarea.run();
        } finally {
            if (anterior == null) ACTUAL.remove(); else ACTUAL.set(anterior);
        }
    }
}
