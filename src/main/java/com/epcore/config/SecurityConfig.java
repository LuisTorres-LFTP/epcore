package com.epcore.config;

import com.epcore.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .userDetailsService(customUserDetailsService)
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

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
                .formLogin(form -> form
                        .defaultSuccessUrl("/evaluaciones", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}