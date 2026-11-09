package com.epcore.service.proceso;

import com.epcore.entity.proceso.FactoresPuntaje;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Calcula el puntaje de las ofertas habilitadas. Fórmulas:
 * precio = menor oferta / oferta x puntos; plazo = menor plazo / plazo x puntos;
 * técnica = calificación del comité (0-100) x puntos / 100;
 * experiencia = experiencia del proveedor / mayor experiencia x puntos;
 * desempeño = evaluación histórica (0-100) x puntos / 100;
 * industria nacional = puntos completos si acredita.
 */
@Component
public class EvaluadorOfertas {

    public record Entrada(Long idOferta, Long idProveedor, BigDecimal valor, int plazoDias,
                          Integer calificacionTecnica, boolean industriaNacional,
                          BigDecimal experienciaAcreditable, Double desempeno,
                          List<String> incumplimientos) {
        public boolean habilitada() {
            return incumplimientos == null || incumplimientos.isEmpty();
        }
    }

    public record Resultado(Long idOferta, Long idProveedor, boolean habilitada, List<String> incumplimientos,
                            Map<String, Double> detalle, Double total, boolean pendienteCalificacion) { }

    public List<Resultado> evaluar(List<Entrada> entradas, FactoresPuntaje f) {
        List<Entrada> hab = entradas.stream().filter(Entrada::habilitada).toList();

        BigDecimal minValor = hab.stream().map(Entrada::valor).min(Comparator.naturalOrder()).orElse(null);
        int minPlazo = hab.stream().mapToInt(Entrada::plazoDias).min().orElse(0);
        BigDecimal maxExp = hab.stream().map(e -> nz(e.experienciaAcreditable()))
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        List<Resultado> resultados = new ArrayList<>();
        for (Entrada e : entradas) {
            if (!e.habilitada()) {
                resultados.add(new Resultado(e.idOferta(), e.idProveedor(), false, e.incumplimientos(), Map.of(), null, false));
                continue;
            }
            Map<String, Double> d = new LinkedHashMap<>();
            boolean pendiente = false;
            if (f.getPrecio() > 0) d.put("precio", razon(minValor, e.valor()) * f.getPrecio());
            if (f.getPlazo() > 0) d.put("plazo", (double) minPlazo / Math.max(1, e.plazoDias()) * f.getPlazo());
            if (f.getTecnica() > 0) {
                if (e.calificacionTecnica() == null) pendiente = true;
                else d.put("tecnica", e.calificacionTecnica() / 100.0 * f.getTecnica());
            }
            if (f.getExperiencia() > 0) d.put("experiencia", maxExp.signum() == 0 ? 0.0
                    : nz(e.experienciaAcreditable()).divide(maxExp, 6, RoundingMode.HALF_UP).doubleValue() * f.getExperiencia());
            if (f.getDesempeno() > 0) d.put("desempeno", e.desempeno() == null ? 0.0 : e.desempeno() / 100.0 * f.getDesempeno());
            if (f.getIndustriaNacional() > 0) d.put("industriaNacional", e.industriaNacional() ? f.getIndustriaNacional() : 0.0);

            Double total = pendiente ? null : redondear(d.values().stream().mapToDouble(Double::doubleValue).sum());
            d.replaceAll((k, v) -> redondear(v));
            resultados.add(new Resultado(e.idOferta(), e.idProveedor(), true, List.of(), d, total, pendiente));
        }

        resultados.sort(Comparator.comparing(Resultado::habilitada).reversed()
                .thenComparing(r -> r.total() == null ? -1 : r.total(), Comparator.reverseOrder()));
        return resultados;
    }

    private static double razon(BigDecimal menor, BigDecimal valor) {
        if (menor == null || valor == null || valor.signum() == 0) return 0;
        return menor.divide(valor, 6, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
