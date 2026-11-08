package com.epcore.service.impl;

import com.epcore.entity.ConsultaSarlaft;
import com.epcore.entity.EstadoSarlaft;
import com.epcore.entity.ListaRestrictiva;
import com.epcore.entity.ResultadoSarlaft;
import com.epcore.repository.ConsultaSarlaftRepository;
import com.epcore.service.ConsultaSarlaftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultaSarlaftServiceImpl implements ConsultaSarlaftService {

    private final ConsultaSarlaftRepository consultaSarlaftRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ConsultaSarlaft> listarPorProveedor(Long idProveedor) {
        validarIdProveedor(idProveedor);

        return consultaSarlaftRepository
                .findByProveedorIdProveedorOrderByFechaConsultaDesc(idProveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConsultaSarlaft> obtenerUltimaConsulta(
            Long idProveedor,
            ListaRestrictiva lista
    ) {
        validarIdProveedor(idProveedor);

        if (lista == null) {
            throw new IllegalArgumentException("La lista restrictiva es obligatoria.");
        }

        return consultaSarlaftRepository
                .findFirstByProveedorIdProveedorAndListaOrderByFechaConsultaDesc(
                        idProveedor,
                        lista
                );
    }

    @Override
    public ConsultaSarlaft registrarConsulta(ConsultaSarlaft consulta) {
        if (consulta == null) {
            throw new IllegalArgumentException("La consulta no puede ser nula.");
        }

        // Las consultas son evidencia: solo se crean, nunca se modifican.
        if (consulta.getIdConsulta() != null) {
            throw new IllegalArgumentException(
                    "Las consultas SARLAFT no se pueden modificar; registre una nueva."
            );
        }

        if (consulta.getProveedor() == null
                || consulta.getProveedor().getIdProveedor() == null) {
            throw new IllegalArgumentException(
                    "La consulta debe estar asociada a un proveedor."
            );
        }

        if (consulta.getLista() == null) {
            throw new IllegalArgumentException("La lista consultada es obligatoria.");
        }

        if (consulta.getResultado() == null) {
            throw new IllegalArgumentException("El resultado de la consulta es obligatorio.");
        }

        if (consulta.getDocumentoConsultado() == null
                || consulta.getDocumentoConsultado().isBlank()) {
            throw new IllegalArgumentException("El documento consultado es obligatorio.");
        }

        if (consulta.getNombreConsultado() == null
                || consulta.getNombreConsultado().isBlank()) {
            throw new IllegalArgumentException("El nombre consultado es obligatorio.");
        }

        if (consulta.getUsuarioResponsable() == null
                || consulta.getUsuarioResponsable().isBlank()) {
            throw new IllegalArgumentException("El usuario responsable es obligatorio.");
        }

        if (consulta.getResultado() != ResultadoSarlaft.SIN_COINCIDENCIA
                && (consulta.getObservacion() == null || consulta.getObservacion().isBlank())) {
            throw new IllegalArgumentException(
                    "Debe registrar una observación cuando hay coincidencia o posible coincidencia."
            );
        }

        if (consulta.getFechaConsulta() != null
                && consulta.getFechaConsulta().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de consulta no puede ser futura.");
        }

        consulta.setDocumentoConsultado(consulta.getDocumentoConsultado().trim());
        consulta.setNombreConsultado(consulta.getNombreConsultado().trim());
        consulta.setUsuarioResponsable(consulta.getUsuarioResponsable().trim());

        return consultaSarlaftRepository.save(consulta);
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoSarlaft calcularEstado(Long idProveedor) {
        return calcularEstado(idProveedor, LocalDateTime.now());
    }

    /**
     * Prioridad: BLOQUEADO > EN_REVISION > SIN_CONSULTAR > VENCIDO > LIMPIO.
     * Se toma la consulta más reciente de cada lista.
     */
    @Override
    @Transactional(readOnly = true)
    public EstadoSarlaft calcularEstado(Long idProveedor, LocalDateTime fechaReferencia) {
        validarIdProveedor(idProveedor);

        LocalDateTime limiteVigencia = fechaReferencia.minusDays(DIAS_VIGENCIA);

        boolean bloqueado = false;
        boolean enRevision = false;
        boolean faltaConsulta = false;
        boolean vencido = false;

        for (ListaRestrictiva lista : ListaRestrictiva.values()) {
            Optional<ConsultaSarlaft> ultima = obtenerUltimaConsulta(idProveedor, lista);
            boolean obligatoria = LISTAS_OBLIGATORIAS.contains(lista);

            if (ultima.isEmpty()) {
                faltaConsulta |= obligatoria;
                continue;
            }

            ConsultaSarlaft consulta = ultima.get();

            switch (consulta.getResultado()) {
                // Ser PEP no impide contratar, pero exige debida diligencia ampliada.
                case COINCIDENCIA -> {
                    if (obligatoria) {
                        bloqueado = true;
                    } else {
                        enRevision = true;
                    }
                }
                case POSIBLE_COINCIDENCIA -> enRevision = true;
                case SIN_COINCIDENCIA -> { }
            }

            if (obligatoria && consulta.getFechaConsulta().isBefore(limiteVigencia)) {
                vencido = true;
            }
        }

        if (bloqueado) {
            return EstadoSarlaft.BLOQUEADO;
        }
        if (enRevision) {
            return EstadoSarlaft.EN_REVISION;
        }
        if (faltaConsulta) {
            return EstadoSarlaft.SIN_CONSULTAR;
        }
        if (vencido) {
            return EstadoSarlaft.VENCIDO;
        }
        return EstadoSarlaft.LIMPIO;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaHabilitado(Long idProveedor) {
        return calcularEstado(idProveedor) == EstadoSarlaft.LIMPIO;
    }

    private void validarIdProveedor(Long idProveedor) {
        if (idProveedor == null || idProveedor <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del proveedor no es válido."
            );
        }
    }
}
