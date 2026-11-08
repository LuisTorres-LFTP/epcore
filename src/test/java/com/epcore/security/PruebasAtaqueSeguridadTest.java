package com.epcore.security;

import com.epcore.config.JwtAuthenticationFilter;
import com.epcore.config.JwtService;
import com.epcore.config.SecurityConfig;
import com.epcore.controller.UsuarioController;
import com.epcore.controller.UsuarioWebController;
import com.epcore.service.UsuarioService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de ataque: cada prueba simula a un atacante y verifica que
 * EpCore lo bloquee. Usa la configuración de seguridad real, sin base de datos.
 */
@WebMvcTest(controllers = {UsuarioController.class, UsuarioWebController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, LoginAttemptService.class})
@TestPropertySource(properties = "app.jwt.secret=" + PruebasAtaqueSeguridadTest.SECRETO)
class PruebasAtaqueSeguridadTest {

    static final String SECRETO = "clave-secreta-solo-para-pruebas-de-seguridad-32+";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private UsuarioService usuarioService;

    private UserDetails auditor;

    @BeforeEach
    void preparar() {
        auditor = User.withUsername("auditor@epcore.co").password("x").roles("AUDITOR").build();
        when(userDetailsService.loadUserByUsername("auditor@epcore.co")).thenReturn(auditor);
        when(userDetailsService.loadUserByUsername("admin@epcore.co")).thenReturn(
                User.withUsername("admin@epcore.co").password("x").roles("ADMIN").build());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- Autenticación de la API ----------

    @Test
    @DisplayName("API sin token: debe responder 401")
    void apiSinToken() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token falsificado con otra clave: debe responder 401")
    void tokenFalsificado() throws Exception {
        String falso = Jwts.builder().subject("admin@epcore.co")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("otra-clave-del-atacante-de-mas-de-32-bytes".getBytes(StandardCharsets.UTF_8)))
                .compact();

        mvc.perform(get("/api/usuarios").header("Authorization", bearer(falso)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token vencido: debe responder 401")
    void tokenVencido() throws Exception {
        String vencido = Jwts.builder().subject("admin@epcore.co")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mvc.perform(get("/api/usuarios").header("Authorization", bearer(vencido)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token alterado (se cambia un carácter de la firma): debe responder 401")
    void tokenAlterado() throws Exception {
        String token = jwtService.generateToken(auditor);
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        mvc.perform(get("/api/usuarios").header("Authorization", bearer(alterado)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Escalada de privilegios ----------

    @Test
    @DisplayName("Un AUDITOR intenta crearse un usuario ADMIN por la API: debe responder 403")
    void auditorNoPuedeCrearAdministradores() throws Exception {
        String token = jwtService.generateToken(auditor);
        String cuerpo = """
                {"nombre":"Atacante","correo":"atacante@epcore.co","password":"Clave1234",
                 "rol":"ADMIN","activo":true}
                """;

        mvc.perform(post("/api/usuarios").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un AUDITOR intenta listar todos los usuarios por la API: debe responder 403")
    void auditorNoPuedeListarUsuarios() throws Exception {
        String token = jwtService.generateToken(auditor);

        mvc.perform(get("/api/usuarios").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un AUDITOR intenta abrir la gestión web de usuarios: debe responder 403")
    void auditorNoEntraAGestionDeUsuarios() throws Exception {
        mvc.perform(get("/usuarios").with(user("auditor@epcore.co").roles("AUDITOR")))
                .andExpect(status().isForbidden());
    }

    // ---------- Falsificación de peticiones (CSRF) ----------

    @Test
    @DisplayName("CSRF: un sitio externo envía el formulario de usuarios con la sesión del ADMIN: debe responder 403")
    void formularioSinTokenCsrf() throws Exception {
        mvc.perform(post("/usuarios/guardar").with(user("admin@epcore.co").roles("ADMIN"))
                        .param("nombre", "Atacante").param("correo", "atacante@epcore.co")
                        .param("password", "Clave1234").param("rol", "ADMIN").param("activo", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF por enlace: inactivar un usuario con un GET (por ejemplo, una imagen en un correo): no debe permitirse")
    void cambioDeEstadoPorGet() throws Exception {
        mvc.perform(get("/usuarios/inactivar/1").with(user("admin@epcore.co").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---------- Comprobaciones de que el uso legítimo sigue funcionando ----------

    @Test
    @DisplayName("Uso legítimo: el ADMIN envía el formulario con token CSRF y sí se procesa")
    void formularioConTokenCsrfFunciona() throws Exception {
        mvc.perform(post("/usuarios/guardar").with(user("admin@epcore.co").roles("ADMIN")).with(csrf())
                        .param("nombre", "Nueva Analista").param("correo", "analista@epcore.co")
                        .param("password", "Clave1234").param("rol", "ANALISTA_COMPRAS").param("activo", "true"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Uso legítimo: el ADMIN con token JWT sí puede listar usuarios por la API")
    void adminConTokenListaUsuarios() throws Exception {
        String token = jwtService.generateToken(
                User.withUsername("admin@epcore.co").password("x").roles("ADMIN").build());

        mvc.perform(get("/api/usuarios").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("La API no acepta la sesión web (cookie) en lugar del token")
    void apiNoAceptaSesionWeb() throws Exception {
        // Sesión web real (como la que deja el inicio de sesión del navegador)
        UserDetails admin = User.withUsername("admin@epcore.co").password("x").roles("ADMIN").build();
        org.springframework.mock.web.MockHttpSession sesion = new org.springframework.mock.web.MockHttpSession();
        sesion.setAttribute("SPRING_SECURITY_CONTEXT", new org.springframework.security.core.context.SecurityContextImpl(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        admin, null, admin.getAuthorities())));

        // La misma sesión sí abre la parte web...
        mvc.perform(get("/usuarios").session(sesion)).andExpect(status().isOk());
        // ...pero la API la ignora y exige el token.
        mvc.perform(get("/api/usuarios").session(sesion)).andExpect(status().isUnauthorized());
    }
}
