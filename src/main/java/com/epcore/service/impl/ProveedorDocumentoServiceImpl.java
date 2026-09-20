package com.epcore.service.impl;

import com.epcore.entity.ProveedorDocumento;
import com.epcore.repository.ProveedorDocumentoRepository;
import com.epcore.service.ProveedorDocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProveedorDocumentoServiceImpl implements ProveedorDocumentoService {

    private final ProveedorDocumentoRepository proveedorDocumentoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDocumento> listarTodos() {
        return proveedorDocumentoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDocumento> listarPorProveedor(Long idProveedor) {
        validarIdProveedor(idProveedor);

        return proveedorDocumentoRepository
                .findByProveedorIdProveedorOrderByFechaCargaDesc(idProveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDocumento> listarActivosPorProveedor(Long idProveedor) {
        validarIdProveedor(idProveedor);

        return proveedorDocumentoRepository
                .findByProveedorIdProveedorAndActivoTrueOrderByFechaCargaDesc(idProveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProveedorDocumento> buscarPorId(Long idDocumento) {
        validarIdDocumento(idDocumento);

        return proveedorDocumentoRepository.findById(idDocumento);
    }

    @Override
    public ProveedorDocumento guardar(ProveedorDocumento documento) {
        if (documento == null) {
            throw new IllegalArgumentException("El documento no puede ser nulo.");
        }

        if (documento.getProveedor() == null
                || documento.getProveedor().getIdProveedor() == null) {
            throw new IllegalArgumentException(
                    "El documento debe estar asociado a un proveedor."
            );
        }

        if (documento.getTipoDocumento() == null
                || documento.getTipoDocumento().isBlank()) {
            throw new IllegalArgumentException(
                    "El tipo de documento es obligatorio."
            );
        }

        if (documento.getNombreArchivo() == null
                || documento.getNombreArchivo().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del archivo es obligatorio."
            );
        }

        if (documento.getRutaArchivo() == null
                || documento.getRutaArchivo().isBlank()) {
            throw new IllegalArgumentException(
                    "La ruta del archivo es obligatoria."
            );
        }

        validarFechas(documento);

        documento.setTipoDocumento(documento.getTipoDocumento().trim());
        documento.setNombreArchivo(documento.getNombreArchivo().trim());
        documento.setRutaArchivo(documento.getRutaArchivo().trim());

        if (documento.getEstadoValidacion() == null
                || documento.getEstadoValidacion().isBlank()) {
            documento.setEstadoValidacion("PENDIENTE");
        } else {
            documento.setEstadoValidacion(
                    documento.getEstadoValidacion().trim().toUpperCase()
            );
        }

        if (documento.getTieneVencimiento() == null) {
            documento.setTieneVencimiento(false);
        }

        if (documento.getObligatorio() == null) {
            documento.setObligatorio(true);
        }

        if (documento.getActivo() == null) {
            documento.setActivo(true);
        }

        actualizarEstadoDocumento(documento);

        return proveedorDocumentoRepository.save(documento);
    }

    @Override
    public void eliminar(Long idDocumento) {
        validarIdDocumento(idDocumento);

        if (!proveedorDocumentoRepository.existsById(idDocumento)) {
            throw new IllegalArgumentException(
                    "No existe un documento con el identificador: " + idDocumento
            );
        }

        proveedorDocumentoRepository.deleteById(idDocumento);
    }

    @Override
    public void inactivar(Long idDocumento) {
        ProveedorDocumento documento = obtenerDocumentoObligatorio(idDocumento);

        documento.setActivo(false);

        proveedorDocumentoRepository.save(documento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDocumento> listarDocumentosVencidos() {
        return proveedorDocumentoRepository
                .listarDocumentosVencidos(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDocumento> listarDocumentosProximosAVencer(int dias) {
        if (dias < 0) {
            throw new IllegalArgumentException(
                    "La cantidad de días no puede ser negativa."
            );
        }

        LocalDate fechaActual = LocalDate.now();
        LocalDate fechaLimite = fechaActual.plusDays(dias);

        return proveedorDocumentoRepository
                .listarDocumentosProximosAVencer(
                        fechaActual,
                        fechaLimite
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Long contarDocumentosObligatorios(Long idProveedor) {
        validarIdProveedor(idProveedor);

        Long total = proveedorDocumentoRepository
                .contarDocumentosObligatorios(idProveedor);

        return total == null ? 0L : total;
    }

    @Override
    @Transactional(readOnly = true)
    public Long contarDocumentosObligatoriosAprobados(Long idProveedor) {
        validarIdProveedor(idProveedor);

        Long total = proveedorDocumentoRepository
                .contarDocumentosObligatoriosAprobados(idProveedor);

        return total == null ? 0L : total;
    }

    @Override
    @Transactional(readOnly = true)
    public Double calcularPorcentajeCumplimiento(Long idProveedor) {
        Long totalObligatorios = contarDocumentosObligatorios(idProveedor);

        if (totalObligatorios == 0) {
            return 0.0;
        }

        Long totalAprobados = contarDocumentosObligatoriosAprobados(idProveedor);

        double porcentaje = (
                totalAprobados.doubleValue()
                        / totalObligatorios.doubleValue()
        ) * 100;

        return Math.round(porcentaje * 100.0) / 100.0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDocumento> listarPorProveedorYEstado(
            Long idProveedor,
            String estado
    ) {
        validarIdProveedor(idProveedor);

        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException(
                    "El estado documental es obligatorio."
            );
        }

        return proveedorDocumentoRepository.listarPorProveedorYEstado(
                idProveedor,
                estado.trim().toUpperCase()
        );
    }

    @Override
    public boolean estaVencido(ProveedorDocumento documento) {
        if (documento == null) {
            return false;
        }

        if (!Boolean.TRUE.equals(documento.getTieneVencimiento())) {
            return false;
        }

        if (documento.getFechaVencimiento() == null) {
            return false;
        }

        return documento.getFechaVencimiento().isBefore(LocalDate.now());
    }

    @Override
    public boolean estaProximoAVencer(
            ProveedorDocumento documento,
            int dias
    ) {
        if (documento == null || dias < 0) {
            return false;
        }

        if (!Boolean.TRUE.equals(documento.getTieneVencimiento())) {
            return false;
        }

        if (documento.getFechaVencimiento() == null) {
            return false;
        }

        LocalDate fechaActual = LocalDate.now();
        LocalDate fechaLimite = fechaActual.plusDays(dias);

        return !documento.getFechaVencimiento().isBefore(fechaActual)
                && !documento.getFechaVencimiento().isAfter(fechaLimite);
    }

    @Override
    public void actualizarEstadosPorVencimiento() {
        List<ProveedorDocumento> documentosVencidos =
                proveedorDocumentoRepository.listarDocumentosVencidos(
                        LocalDate.now()
                );

        for (ProveedorDocumento documento : documentosVencidos) {
            if (!"VENCIDO".equalsIgnoreCase(
                    documento.getEstadoValidacion()
            )) {
                documento.setEstadoValidacion("VENCIDO");
            }
        }

        proveedorDocumentoRepository.saveAll(documentosVencidos);
    }

    private void actualizarEstadoDocumento(
            ProveedorDocumento documento
    ) {
        if (estaVencido(documento)) {
            documento.setEstadoValidacion("VENCIDO");
        }
    }

    private void validarFechas(ProveedorDocumento documento) {
        if (Boolean.TRUE.equals(documento.getTieneVencimiento())
                && documento.getFechaVencimiento() == null) {
            throw new IllegalArgumentException(
                    "Debe ingresar la fecha de vencimiento."
            );
        }

        if (documento.getFechaExpedicion() != null
                && documento.getFechaVencimiento() != null
                && documento.getFechaVencimiento()
                .isBefore(documento.getFechaExpedicion())) {
            throw new IllegalArgumentException(
                    "La fecha de vencimiento no puede ser anterior "
                            + "a la fecha de expedición."
            );
        }
    }

    private ProveedorDocumento obtenerDocumentoObligatorio(
            Long idDocumento
    ) {
        validarIdDocumento(idDocumento);

        return proveedorDocumentoRepository.findById(idDocumento)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un documento con el identificador: "
                                + idDocumento
                ));
    }

    private void validarIdDocumento(Long idDocumento) {
        if (idDocumento == null || idDocumento <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del documento no es válido."
            );
        }
    }

    private void validarIdProveedor(Long idProveedor) {
        if (idProveedor == null || idProveedor <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del proveedor no es válido."
            );
        }
    }
}