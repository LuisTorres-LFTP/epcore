package com.epcore.service.impl;

import com.epcore.entity.InformacionFinanciera;
import com.epcore.repository.InformacionFinancieraRepository;
import com.epcore.service.InformacionFinancieraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class InformacionFinancieraServiceImpl implements InformacionFinancieraService {

    private static final Set<String> FUENTES_VALIDAS = Set.of(
            "ESTADOS_FINANCIEROS",
            "RUP",
            "DECLARACION_RENTA"
    );

    /** Diferencia máxima tolerada (en pesos) en la ecuación contable por redondeos. */
    private static final BigDecimal TOLERANCIA_ECUACION = BigDecimal.valueOf(1000);

    private final InformacionFinancieraRepository informacionFinancieraRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InformacionFinanciera> listarPorProveedor(Long idProveedor) {
        validarId(idProveedor, "proveedor");

        return informacionFinancieraRepository
                .findByProveedorIdProveedorOrderByAnioCorteDesc(idProveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InformacionFinanciera> buscarPorId(Long idInfoFinanciera) {
        validarId(idInfoFinanciera, "registro financiero");

        return informacionFinancieraRepository.findById(idInfoFinanciera);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InformacionFinanciera> obtenerMasReciente(Long idProveedor) {
        validarId(idProveedor, "proveedor");

        return informacionFinancieraRepository
                .findFirstByProveedorIdProveedorOrderByAnioCorteDesc(idProveedor);
    }

    @Override
    public InformacionFinanciera guardar(InformacionFinanciera informacion) {
        if (informacion == null) {
            throw new IllegalArgumentException("La información financiera no puede ser nula.");
        }

        if (informacion.getProveedor() == null
                || informacion.getProveedor().getIdProveedor() == null) {
            throw new IllegalArgumentException(
                    "La información financiera debe estar asociada a un proveedor."
            );
        }

        validarAnioYFecha(informacion);

        requerido(informacion.getActivoCorriente(), "activo corriente");
        requerido(informacion.getPasivoCorriente(), "pasivo corriente");
        requerido(informacion.getActivoTotal(), "activo total");
        requerido(informacion.getPasivoTotal(), "pasivo total");
        requerido(informacion.getPatrimonio(), "patrimonio");

        noNegativo(informacion.getActivoCorriente(), "activo corriente");
        noNegativo(informacion.getPasivoCorriente(), "pasivo corriente");
        noNegativo(informacion.getActivoTotal(), "activo total");
        noNegativo(informacion.getPasivoTotal(), "pasivo total");
        noNegativo(informacion.getGastosIntereses(), "gastos de intereses");
        noNegativo(informacion.getIngresosOperacionales(), "ingresos operacionales");

        if (informacion.getActivoCorriente().compareTo(informacion.getActivoTotal()) > 0) {
            throw new IllegalArgumentException(
                    "El activo corriente no puede ser mayor que el activo total."
            );
        }

        if (informacion.getPasivoCorriente().compareTo(informacion.getPasivoTotal()) > 0) {
            throw new IllegalArgumentException(
                    "El pasivo corriente no puede ser mayor que el pasivo total."
            );
        }

        // Ecuación contable: Activo = Pasivo + Patrimonio
        BigDecimal diferencia = informacion.getActivoTotal()
                .subtract(informacion.getPasivoTotal())
                .subtract(informacion.getPatrimonio())
                .abs();

        if (diferencia.compareTo(TOLERANCIA_ECUACION) > 0) {
            throw new IllegalArgumentException(
                    "Los valores no cumplen la ecuación contable "
                            + "(activo total = pasivo total + patrimonio)."
            );
        }

        normalizarFuente(informacion);
        validarAnioUnico(informacion);

        return informacionFinancieraRepository.save(informacion);
    }

    @Override
    public void eliminar(Long idInfoFinanciera) {
        validarId(idInfoFinanciera, "registro financiero");

        if (!informacionFinancieraRepository.existsById(idInfoFinanciera)) {
            throw new IllegalArgumentException(
                    "No existe información financiera con el identificador: "
                            + idInfoFinanciera
            );
        }

        informacionFinancieraRepository.deleteById(idInfoFinanciera);
    }

    @Override
    public List<String> verificarCapacidadFinanciera(
            InformacionFinanciera informacion,
            BigDecimal liquidezMinima,
            BigDecimal endeudamientoMaximo,
            BigDecimal coberturaInteresesMinima,
            BigDecimal patrimonioMinimo
    ) {
        List<String> incumplimientos = new ArrayList<>();

        if (informacion == null) {
            incumplimientos.add("El proveedor no tiene información financiera registrada.");
            return incumplimientos;
        }

        // Un indicador nulo es "indeterminado" (divisor en cero): por
        // ejemplo, sin pasivo corriente la liquidez es infinita y cumple.
        BigDecimal liquidez = informacion.getIndiceLiquidez();
        if (liquidezMinima != null && liquidez != null
                && liquidez.compareTo(liquidezMinima) < 0) {
            incumplimientos.add("Índice de liquidez " + liquidez
                    + " inferior al mínimo " + liquidezMinima + ".");
        }

        BigDecimal endeudamiento = informacion.getNivelEndeudamiento();
        if (endeudamientoMaximo != null) {
            if (endeudamiento == null) {
                incumplimientos.add(
                        "No se puede calcular el endeudamiento: el activo total es cero."
                );
            } else if (endeudamiento.compareTo(endeudamientoMaximo) > 0) {
                incumplimientos.add("Nivel de endeudamiento " + endeudamiento
                        + " superior al máximo " + endeudamientoMaximo + ".");
            }
        }

        BigDecimal cobertura = informacion.getRazonCoberturaIntereses();
        if (coberturaInteresesMinima != null && cobertura != null
                && cobertura.compareTo(coberturaInteresesMinima) < 0) {
            incumplimientos.add("Razón de cobertura de intereses " + cobertura
                    + " inferior al mínimo " + coberturaInteresesMinima + ".");
        }

        if (patrimonioMinimo != null
                && informacion.getPatrimonio().compareTo(patrimonioMinimo) < 0) {
            incumplimientos.add("Patrimonio inferior al mínimo exigido ("
                    + patrimonioMinimo.toPlainString() + ").");
        }

        return incumplimientos;
    }

    private void validarAnioYFecha(InformacionFinanciera informacion) {
        if (informacion.getFechaCorte() == null) {
            throw new IllegalArgumentException("La fecha de corte es obligatoria.");
        }

        if (informacion.getFechaCorte().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de corte no puede ser futura.");
        }

        if (informacion.getAnioCorte() == null) {
            informacion.setAnioCorte(informacion.getFechaCorte().getYear());
        } else if (!informacion.getAnioCorte().equals(informacion.getFechaCorte().getYear())) {
            throw new IllegalArgumentException(
                    "El año de corte no coincide con la fecha de corte."
            );
        }
    }

    private void normalizarFuente(InformacionFinanciera informacion) {
        if (informacion.getFuente() == null || informacion.getFuente().isBlank()) {
            informacion.setFuente("ESTADOS_FINANCIEROS");
            return;
        }

        String fuente = informacion.getFuente().trim().toUpperCase();

        if (!FUENTES_VALIDAS.contains(fuente)) {
            throw new IllegalArgumentException(
                    "Fuente no válida. Use: " + String.join(", ", FUENTES_VALIDAS)
            );
        }

        informacion.setFuente(fuente);
    }

    private void validarAnioUnico(InformacionFinanciera informacion) {
        informacionFinancieraRepository
                .findByProveedorIdProveedorAndAnioCorte(
                        informacion.getProveedor().getIdProveedor(),
                        informacion.getAnioCorte()
                )
                .filter(existente -> !existente.getIdInfoFinanciera()
                        .equals(informacion.getIdInfoFinanciera()))
                .ifPresent(existente -> {
                    throw new IllegalArgumentException(
                            "El proveedor ya tiene información financiera con corte "
                                    + informacion.getAnioCorte() + "."
                    );
                });
    }

    private static void requerido(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
    }

    private static void noNegativo(BigDecimal valor, String campo) {
        if (valor != null && valor.signum() < 0) {
            throw new IllegalArgumentException("El campo " + campo + " no puede ser negativo.");
        }
    }

    private static void validarId(Long id, String nombre) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del " + nombre + " no es válido."
            );
        }
    }
}
