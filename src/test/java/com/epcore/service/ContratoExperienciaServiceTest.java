package com.epcore.service;

import com.epcore.entity.ContratoExperiencia;
import com.epcore.entity.Proveedor;
import com.epcore.repository.ContratoExperienciaRepository;
import com.epcore.service.impl.ContratoExperienciaServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContratoExperienciaServiceTest {

    private final ContratoExperienciaRepository repository =
            mock(ContratoExperienciaRepository.class);
    private final ContratoExperienciaService service =
            new ContratoExperienciaServiceImpl(repository);

    private static ContratoExperiencia contrato(
            long valor, int participacion, String unspsc, String estado, boolean certificado
    ) {
        return ContratoExperiencia.builder()
                .proveedor(Proveedor.builder().idProveedor(1L).build())
                .contratante("Banco Ejemplo S.A.")
                .objeto("Servicio de aseo")
                .valor(BigDecimal.valueOf(valor))
                .porcentajeParticipacion(BigDecimal.valueOf(participacion))
                .codigosUnspsc(unspsc)
                .fechaInicio(LocalDate.of(2023, 1, 1))
                .fechaTerminacion(LocalDate.of(2023, 12, 31))
                .estado(estado)
                .tieneCertificacion(certificado)
                .build();
    }

    @Test
    void valorAcreditableAplicaParticipacion() {
        assertEquals(0, BigDecimal.valueOf(300_000_000L).compareTo(
                contrato(600_000_000L, 50, null, "TERMINADO", true).getValorAcreditable()));
    }

    @Test
    void sumaSoloContratosAcreditables() {
        when(repository.findByProveedorIdProveedorOrderByFechaInicioDesc(1L)).thenReturn(List.of(
                contrato(100, 100, "76111500", "TERMINADO", true),
                contrato(200, 50, "76111501,47131800", "LIQUIDADO", true),
                contrato(400, 100, "76111500", "TERMINADO", false),
                contrato(800, 100, "76111500", "EN_EJECUCION", true),
                contrato(1600, 100, "43211500", "TERMINADO", true)
        ));

        // Aseo (clase 761115), solo certificados y ya ejecutados: 100 + 200 x 50 %
        assertEquals(0, BigDecimal.valueOf(200).compareTo(
                service.sumarExperienciaAcreditable(1L, "761115", true)));
        // Sin filtro de código ni certificación: 100 + 100 + 400 + 1600
        assertEquals(0, BigDecimal.valueOf(2200).compareTo(
                service.sumarExperienciaAcreditable(1L, null, false)));
    }

    @Test
    void rechazaCodigoUnspscInvalido() {
        ContratoExperiencia c = contrato(100, 100, "7611", "TERMINADO", true);

        assertThrows(IllegalArgumentException.class, () -> service.guardar(c));
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaTerminadoSinFechaDeTerminacion() {
        ContratoExperiencia c = contrato(100, 100, null, "TERMINADO", true);
        c.setFechaTerminacion(null);

        assertThrows(IllegalArgumentException.class, () -> service.guardar(c));
    }

    @Test
    void normalizaCodigosAlGuardar() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContratoExperiencia guardado = service.guardar(
                contrato(100, 100, " 76111500 , 47131800,", "terminado", true));

        assertEquals("76111500,47131800", guardado.getCodigosUnspsc());
        assertEquals("TERMINADO", guardado.getEstado());
    }
}
