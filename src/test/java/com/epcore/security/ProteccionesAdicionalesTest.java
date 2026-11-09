package com.epcore.security;

import com.epcore.config.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;


import static org.junit.jupiter.api.Assertions.*;

class ProteccionesAdicionalesTest {

    @Test
    void rechazaLaClaveJwtQueEstuvoPublicadaEnElRepositorio() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new JwtService("change-this-development-secret-key-at-least-32-chars", 60_000));
        assertTrue(ex.getMessage().contains("clave de ejemplo"));
    }

    @Test
    void sinClaveConfiguradaGeneraUnaAleatoriaYFunciona() {
        JwtService servicio = new JwtService("", 60_000);
        assertNotNull(servicio.generateToken(
                org.springframework.security.core.userdetails.User.withUsername("a@epcore.co")
                        .password("x").roles("ADMIN").build()));
    }

    private static boolean contenidoValido(String nombre, byte[] datos, String extension) {
        return com.epcore.service.CargaDocumentoService.contenidoCoincide(
                new MockMultipartFile("archivo", nombre, null, datos), extension);
    }

    @Test
    void aceptaUnPdfReal() throws Exception {
        assertTrue(contenidoValido("rut.pdf", "%PDF-1.7\n...".getBytes(), "pdf"));
    }

    @Test
    void rechazaUnEjecutableDisfrazadoDePdf() throws Exception {
        byte[] ejecutableWindows = {0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00}; // "MZ"
        assertFalse(contenidoValido("rut.pdf", ejecutableWindows, "pdf"));
    }

    @Test
    void rechazaUnHtmlDisfrazadoDeImagen() throws Exception {
        assertFalse(contenidoValido("foto.png", "<script>alert(1)</script>".getBytes(), "png"));
    }
}
