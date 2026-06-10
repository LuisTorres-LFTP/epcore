package com.epcore.controller;

import com.epcore.entity.Criterio;
import com.epcore.entity.Evaluacion;
import com.epcore.entity.EvaluacionDetalle;
import com.epcore.service.CriterioService;
import com.epcore.service.EvaluacionDetalleService;
import com.epcore.service.EvaluacionService;
import com.epcore.service.ProveedorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class EvaluacionWebController {

    private final EvaluacionService evaluacionService;
    private final ProveedorService proveedorService;
    private final CriterioService criterioService;
    private final EvaluacionDetalleService evaluacionDetalleService;

    public EvaluacionWebController(
            EvaluacionService evaluacionService,
            ProveedorService proveedorService,
            CriterioService criterioService,
            EvaluacionDetalleService evaluacionDetalleService) {
        this.evaluacionService = evaluacionService;
        this.proveedorService = proveedorService;
        this.criterioService = criterioService;
        this.evaluacionDetalleService = evaluacionDetalleService;
    }

    @GetMapping("/evaluaciones")
    public String listarEvaluaciones(
            @RequestParam(required = false, defaultValue = "todos") String filtro,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false, defaultValue = "todos") String sector,
            Model model) {

        if (proveedorId != null) {
            model.addAttribute("evaluaciones", evaluacionService.listarPorProveedor(proveedorId));
        } else {
            model.addAttribute("evaluaciones", evaluacionService.listarPorFiltro(filtro));
        }

        model.addAttribute("filtroSeleccionado", filtro);
        model.addAttribute("proveedorSeleccionado", proveedorId);
        model.addAttribute("sectorSeleccionado", sector);

        model.addAttribute("proveedores", proveedorService.listarProveedores());
        model.addAttribute("sectoresEconomicos", proveedorService.listarSectoresEconomicos());

        model.addAttribute("totalEvaluaciones", evaluacionService.totalEvaluaciones());
        model.addAttribute("promedioGeneral", evaluacionService.promedioGeneral());
        model.addAttribute("proveedoresSobresalientes", evaluacionService.proveedoresSobresalientes());
        model.addAttribute("proveedoresEnRiesgo", evaluacionService.proveedoresEnRiesgo());

        model.addAttribute("rankingProveedores", evaluacionService.obtenerRankingProveedores());
        model.addAttribute("rankingPorSector", evaluacionService.obtenerRankingPorSectorFiltrado(sector));
        model.addAttribute("indicadoresPorSector", evaluacionService.obtenerIndicadoresPorSector(sector));

        return "evaluaciones/lista";
    }

    @GetMapping("/evaluaciones/nueva")
    public String mostrarFormularioNueva(Model model) {
        model.addAttribute("evaluacion", new Evaluacion());
        model.addAttribute("proveedores", proveedorService.listarProveedores());
        model.addAttribute("criterios", criterioService.listarTodos());

        return "evaluaciones/formulario";
    }

    @PostMapping("/evaluaciones/guardar")
    public String guardarEvaluacion(
            @ModelAttribute Evaluacion evaluacion,
            @RequestParam Map<String, String> parametros) {

        evaluacion.setResultadoFinal(0.0);
        Evaluacion evaluacionGuardada = evaluacionService.guardar(evaluacion);

        double resultadoFinal = 0.0;

        for (Map.Entry<String, String> entry : parametros.entrySet()) {
            if (entry.getKey().startsWith("calificacion_")) {
                String valorCalificacion = entry.getValue();

                if (valorCalificacion != null && !valorCalificacion.isBlank()) {
                    Long idCriterio = Long.parseLong(entry.getKey().replace("calificacion_", ""));
                    Double calificacion = Double.parseDouble(valorCalificacion);

                    Criterio criterio = criterioService.buscarPorId(idCriterio)
                            .orElseThrow(() -> new IllegalArgumentException("Criterio no encontrado"));

                    Double resultadoPonderado = (calificacion * criterio.getPeso()) / 100;

                    EvaluacionDetalle detalle = EvaluacionDetalle.builder()
                            .evaluacion(evaluacionGuardada)
                            .criterio(criterio)
                            .calificacion(calificacion)
                            .peso(criterio.getPeso())
                            .resultadoPonderado(resultadoPonderado)
                            .build();

                    evaluacionDetalleService.guardar(detalle);
                    resultadoFinal += resultadoPonderado;
                }
            }
        }

        evaluacionGuardada.setResultadoFinal(resultadoFinal);
        evaluacionService.guardar(evaluacionGuardada);

        return "redirect:/evaluaciones";
    }

    @GetMapping("/evaluaciones/eliminar/{id}")
    public String eliminarEvaluacion(@PathVariable Long id) {
        evaluacionDetalleService.eliminarPorEvaluacion(id);
        evaluacionService.eliminar(id);
        return "redirect:/evaluaciones";
    }

    @GetMapping("/evaluaciones/detalle/{id}")
    public String verDetalleEvaluacion(@PathVariable Long id, Model model) {
        Evaluacion evaluacion = evaluacionService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Evaluación no encontrada"));

        model.addAttribute("evaluacion", evaluacion);
        model.addAttribute("detalles", evaluacionDetalleService.listarPorEvaluacion(id));

        return "evaluaciones/detalle";
    }
}