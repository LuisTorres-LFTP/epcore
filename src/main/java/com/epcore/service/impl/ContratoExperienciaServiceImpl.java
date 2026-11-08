package com.epcore.service.impl;

import com.epcore.entity.ContratoExperiencia;
import com.epcore.repository.ContratoExperienciaRepository;
import com.epcore.service.ContratoExperienciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class ContratoExperienciaServiceImpl implements ContratoExperienciaService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "EN_EJECUCION",
            "TERMINADO",
            "LIQUIDADO"
    );

    /** Un código UNSPSC tiene 8 dígitos (segmento, familia, clase, producto). */
    private static final Pattern CODIGO_UNSPSC = Pattern.compile("\\d{8}");

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ContratoExperienciaRepository contratoExperienciaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ContratoExperiencia> listarPorProveedor(Long idProveedor) {
        validarId(idProveedor, "proveedor");

        return contratoExperienciaRepository
                .findByProveedorIdProveedorOrderByFechaInicioDesc(idProveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContratoExperiencia> buscarPorId(Long idContrato) {
        validarId(idContrato, "contrato");

        return contratoExperienciaRepository.findById(idContrato);
    }

    @Override
    public ContratoExperiencia guardar(ContratoExperiencia contrato) {
        if (contrato == null) {
            throw new IllegalArgumentException("El contrato no puede ser nulo.");
        }

        if (contrato.getProveedor() == null
                || contrato.getProveedor().getIdProveedor() == null) {
            throw new IllegalArgumentException(
                    "El contrato debe estar asociado a un proveedor."
            );
        }

        if (contrato.getContratante() == null || contrato.getContratante().isBlank()) {
            throw new IllegalArgumentException("El contratante es obligatorio.");
        }

        if (contrato.getObjeto() == null || contrato.getObjeto().isBlank()) {
            throw new IllegalArgumentException("El objeto del contrato es obligatorio.");
        }

        if (contrato.getValor() == null || contrato.getValor().signum() <= 0) {
            throw new IllegalArgumentException("El valor del contrato debe ser mayor que cero.");
        }

        if (contrato.getPorcentajeParticipacion() == null) {
            contrato.setPorcentajeParticipacion(CIEN);
        }

        if (contrato.getPorcentajeParticipacion().signum() <= 0
                || contrato.getPorcentajeParticipacion().compareTo(CIEN) > 0) {
            throw new IllegalArgumentException(
                    "El porcentaje de participación debe estar entre 0 y 100."
            );
        }

        validarFechasYEstado(contrato);
        normalizarCodigosUnspsc(contrato);

        contrato.setContratante(contrato.getContratante().trim());
        contrato.setObjeto(contrato.getObjeto().trim());

        if (contrato.getContratantePublico() == null) {
            contrato.setContratantePublico(false);
        }

        if (contrato.getTieneCertificacion() == null) {
            contrato.setTieneCertificacion(false);
        }

        return contratoExperienciaRepository.save(contrato);
    }

    @Override
    public void eliminar(Long idContrato) {
        validarId(idContrato, "contrato");

        if (!contratoExperienciaRepository.existsById(idContrato)) {
            throw new IllegalArgumentException(
                    "No existe un contrato con el identificador: " + idContrato
            );
        }

        contratoExperienciaRepository.deleteById(idContrato);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoExperiencia> listarAcreditables(
            Long idProveedor,
            String codigoUnspsc,
            boolean soloCertificados
    ) {
        return listarPorProveedor(idProveedor).stream()
                // Solo acredita experiencia lo ya ejecutado.
                .filter(c -> !"EN_EJECUCION".equals(c.getEstado()))
                .filter(c -> !soloCertificados || Boolean.TRUE.equals(c.getTieneCertificacion()))
                .filter(c -> codigoUnspsc == null || codigoUnspsc.isBlank()
                        || c.tieneCodigoUnspsc(codigoUnspsc))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumarExperienciaAcreditable(
            Long idProveedor,
            String codigoUnspsc,
            boolean soloCertificados
    ) {
        return listarAcreditables(idProveedor, codigoUnspsc, soloCertificados).stream()
                .map(ContratoExperiencia::getValorAcreditable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validarFechasYEstado(ContratoExperiencia contrato) {
        if (contrato.getFechaInicio() == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria.");
        }

        if (contrato.getFechaInicio().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser futura.");
        }

        if (contrato.getFechaTerminacion() != null
                && contrato.getFechaTerminacion().isBefore(contrato.getFechaInicio())) {
            throw new IllegalArgumentException(
                    "La fecha de terminación no puede ser anterior a la de inicio."
            );
        }

        String estado = contrato.getEstado() == null || contrato.getEstado().isBlank()
                ? (contrato.getFechaTerminacion() == null ? "EN_EJECUCION" : "TERMINADO")
                : contrato.getEstado().trim().toUpperCase();

        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new IllegalArgumentException(
                    "Estado no válido. Use: " + String.join(", ", ESTADOS_VALIDOS)
            );
        }

        if (!"EN_EJECUCION".equals(estado) && contrato.getFechaTerminacion() == null) {
            throw new IllegalArgumentException(
                    "Un contrato terminado o liquidado debe tener fecha de terminación."
            );
        }

        contrato.setEstado(estado);
    }

    private void normalizarCodigosUnspsc(ContratoExperiencia contrato) {
        List<String> codigos = contrato.getListaCodigosUnspsc();

        for (String codigo : codigos) {
            if (!CODIGO_UNSPSC.matcher(codigo).matches()) {
                throw new IllegalArgumentException(
                        "Código UNSPSC no válido: " + codigo + " (deben ser 8 dígitos)."
                );
            }
        }

        contrato.setCodigosUnspsc(codigos.isEmpty() ? null : String.join(",", codigos));
    }

    private static void validarId(Long id, String nombre) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del " + nombre + " no es válido."
            );
        }
    }
}
