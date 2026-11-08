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
@WebMvcTest(controllers = {UsuarioController.class, UsuarioWebController.class, com.epcore.controller.RolSistemaController.class})
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

    @MockitoBean
    private com.epcore.service.RolSistemaService rolSistemaService;

    private UserDetails auditor;

    @BeforeEach
    void preparar() {
        auditor = usr("auditor@epcore.co", com.epcore.entity.Rol.AUDITOR);
        when(userDetailsService.loadUserByUsername("auditor@epcore.co")).thenReturn(auditor);
        when(userDetailsService.loadUserByUsername("admin@epcore.co")).thenReturn(
                usr("admin@epcore.co", com.epcore.entity.Rol.ADMIN));
    }

    /** Usuario de prueba con ROLE_<rol> y los permisos predeterminados del rol. */
    static UserDetails usr(String correo, com.epcore.entity.Rol rol) {
        java.util.List<String> auts = new java.util.ArrayList<>();
        auts.add("ROLE_" + rol.name());
        PerfilesPredeterminados.permisosDe(rol).forEach(p -> auts.add(p.name()));
        return User.withUsername(correo).password("x").authorities(auts.toArray(String[]::new)).build();
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
        mvc.perform(get("/usuarios").with(user(usr("auditor@epcore.co", com.epcore.entity.Rol.AUDITOR))))
                .andExpect(status().isForbidden());
    }

    // ---------- Falsificación de peticiones (CSRF) ----------

    @Test
    @DisplayName("CSRF: un sitio externo envía el formulario de usuarios con la sesión del ADMIN: debe responder 403")
    void formularioSinTokenCsrf() throws Exception {
        mvc.perform(post("/usuarios/guardar").with(user(usr("admin@epcore.co", com.epcore.entity.Rol.ADMIN)))
                        .param("nombre", "Atacante").param("correo", "atacante@epcore.co")
                        .param("password", "Clave1234").param("rol", "ADMIN").param("activo", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF por enlace: inactivar un usuario con un GET (por ejemplo, una imagen en un correo): no debe permitirse")
    void cambioDeEstadoPorGet() throws Exception {
        mvc.perform(get("/usuarios/inactivar/1").with(user(usr("admin@epcore.co", com.epcore.entity.Rol.ADMIN))))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---------- Comprobaciones de que el uso legítimo sigue funcionando ----------

    @Test
    @DisplayName("Uso legítimo: el ADMIN envía el formulario con token CSRF y sí se procesa")
    void formularioConTokenCsrfFunciona() throws Exception {
        mvc.perform(post("/usuarios/guardar").with(user(usr("admin@epcore.co", com.epcore.entity.Rol.ADMIN))).with(csrf())
                        .param("nombre", "Nueva Analista").param("correo", "analista@epcore.co")
                        .param("password", "Clave1234").param("rol", "ANALISTA_COMPRAS").param("activo", "true"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Uso legítimo: el ADMIN con token JWT sí puede listar usuarios por la API")
    void adminConTokenListaUsuarios() throws Exception {
        String token = jwtService.generateToken(
                usr("admin@epcore.co", com.epcore.entity.Rol.ADMIN));

        mvc.perform(get("/api/usuarios").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("La API no acepta la sesión web (cookie) en lugar del token")
    void apiNoAceptaSesionWeb() throws Exception {
        // Sesión web real (como la que deja el inicio de sesión del navegador)
        UserDetails admin = usr("admin@epcore.co", com.epcore.entity.Rol.ADMIN);
        org.springframework.mock.web.MockHttpSession sesion = new org.springframework.mock.web.MockHttpSession();
        sesion.setAttribute("SPRING_SECURITY_CONTEXT", new org.springframework.security.core.context.SecurityContextImpl(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        admin, null, admin.getAuthorities())));

        // La misma sesión sí abre la parte web...
        mvc.perform(get("/usuarios").session(sesion)).andExpect(status().isOk());
        // ...pero la API la ignora y exige el token.
        mvc.perform(get("/api/usuarios").session(sesion)).andExpect(status().isUnauthorized());
    }

    // ---------- Roles nuevos y permisos ----------

    @Test
    @DisplayName("Un ADMIN no puede aprobar documentos (separación de funciones)")
    void adminNoApruebaDocumentos() throws Exception {
        mvc.perform(post("/proveedores/1/documentos/1/estado")
                        .with(user(usr("admin@epcore.co", com.epcore.entity.Rol.ADMIN)))
                        .with(csrf()).param("estadoValidacion", "APROBADO"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un EVALUADOR_TECNICO no puede gestionar usuarios")
    void evaluadorTecnicoNoAdministra() throws Exception {
        mvc.perform(get("/usuarios").with(user(usr("tec@epcore.co", com.epcore.entity.Rol.EVALUADOR_TECNICO))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un usuario PROVEEDOR no entra a la parte interna")
    void proveedorNoEntraALaParteInterna() throws Exception {
        mvc.perform(get("/usuarios").with(user(usr("prov@empresa.co", com.epcore.entity.Rol.PROVEEDOR))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API: solo quien tiene ROLES_GESTIONAR consulta los roles")
    void apiRolesSoloAdministrador() throws Exception {
        String tokenAuditor = jwtService.generateToken(auditor);
        mvc.perform(get("/api/roles").header("Authorization", bearer(tokenAuditor)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API: una ruta no declarada se niega por defecto")
    void apiRutaNoDeclaradaSeNiega() throws Exception {
        String token = jwtService.generateToken(usr("admin@epcore.co", com.epcore.entity.Rol.ADMIN));
        mvc.perform(get("/api/ruta-que-no-existe").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }
}
