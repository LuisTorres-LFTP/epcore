package com.epcore.security;

import com.epcore.entity.Usuario;
import com.epcore.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        for (Usuario usuario : usuarioRepository.findAll()) {
            String password = usuario.getPassword();
            if (password != null && !isBcryptHash(password)) {
                usuario.setPassword(passwordEncoder.encode(password));
                usuarioRepository.save(usuario);
            }
        }
    }

    private boolean isBcryptHash(String value) {
        return value != null && value.matches("\\$2[aby]?\\$10\\$[./A-Za-z0-9]{53}");
    }
}
