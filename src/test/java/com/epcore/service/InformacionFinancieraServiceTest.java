package com.epcore.service;

import com.epcore.entity.InformacionFinanciera;
import com.epcore.entity.Proveedor;
import com.epcore.repository.InformacionFinancieraRepository;
import com.epcore.service.impl.InformacionFinancieraServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InformacionFinancieraServiceTest {

    private final InformacionFinancieraRepository repository =
            mock(InformacionFinancieraRepository.class);
    private final InformacionFinancieraService service =
            new InformacionFinancieraServiceImpl(repository);

    private static InformacionFinanciera estados(
            long ac, long pc, long at, long pt, long patrimonio, long uo, long gi
    ) {
        return InformacionFinanciera.builder()
                .proveedor(Proveedor.builder().idProveedor(1L).build())
                .fechaCorte(LocalDate.of(2025, 12, 31))
                .activoCorriente(BigDecimal.valueOf(ac))
                .pasivoCorriente(BigDecimal.valueOf(pc))
                .activoTotal(BigDecimal.valueOf(at))
                .pasivoTotal(BigDecimal.valueOf(pt))
                .patrimonio(BigDecimal.valueOf(patrimonio))
                .utilidadOperacional(BigDecimal.valueOf(uo))
                .gastosIntereses(BigDecimal.valueOf(gi))
                .build();
    }

    @Test
    void calculaIndicadores() {
        InformacionFinanciera info = estados(300, 150, 1000, 400, 600, 120, 40);

        assertEquals(0, new BigDecimal("2").compareTo(info.getIndiceLiquidez()));
        assertEquals(0, new BigDecimal("0.4").compareTo(info.getNivelEndeudamiento()));
        assertEquals(0, new BigDecimal("3").compareTo(info.getRazonCoberturaIntereses()));
        assertEquals(0, new BigDecimal("0.2").compareTo(info.getRentabilidadPatrimonio()));
    }

    @Test
    void indicadorIndeterminadoCuandoDivisorEsCero() {
        InformacionFinanciera info = estados(300, 0, 1000, 400, 600, 120, 0);

        assertNull(info.getIndiceLiquidez());
        assertNull(info.getRazonCoberturaIntereses());
    }

    @Test
    void verificaCapacidadFinanciera() {
        InformacionFinanciera info = estados(300, 150, 1000, 700, 300, 120, 40);

        List<String> fallas = service.verificarCapacidadFinanciera(
                info,
                new BigDecimal("1.2"),
                new BigDecimal("0.6"),
                new BigDecimal("1.5"),
                BigDecimal.valueOf(500)
        );

        assertEquals(2, fallas.size());
        assertTrue(fallas.get(0).startsWith("Nivel de endeudamiento"));
        assertTrue(fallas.get(1).startsWith("Patrimonio"));
    }

    @Test
    void cumpleCuandoLosIndicadoresSonIndeterminados() {
        InformacionFinanciera info = estados(300, 0, 1000, 400, 600, 120, 0);

        assertTrue(service.verificarCapacidadFinanciera(
                info, new BigDecimal("1.2"), new BigDecimal("0.6"),
                new BigDecimal("1.5"), null
        ).isEmpty());
    }

    @Test
    void rechazaSiNoCumpleEcuacionContable() {
        // Activo 1.000 M, pasivo 400 M, patrimonio 900 M: descuadre de 300 M.
        InformacionFinanciera info = estados(300_000_000L, 150_000_000L, 1_000_000_000L,
                400_000_000L, 900_000_000L, 120_000_000L, 40_000_000L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> service.guardar(info));
        assertTrue(ex.getMessage().contains("ecuación contable"));
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaAnioDuplicado() {
        InformacionFinanciera existente = estados(1, 1, 1, 1, 0, 0, 0);
        existente.setIdInfoFinanciera(99L);
        when(repository.findByProveedorIdProveedorAndAnioCorte(1L, 2025))
                .thenReturn(Optional.of(existente));

        InformacionFinanciera nueva = estados(300, 150, 1000, 400, 600, 120, 40);

        assertThrows(IllegalArgumentException.class, () -> service.guardar(nueva));
    }

    @Test
    void guardaYCompletaAnioYFuente() {
        when(repository.findByProveedorIdProveedorAndAnioCorte(1L, 2025))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InformacionFinanciera info = service.guardar(
                estados(300, 150, 1000, 400, 600, 120, 40));

        assertEquals(2025, info.getAnioCorte());
        assertEquals("ESTADOS_FINANCIEROS", info.getFuente());
    }
}
