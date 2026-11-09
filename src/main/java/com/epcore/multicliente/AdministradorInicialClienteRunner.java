package com.epcore.multicliente;

import com.epcore.entity.Rol;
import com.epcore.entity.Usuario;
import com.epcore.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Alta de un cliente: EpCore crea únicamente su primer administrador de accesos.
 * Desde ahí, el cliente gestiona sus propios usuarios y roles. Si el
 * administrador ya existe, no se toca (no se sobrescribe su contraseña).
 */
@Component
@Order(10)
public class AdministradorInicialClienteRunner implements ApplicationRunner {

    private final ClientesConfigurados clientes;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdministradorInicialClienteRunner(ClientesConfigurados clientes, UsuarioRepository usuarioRepository,
                                             PasswordEncoder passwordEncoder) {
        this.clientes = clientes;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        MultiClienteProperties props = clientes.propiedades();
        if (!props.isHabilitado()) {
            return;
        }
        clientes.paraCadaCliente(id -> {
            MultiClienteProperties.Cliente c = props.getClientes().get(id);
            if (c == null || c.getAdminCorreo() == null || c.getAdminCorreo().isBlank()
                    || c.getAdminClave() == null || c.getAdminClave().length() < 12) {
                return; // principal o cliente sin administrador inicial configurado (clave mínima 12)
            }
            if (usuarioRepository.findByCorreo(c.getAdminCorreo().trim()).isPresent()) {
                return;
            }
            Usuario admin = new Usuario();
            admin.setNombre("Administrador de accesos - " + (c.getNombre() == null ? id : c.getNombre()));
            admin.setCorreo(c.getAdminCorreo().trim());
            admin.setPassword(passwordEncoder.encode(c.getAdminClave()));
            admin.setRol(Rol.ADMIN);
            admin.setActivo(true);
            usuarioRepository.save(admin);
        });
    }
}
