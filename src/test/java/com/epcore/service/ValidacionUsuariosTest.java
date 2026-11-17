package com.epcore.service;

import com.epcore.entity.Rol;
import com.epcore.entity.Usuario;
import com.epcore.repository.UsuarioRepository;
import com.epcore.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Validaciones en servidor del formulario de usuarios. */
class ValidacionUsuariosTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UsuarioService service = new UsuarioServiceImpl(usuarios, encoder);

    private static Usuario usuario(Long id, String correo, String clave) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setNombre("Usuario de prueba");
        u.setCorreo(correo);
        u.setPassword(clave);
        u.setRol(Rol.ANALISTA_COMPRAS);
        return u;
    }

    @Test
    void rechazaCorreoInvalido() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.guardar(usuario(null, "sin-arroba", "Clave1234")));
        assertTrue(e.getMessage().contains("correo"));
        verify(usuarios, never()).save(any());
    }

    @Test
    void rechazaCorreoDuplicadoSinImportarMayusculas() {
        when(usuarios.findByCorreo("ana@empresa.test")).thenReturn(Optional.of(usuario(7L, "ana@empresa.test", "x")));

        assertThrows(IllegalArgumentException.class,
                () -> service.guardar(usuario(null, " Ana@Empresa.test ", "Clave1234")));
        verify(usuarios, never()).save(any());
    }

    @Test
    void rechazaContrasenaDebil() {
        when(usuarios.findByCorreo(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.guardar(usuario(null, "ana@empresa.test", "1")));
        assertThrows(IllegalArgumentException.class, () -> service.guardar(usuario(null, "ana@empresa.test", "solotexto")));
        assertThrows(IllegalArgumentException.class, () -> service.guardar(usuario(null, "ana@empresa.test", "12345678")));
    }

    @Test
    void guardaUsuarioValidoConCorreoNormalizadoYClaveCifrada() {
        when(usuarios.findByCorreo(any())).thenReturn(Optional.empty());
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario guardado = service.guardar(usuario(null, " Ana@Empresa.test ", "Clave1234"));

        assertEquals("ana@empresa.test", guardado.getCorreo());
        assertTrue(encoder.matches("Clave1234", guardado.getPassword()));
    }
}
