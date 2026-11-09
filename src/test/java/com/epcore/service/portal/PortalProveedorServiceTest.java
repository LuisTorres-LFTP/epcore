package com.epcore.service.portal;

import com.epcore.entity.Proveedor;
import com.epcore.entity.Rol;
import com.epcore.entity.Usuario;
import com.epcore.entity.proceso.Proceso;
import com.epcore.entity.proceso.ProcesoEnums.EstadoProceso;
import com.epcore.entity.proceso.ProcesoEnums.Visibilidad;
import com.epcore.repository.ProveedorRepository;
import com.epcore.repository.UsuarioRepository;
import com.epcore.repository.proceso.OfertaRepository;
import com.epcore.repository.proceso.ProcesoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PortalProveedorServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ProveedorRepository proveedores = mock(ProveedorRepository.class);
    private final PortalProveedorService portal = new PortalProveedorService(usuarios, proveedores,
            mock(ProcesoRepository.class), mock(OfertaRepository.class), new BCryptPasswordEncoder());

    private static PortalProveedorService.Registro registro(String nit, String clave, boolean autoriza) {
        return new PortalProveedorService.Registro("Servicios del Eje SAS", nit, "Armenia", "Marta Ruiz",
                "3105550000", "contacto@sgeje.co", clave, clave, autoriza);
    }

    @Test
    void calculaElDigitoDeVerificacionDeLaDian() {
        assertEquals(7, PortalProveedorService.digitoVerificacion("901234567"));
        assertEquals(0, PortalProveedorService.digitoVerificacion("900640118"));
        assertEquals(8, PortalProveedorService.digitoVerificacion("890903938"));
    }

    @Test
    void registraProveedorYUsuarioDelPortal() {
        when(proveedores.findByNit(any())).thenReturn(Optional.empty());
        when(usuarios.findByCorreo(any())).thenReturn(Optional.empty());
        when(proveedores.save(any())).thenAnswer(i -> { Proveedor p = i.getArgument(0); p.setIdProveedor(20L); return p; });
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario u = portal.registrar(registro("901234567-7", "Clave2026x", true));

        assertEquals(Rol.PROVEEDOR, u.getRol());
        assertEquals(20L, u.getProveedor().getIdProveedor());
        assertEquals("901.234.567-7", u.getProveedor().getNit());
        assertEquals("PENDIENTE_VERIFICACION", u.getProveedor().getEstado());
        assertNotEquals("Clave2026x", u.getPassword(), "la contraseña se guarda cifrada");
    }

    @Test
    void rechazaDigitoDeVerificacionIncorrectoClaveDebilYSinAutorizacion() {
        when(usuarios.findByCorreo(any())).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> portal.registrar(registro("901234567-3", "corta", false)));
        assertTrue(ex.getMessage().contains("dígito de verificación"));
        assertTrue(ex.getMessage().contains("contraseña"));
        assertTrue(ex.getMessage().contains("Ley 1581"));
        verify(usuarios, never()).save(any());
    }

    @Test
    void noPermiteNitDuplicado() {
        when(proveedores.findByNit("901.234.567-7")).thenReturn(Optional.of(new Proveedor()));
        when(usuarios.findByCorreo(any())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> portal.registrar(registro("901234567-7", "Clave2026x", true)));
    }

    private static Proceso proceso(EstadoProceso estado, Visibilidad vis, Long... invitados) {
        Proceso p = Proceso.builder().estado(estado).visibilidad(vis).build();
        for (Long id : invitados) p.getInvitados().add(Proveedor.builder().idProveedor(id).build());
        return p;
    }

    @Test
    void reglasDeVisibilidad() {
        assertTrue(portal.puedeVer(proceso(EstadoProceso.PUBLICADO, Visibilidad.ABIERTO), 5L));
        assertTrue(portal.puedeVer(proceso(EstadoProceso.PUBLICADO, Visibilidad.SECOP), 5L));
        assertTrue(portal.puedeVer(proceso(EstadoProceso.PUBLICADO, Visibilidad.INVITACION, 5L), 5L));
        assertFalse(portal.puedeVer(proceso(EstadoProceso.PUBLICADO, Visibilidad.INVITACION, 6L), 5L), "no invitado");
        assertFalse(portal.puedeVer(proceso(EstadoProceso.BORRADOR, Visibilidad.ABIERTO), 5L), "nunca ve borradores");
        assertFalse(portal.puedeVer(proceso(EstadoProceso.EN_APROBACION, Visibilidad.ABIERTO), 5L));
    }

    @Test
    void soloConoceSuPropioResultadoSinRevelarAlGanador() {
        Proceso p = proceso(EstadoProceso.ADJUDICADO, Visibilidad.ABIERTO);
        p.setAdjudicadoA(Proveedor.builder().idProveedor(9L).build());
        assertEquals("Adjudicado a su empresa", portal.miResultado(p, 9L, true));
        assertEquals("No adjudicado", portal.miResultado(p, 5L, true));
        assertNull(portal.miResultado(p, 5L, false), "sin oferta no ve resultado");
    }

    @Test
    void unUsuarioInternoNoEsProveedor() {
        Usuario interno = new Usuario();
        interno.setRol(Rol.ADMIN);
        when(usuarios.findByCorreo("admin@epcore.co")).thenReturn(Optional.of(interno));
        assertThrows(IllegalStateException.class, () -> portal.proveedorDe("admin@epcore.co"));
    }
}
