package com.epcore.config;

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
 *    Thymeleaf agrega el token CSRF automáticamente en los formularios con th:action.
 */
@Configuration
public class SecurityConfig {

    private static final String[] ROLES_INTERNOS = {"ADMIN", "ANALISTA_COMPRAS", "GERENTE", "AUDITOR"};

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
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/proveedores/**").hasAnyRole("ADMIN", "ANALISTA_COMPRAS")
                        .requestMatchers(HttpMethod.PUT, "/api/proveedores/**").hasAnyRole("ADMIN", "ANALISTA_COMPRAS")
                        .requestMatchers(HttpMethod.DELETE, "/api/proveedores/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasAnyRole(ROLES_INTERNOS)
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
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/criterios/nuevo", "/criterios/guardar", "/criterios/editar/**", "/criterios/eliminar/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/criterios/**").hasAnyRole(ROLES_INTERNOS)
                        // Documentos: solo quien gestiona proveedores puede cargarlos o cambiar su estado.
                        .requestMatchers(HttpMethod.POST, "/proveedores/*/documentos/**").hasAnyRole("ADMIN", "ANALISTA_COMPRAS")
                        .requestMatchers("/proveedores/*/documentos/nuevo").hasAnyRole("ADMIN", "ANALISTA_COMPRAS")
                        .requestMatchers("/proveedores/eliminar/**").hasRole("ADMIN")
                        .requestMatchers("/proveedores/nuevo", "/proveedores/guardar", "/proveedores/editar/**")
                        .hasAnyRole("ADMIN", "ANALISTA_COMPRAS")
                        .requestMatchers("/proveedores/**").hasAnyRole(ROLES_INTERNOS)
                        .requestMatchers("/evaluaciones/nueva", "/evaluaciones/guardar").hasAnyRole("ADMIN", "ANALISTA_COMPRAS")
                        .requestMatchers("/evaluaciones/eliminar/**").hasRole("ADMIN")
                        .requestMatchers("/evaluaciones/**").hasAnyRole(ROLES_INTERNOS)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .successHandler((request, response, authentication) -> {
                            loginAttemptService.registrarExito(authentication.getName());
                            response.sendRedirect(request.getContextPath() + "/evaluaciones");
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
