package com.epcore.service;

import com.epcore.entity.Criterio;
import com.epcore.entity.Proveedor;
import com.epcore.repository.CriterioRepository;
import com.epcore.repository.ProveedorRepository;
import com.epcore.service.impl.CriterioServiceImpl;
import com.epcore.service.impl.ProveedorServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Validaciones en servidor de proveedores y criterios (defectos BUG-001, BUG-002, BUG-003 y BUG-005). */
class ValidacionProveedoresYCriteriosTest {

    private final ProveedorRepository proveedores = mock(ProveedorRepository.class);
    private final ProveedorService proveedorService = new ProveedorServiceImpl(proveedores);

    private final CriterioRepository criterios = mock(CriterioRepository.class);
    private final CriterioService criterioService = new CriterioServiceImpl(criterios);

    private static Proveedor proveedor(Long id, String nit) {
        return Proveedor.builder().idProveedor(id).tipoPersona("JURIDICA").nit(nit)
                .razonSocial("Proveedor de prueba S.A.S.").correo("contacto@prueba.test").build();
    }

    private static Criterio criterio(Long id, int peso, boolean activo) {
        return Criterio.builder().idCriterio(id).nombreCriterio("Criterio " + id).peso(peso).activo(activo).build();
    }

    @Test
    void rechazaNitDuplicadoConMensajeClaro() {
        when(proveedores.findByNit("900999001-1")).thenReturn(Optional.of(proveedor(13L, "900999001-1")));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> proveedorService.guardarProveedor(proveedor(null, "900999001-1")));

        assertTrue(e.getMessage().contains("Ya existe un proveedor registrado con el NIT"));
        verify(proveedores, never()).save(any());
    }

    @Test
    void permiteEditarElMismoProveedorSinCambiarSuNit() {
        Proveedor existente = proveedor(13L, "900999001-1");
        when(proveedores.findByNit("900999001-1")).thenReturn(Optional.of(existente));
        when(proveedores.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> proveedorService.guardarProveedor(proveedor(13L, "900999001-1")));
    }

    @Test
    void rechazaNitVacioYQuitaEspaciosDelNit() {
        assertThrows(IllegalArgumentException.class, () -> proveedorService.guardarProveedor(proveedor(null, "  ")));

        when(proveedores.findByNit(any())).thenReturn(Optional.empty());
        when(proveedores.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("901234567", proveedorService.guardarProveedor(proveedor(null, " 901234567 ")).getNit());
    }

    @Test
    void rechazaRazonSocialYCorreoVacios() {
        Proveedor sinRazonSocial = proveedor(null, "901234567");
        sinRazonSocial.setRazonSocial("");
        Proveedor sinCorreo = proveedor(null, "901234567");
        sinCorreo.setCorreo(null);

        assertThrows(IllegalArgumentException.class, () -> proveedorService.guardarProveedor(sinRazonSocial));
        assertThrows(IllegalArgumentException.class, () -> proveedorService.guardarProveedor(sinCorreo));
        verify(proveedores, never()).save(any());
    }

    @Test
    void elPesoDelCriterioDebeEstarEntre1Y100() {
        when(criterios.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThrows(IllegalArgumentException.class, () -> criterioService.guardar(criterio(null, 0, true)));
        assertThrows(IllegalArgumentException.class, () -> criterioService.guardar(criterio(null, 101, true)));
        assertThrows(IllegalArgumentException.class, () -> criterioService.guardar(criterio(null, 150, true)));
        assertDoesNotThrow(() -> criterioService.guardar(criterio(null, 1, true)));
        assertDoesNotThrow(() -> criterioService.guardar(criterio(null, 100, true)));
    }

    @Test
    void sumaSoloLosPesosDeLosCriteriosActivos() {
        when(criterios.findByActivoTrue()).thenReturn(List.of(criterio(1L, 30, true), criterio(2L, 25, true),
                criterio(3L, 20, true), criterio(4L, 15, true), criterio(5L, 10, true)));

        assertEquals(CriterioService.SUMA_PESOS_REQUERIDA, criterioService.sumaPesosActivos());
    }
}
