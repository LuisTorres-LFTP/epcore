package com.epcore.multicliente;

import com.epcore.config.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MultiClienteTest {

    private static MultiClienteProperties props() {
        MultiClienteProperties p = new MultiClienteProperties();
        p.setHabilitado(true);
        p.setDominioBase("epcore.co");
        p.getClientes().put("bancocafetero", new MultiClienteProperties.Cliente());
        p.getClientes().put("cooperativa-eje", new MultiClienteProperties.Cliente());
        return p;
    }

    @AfterEach
    void limpiar() {
        ClienteContexto.limpiar();
    }

    @Test
    void resuelveElClientePorSubdominio() {
        ClienteFilter f = new ClienteFilter(props());
        assertEquals("bancocafetero", f.resolver("BancoCafetero.epcore.co"));
        assertEquals("cooperativa-eje", f.resolver("cooperativa-eje.epcore.co"));
        assertEquals(ClienteContexto.PRINCIPAL, f.resolver("epcore.co"));
        assertEquals(ClienteContexto.PRINCIPAL, f.resolver("www.epcore.co"));
        assertEquals(ClienteContexto.PRINCIPAL, f.resolver("localhost"));
        assertThrows(IllegalArgumentException.class, () -> f.resolver("otrobanco.epcore.co"));
        assertThrows(IllegalArgumentException.class, () -> f.resolver("a.b.epcore.co"));
    }

    @Test
    void sinMultiClienteTodoVaALaBasePrincipal() {
        MultiClienteProperties p = props();
        p.setHabilitado(false);
        assertEquals(ClienteContexto.PRINCIPAL, new ClienteFilter(p).resolver("bancocafetero.epcore.co"));
    }

    @Test
    void elFiltroFijaElClienteDuranteLaPeticionYLoLimpiaAlFinal() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServerName("bancocafetero.epcore.co");
        AtomicReference<String> visto = new AtomicReference<>();
        new ClienteFilter(props()).doFilter(req, new MockHttpServletResponse(),
                (rq, rs) -> visto.set(ClienteContexto.actual()));
        assertEquals("bancocafetero", visto.get());
        assertEquals(ClienteContexto.PRINCIPAL, ClienteContexto.actual(), "no queda contaminado el hilo");
    }

    @Test
    void clienteDesconocidoRespondeNoEncontrado() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServerName("intruso.epcore.co");
        MockHttpServletResponse res = new MockHttpServletResponse();
        new ClienteFilter(props()).doFilter(req, res, new MockFilterChain());
        assertEquals(404, res.getStatus());
    }

    @Test
    void unaSesionDeUnClienteNoSirveEnOtro() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(ClienteFilter.ATRIBUTO_SESION, "bancocafetero");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServerName("cooperativa-eje.epcore.co");
        req.setSession(sesion);

        new ClienteFilter(props()).doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        assertTrue(sesion.isInvalid(), "la sesión del otro cliente se descarta");
    }

    @Test
    void unTokenEmitidoParaUnClienteNoValeEnOtro() {
        JwtService jwt = new JwtService("clave-de-pruebas-multicliente-de-mas-de-32-bytes", 60_000);
        UserDetails u = User.withUsername("admin@bancocafetero.co").password("x").roles("ADMIN").build();

        String[] token = new String[1];
        ClienteContexto.ejecutarComo("bancocafetero", () -> token[0] = jwt.generateToken(u));

        ClienteContexto.ejecutarComo("bancocafetero", () -> assertTrue(jwt.isTokenValid(token[0], u)));
        ClienteContexto.ejecutarComo("cooperativa-eje", () -> assertFalse(jwt.isTokenValid(token[0], u)));
        assertFalse(jwt.isTokenValid(token[0], u), "tampoco vale en la base principal");
    }

    @Test
    void elEnrutadorConectaALaBaseDelClienteYNuncaAOtra() {
        DataSource principal = mock(DataSource.class);
        DataSource banco = mock(DataSource.class);
        ClienteRoutingDataSource routing = new ClienteRoutingDataSource() {
            DataSource destino() { return determineTargetDataSource(); }
        };
        routing.setTargetDataSources(Map.of(ClienteContexto.PRINCIPAL, principal, "bancocafetero", banco));
        routing.setDefaultTargetDataSource(principal);
        routing.afterPropertiesSet();

        ClienteContexto.ejecutarComo("bancocafetero",
                () -> assertSame(banco, ((ClienteRoutingDataSourceAccesor) () -> routing).destino()));
        assertSame(principal, ((ClienteRoutingDataSourceAccesor) () -> routing).destino());
        ClienteContexto.ejecutarComo("desconocido", () -> assertThrows(IllegalStateException.class,
                () -> ((ClienteRoutingDataSourceAccesor) () -> routing).destino()));
    }

    /** Acceso al método protegido determineTargetDataSource para la prueba. */
    @FunctionalInterface
    interface ClienteRoutingDataSourceAccesor {
        ClienteRoutingDataSource routing();

        default DataSource destino() {
            try {
                var m = org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource.class
                        .getDeclaredMethod("determineTargetDataSource");
                m.setAccessible(true);
                return (DataSource) m.invoke(routing());
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw (RuntimeException) e.getCause();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
