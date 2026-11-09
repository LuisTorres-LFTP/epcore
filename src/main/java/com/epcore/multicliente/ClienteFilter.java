package com.epcore.multicliente;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * Identifica el cliente por el subdominio (bancocafetero.epcore.co -> "bancocafetero")
 * y amarra la sesión a ese cliente: una sesión iniciada en un cliente no sirve en otro.
 * El cliente nunca se toma de un parámetro o cabecera que el usuario pueda cambiar.
 */
public class ClienteFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_SESION = "EPCORE_CLIENTE";

    private final MultiClienteProperties props;

    public ClienteFilter(MultiClienteProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cliente;
        try {
            cliente = resolver(request.getServerName());
        } catch (IllegalArgumentException desconocido) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Cliente no encontrado");
            return;
        }

        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            Object deSesion = sesion.getAttribute(ATRIBUTO_SESION);
            if (deSesion != null && !deSesion.equals(cliente)) {
                // Intento de usar la sesión de otro cliente: se descarta.
                sesion.invalidate();
            } else if (deSesion == null) {
                sesion.setAttribute(ATRIBUTO_SESION, cliente);
            }
        }

        try {
            ClienteContexto.establecer(cliente);
            chain.doFilter(request, response);
        } finally {
            ClienteContexto.limpiar();
        }
    }

    /** Devuelve el cliente para el host; lanza IllegalArgumentException si no existe. */
    public String resolver(String host) {
        if (!props.isHabilitado() || host == null) {
            return ClienteContexto.PRINCIPAL;
        }
        String h = host.toLowerCase(Locale.ROOT);
        String sufijo = "." + props.getDominioBase().toLowerCase(Locale.ROOT);
        if (!h.endsWith(sufijo)) {
            return ClienteContexto.PRINCIPAL; // dominio raíz, localhost o IP: base principal de EpCore
        }
        String id = h.substring(0, h.length() - sufijo.length());
        if (id.equals("www")) {
            return ClienteContexto.PRINCIPAL;
        }
        if (!MultiClienteProperties.ID_VALIDO.matcher(id).matches() || !props.getClientes().containsKey(id)) {
            throw new IllegalArgumentException("Cliente no encontrado: " + id);
        }
        return id;
    }
}
