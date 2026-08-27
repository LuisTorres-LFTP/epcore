package com.epcore.config;

import com.epcore.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF se mantiene deshabilitado en esta versión porque la aplicación
                // utiliza formularios Thymeleaf y también expone una API REST.
                // Las operaciones REST se protegen mediante autenticación/JWT.
                .csrf(AbstractHttpConfigurer::disable)
                .userDetailsService(customUserDetailsService)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/error").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/**").authenticated()

                        .requestMatchers("/usuarios/**").hasRole("ADMIN")

                        .requestMatchers("/criterios/nuevo", "/criterios/guardar", "/criterios/editar/**", "/criterios/eliminar/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/criterios/**")
                        .hasAnyRole("ADMIN", "ANALISTA_COMPRAS", "AUDITOR", "GERENTE")

                        .requestMatchers("/proveedores/eliminar/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/proveedores/nuevo", "/proveedores/guardar", "/proveedores/editar/**")
                        .hasAnyRole("ADMIN", "ANALISTA_COMPRAS")

                        .requestMatchers("/proveedores/**")
                        .hasAnyRole("ADMIN", "ANALISTA_COMPRAS", "GERENTE", "AUDITOR")

                        .requestMatchers("/evaluaciones/nueva", "/evaluaciones/guardar")
                        .hasAnyRole("ADMIN", "ANALISTA_COMPRAS")

                        .requestMatchers("/evaluaciones/eliminar/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/evaluaciones/**")
                        .hasAnyRole("ADMIN", "ANALISTA_COMPRAS", "GERENTE", "AUDITOR")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> response.sendError(HttpStatus.UNAUTHORIZED.value(), "Autenticación requerida"),
                                new AntPathRequestMatcher("/api/**")
                        )
                )
                .formLogin(form -> form
                        .defaultSuccessUrl("/evaluaciones", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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
