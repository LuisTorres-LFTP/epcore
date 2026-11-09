package com.epcore.config;

import com.epcore.entity.RolSistema;
import com.epcore.repository.RolSistemaRepository;
import com.epcore.security.PerfilesPredeterminados;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;

/**
 * Crea los roles predefinidos que falten. Nunca sobrescribe un rol que ya
 * existe, para respetar los ajustes que haya hecho el cliente.
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class RolesPredeterminadosRunner implements CommandLineRunner {

    private final RolSistemaRepository rolSistemaRepository;
    private final com.epcore.multicliente.ClientesConfigurados clientes;

    @Override
    public void run(String... args) {
        // En la base de cada cliente (y en la principal)
        clientes.paraCadaCliente(id -> crearFaltantes());
    }

    private void crearFaltantes() {
        PerfilesPredeterminados.todos().forEach((rol, perfil) -> {
            if (!rolSistemaRepository.existsByCodigo(rol.name())) {
                rolSistemaRepository.save(RolSistema.builder()
                        .codigo(rol.name())
                        .nombre(perfil.nombre())
                        .descripcion(perfil.descripcion())
                        .permisos(EnumSet.copyOf(perfil.permisos()))
                        .predefinido(true)
                        .activo(true)
                        .fechaModificacion(LocalDateTime.now())
                        .modificadoPor("sistema")
                        .build());
            }
        });
    }
}
