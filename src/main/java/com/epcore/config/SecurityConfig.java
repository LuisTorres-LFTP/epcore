package com.epcore.config;

import com.epcore.entity.Permiso;
import com.epcore.security.CustomUserDetailsService;
import com.epcore.security.LoginAttemptService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Dos cadenas de seguridad independientes:
 *
 * 1. API REST (/api/**): sin sesión ni cookies, solo token JWT en la cabecera.
 *    Como no usa cookies, no es vulnerable a CSRF y por eso CSRF se desactiva
 *    únicamente aquí.
 * 2. Aplicación web (Thymeleaf): sesión con cookie y protección CSRF activa.
 *
 * El acceso se decide por PERMISOS (no por nombre de rol), así cada cliente
 * puede crear roles propios combinando permisos sin tocar el código.
 *    Thymeleaf agrega el token CSRF automáticamente en los formularios con th:action.
 */
@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginAttemptService loginAttemptService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          LoginAttemptService loginAttemptService) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.loginAttemptService = loginAttemptService;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .userDetailsService(customUserDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/usuarios/**").hasAuthority(Permiso.USUARIOS_GESTIONAR.name())
                        .requestMatchers("/api/roles/**").hasAuthority(Permiso.ROLES_GESTIONAR.name())
                        .requestMatchers(HttpMethod.POST, "/api/proveedores/**").hasAuthority(Permiso.PROVEEDORES_GESTIONAR.name())
                        .requestMatchers(HttpMethod.PUT, "/api/proveedores/**").hasAuthority(Permiso.PROVEEDORES_GESTIONAR.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/proveedores/**").hasAuthority(Permiso.PROVEEDORES_ELIMINAR.name())
                        .requestMatchers(HttpMethod.GET, "/api/proveedores/**").hasAuthority(Permiso.PROVEEDORES_VER.name())
                        // Todo lo que no esté listado se niega por defecto.
                        .anyRequest().denyAll()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .userDetailsService(customUserDetailsService)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/error").permitAll()
                        .requestMatchers("/usuarios/**").hasAuthority(Permiso.USUARIOS_GESTIONAR.name())
                        .requestMatchers("/roles/**").hasAuthority(Permiso.ROLES_GESTIONAR.name())
                        .requestMatchers("/criterios/nuevo", "/criterios/guardar", "/criterios/editar/**", "/criterios/eliminar/**")
                        .hasAuthority(Permiso.CATALOGOS_GESTIONAR.name())
                        .requestMatchers("/criterios/**").hasAnyAuthority(Permiso.EVALUACIONES_VER.name(), Permiso.CATALOGOS_GESTIONAR.name())
                        // Documentos: aprobar exige permiso de aprobación; cargar o inactivar, permiso de carga.
                        .requestMatchers(HttpMethod.POST, "/proveedores/*/documentos/*/estado")
                        .hasAnyAuthority(Permiso.DOCUMENTOS_APROBAR_NIVEL_1.name(), Permiso.DOCUMENTOS_APROBAR_NIVEL_2.name())
                        .requestMatchers(HttpMethod.POST, "/proveedores/*/documentos/**").hasAuthority(Permiso.DOCUMENTOS_CARGAR.name())
                        .requestMatchers("/proveedores/*/documentos/nuevo").hasAuthority(Permiso.DOCUMENTOS_CARGAR.name())
                        .requestMatchers("/proveedores/eliminar/**").hasAuthority(Permiso.PROVEEDORES_ELIMINAR.name())
                        .requestMatchers("/proveedores/nuevo", "/proveedores/guardar", "/proveedores/editar/**")
                        .hasAuthority(Permiso.PROVEEDORES_GESTIONAR.name())
                        .requestMatchers("/proveedores/**").hasAuthority(Permiso.PROVEEDORES_VER.name())
                        .requestMatchers("/evaluaciones/nueva", "/evaluaciones/guardar").hasAuthority(Permiso.EVALUACIONES_REGISTRAR.name())
                        .requestMatchers("/evaluaciones/eliminar/**").hasAuthority(Permiso.EVALUACIONES_ELIMINAR.name())
                        .requestMatchers("/evaluaciones/**").hasAuthority(Permiso.EVALUACIONES_VER.name())
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .successHandler((request, response, authentication) -> {
                            loginAttemptService.registrarExito(authentication.getName());
                            boolean veEvaluaciones = authentication.getAuthorities().stream()
                                    .anyMatch(a -> Permiso.EVALUACIONES_VER.name().equals(a.getAuthority()));
                            response.sendRedirect(request.getContextPath()
                                    + (veEvaluaciones ? "/evaluaciones" : "/proveedores"));
                        })
                        .failureHandler((request, response, exception) -> {
                            if (exception instanceof BadCredentialsException) {
                                loginAttemptService.registrarFallo(request.getParameter("username"));
                            }
                            String motivo = exception instanceof LockedException ? "bloqueado" : "error";
                            response.sendRedirect(request.getContextPath() + "/login?" + motivo);
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * El filtro JWT solo debe ejecutarse dentro de la cadena de la API,
     * no como filtro global del servidor.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(filter);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
