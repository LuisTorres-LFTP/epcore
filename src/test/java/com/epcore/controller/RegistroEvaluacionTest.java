package com.epcore.controller;

import com.epcore.entity.Criterio;
import com.epcore.entity.Evaluacion;
import com.epcore.entity.EvaluacionDetalle;
import com.epcore.entity.Proveedor;
import com.epcore.service.CriterioService;
import com.epcore.service.EvaluacionDetalleService;
import com.epcore.service.EvaluacionService;
import com.epcore.service.ProveedorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Registro de evaluaciones: rango de calificaciones y suma de pesos (defectos BUG-004 y BUG-005). */
class RegistroEvaluacionTest {

    private final EvaluacionService evaluaciones = mock(EvaluacionService.class);
    private final CriterioService criterios = mock(CriterioService.class);
    private final EvaluacionDetalleService detalles = mock(EvaluacionDetalleService.class);
    private final EvaluacionWebController controller = new EvaluacionWebController(
            evaluaciones, mock(ProveedorService.class), criterios, detalles);

    private final Criterio calidad = Criterio.builder().idCriterio(1L).nombreCriterio("Calidad").peso(60).activo(true).build();
    private final Criterio entrega = Criterio.builder().idCriterio(2L).nombreCriterio("Entrega").peso(40).activo(true).build();
    private final Criterio inactivo = Criterio.builder().idCriterio(3L).nombreCriterio("Antiguo").peso(5).activo(false).build();

    @BeforeEach
    void configurar() {
        when(criterios.listarActivos()).thenReturn(List.of(calidad, entrega));
        when(criterios.sumaPesosActivos()).thenReturn(100);
        when(criterios.buscarPorId(1L)).thenReturn(Optional.of(calidad));
        when(criterios.buscarPorId(2L)).thenReturn(Optional.of(entrega));
        when(criterios.buscarPorId(3L)).thenReturn(Optional.of(inactivo));
        when(evaluaciones.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static Evaluacion evaluacion() {
        Evaluacion e = new Evaluacion();
        e.setProveedor(Proveedor.builder().idProveedor(13L).build());
        e.setFechaEvaluacion(LocalDate.of(2026, 11, 17));
        return e;
    }

    private String registrar(Map<String, String> calificaciones, RedirectAttributesModelMap flash) {
        return controller.guardarEvaluacion(evaluacion(), new HashMap<>(calificaciones), flash);
    }

    @Test
    void guardaEvaluacionValidaConResultadoPonderado() {
        Evaluacion guardada = evaluacion();
        when(evaluaciones.guardar(any())).thenAnswer(i -> i.getArgument(0));

        String vista = controller.guardarEvaluacion(guardada,
                new HashMap<>(Map.of("calificacion_1", "90", "calificacion_2", "70")), new RedirectAttributesModelMap());

        assertEquals("redirect:/evaluaciones", vista);
        assertEquals(82.0, guardada.getResultadoFinal(), 0.001); // 90 x 60 % + 70 x 40 %
        verify(detalles, times(2)).guardar(any(EvaluacionDetalle.class));
    }

    @Test
    void rechazaCalificacionFueraDeRangoSinGuardarNada() {
        for (String valor : List.of("150", "-20")) {
            RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

            String vista = registrar(Map.of("calificacion_1", valor, "calificacion_2", "80"), flash);

            assertEquals("redirect:/evaluaciones/nueva", vista);
            assertTrue(flash.getFlashAttributes().get("mensajeError").toString().contains("entre 0 y 100"));
        }
        verify(evaluaciones, never()).guardar(any());
        verify(detalles, never()).guardar(any());
    }

    @Test
    void bloqueaEvaluarSiLosPesosActivosNoSuman100() {
        when(criterios.sumaPesosActivos()).thenReturn(107);
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        String vista = registrar(Map.of("calificacion_1", "90", "calificacion_2", "70"), flash);

        assertEquals("redirect:/evaluaciones/nueva", vista);
        assertTrue(flash.getFlashAttributes().get("mensajeError").toString().contains("107%"));
        verify(evaluaciones, never()).guardar(any());
    }

    @Test
    void rechazaCriterioInactivoYCalificacionFaltante() {
        RedirectAttributesModelMap conInactivo = new RedirectAttributesModelMap();
        registrar(Map.of("calificacion_1", "90", "calificacion_2", "70", "calificacion_3", "50"), conInactivo);
        assertTrue(conInactivo.getFlashAttributes().get("mensajeError").toString().contains("inactivo"));

        RedirectAttributesModelMap faltante = new RedirectAttributesModelMap();
        registrar(Map.of("calificacion_1", "90"), faltante);
        assertTrue(faltante.getFlashAttributes().get("mensajeError").toString().contains("todos los criterios"));

        verify(evaluaciones, never()).guardar(any());
    }
}
