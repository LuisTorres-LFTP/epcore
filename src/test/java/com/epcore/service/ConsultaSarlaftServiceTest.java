package com.epcore.service;

import com.epcore.entity.ConsultaSarlaft;
import com.epcore.entity.EstadoSarlaft;
import com.epcore.entity.ListaRestrictiva;
import com.epcore.entity.Proveedor;
import com.epcore.entity.ResultadoSarlaft;
import com.epcore.repository.ConsultaSarlaftRepository;
import com.epcore.service.impl.ConsultaSarlaftServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConsultaSarlaftServiceTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 8, 10, 0);

    private final ConsultaSarlaftRepository repository = mock(ConsultaSarlaftRepository.class);
    private final ConsultaSarlaftService service = new ConsultaSarlaftServiceImpl(repository);

    private void responder(ListaRestrictiva lista, ResultadoSarlaft resultado, int diasAtras) {
        ConsultaSarlaft consulta = ConsultaSarlaft.builder()
                .lista(lista)
                .resultado(resultado)
                .fechaConsulta(HOY.minusDays(diasAtras))
                .build();
        when(repository.findFirstByProveedorIdProveedorAndListaOrderByFechaConsultaDesc(1L, lista))
                .thenReturn(Optional.of(consulta));
    }

    private void sinConsultas() {
        when(repository.findFirstByProveedorIdProveedorAndListaOrderByFechaConsultaDesc(
                eq(1L), any())).thenReturn(Optional.empty());
    }

    private void todasObligatoriasLimpias() {
        sinConsultas();
        for (ListaRestrictiva lista : ConsultaSarlaftService.LISTAS_OBLIGATORIAS) {
            responder(lista, ResultadoSarlaft.SIN_COINCIDENCIA, 10);
        }
    }

    @Test
    void sinConsultasEstaSinConsultar() {
        sinConsultas();

        assertEquals(EstadoSarlaft.SIN_CONSULTAR, service.calcularEstado(1L, HOY));
    }

    @Test
    void todasLimpiasYVigentes() {
        todasObligatoriasLimpias();

        assertEquals(EstadoSarlaft.LIMPIO, service.calcularEstado(1L, HOY));
    }

    @Test
    void coincidenciaEnListaObligatoriaBloquea() {
        todasObligatoriasLimpias();
        responder(ListaRestrictiva.OFAC, ResultadoSarlaft.COINCIDENCIA, 1);

        assertEquals(EstadoSarlaft.BLOQUEADO, service.calcularEstado(1L, HOY));
    }

    @Test
    void pepNoBloqueaPeroExigeRevision() {
        todasObligatoriasLimpias();
        responder(ListaRestrictiva.PEP, ResultadoSarlaft.COINCIDENCIA, 1);

        assertEquals(EstadoSarlaft.EN_REVISION, service.calcularEstado(1L, HOY));
    }

    @Test
    void consultaAntiguaQuedaVencida() {
        todasObligatoriasLimpias();
        responder(ListaRestrictiva.POLICIA, ResultadoSarlaft.SIN_COINCIDENCIA,
                ConsultaSarlaftService.DIAS_VIGENCIA + 1);

        assertEquals(EstadoSarlaft.VENCIDO, service.calcularEstado(1L, HOY));
    }

    @Test
    void noPermiteModificarConsultas() {
        ConsultaSarlaft existente = ConsultaSarlaft.builder().idConsulta(5L).build();

        assertThrows(IllegalArgumentException.class, () -> service.registrarConsulta(existente));
        verify(repository, never()).save(any());
    }

    @Test
    void coincidenciaExigeObservacion() {
        ConsultaSarlaft consulta = ConsultaSarlaft.builder()
                .proveedor(Proveedor.builder().idProveedor(1L).build())
                .lista(ListaRestrictiva.ONU)
                .resultado(ResultadoSarlaft.POSIBLE_COINCIDENCIA)
                .documentoConsultado("900123456")
                .nombreConsultado("Proveedor Ejemplo S.A.S.")
                .usuarioResponsable("analista@epcore.com")
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.registrarConsulta(consulta));
    }
}
