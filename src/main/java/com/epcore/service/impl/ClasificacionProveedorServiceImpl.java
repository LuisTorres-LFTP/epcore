package com.epcore.service.impl;

import com.epcore.entity.*;
import com.epcore.repository.*;
import com.epcore.service.ClasificacionProveedorService;
import com.epcore.service.ClasificadorProveedor;
import com.epcore.service.ClasificadorProveedor.ActividadDeclarada;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClasificacionProveedorServiceImpl implements ClasificacionProveedorService {

    private static final int MIN_JUSTIFICACION = 20;
    private static final String SEPARADOR = " | ";

    private final ProveedorRepository proveedorRepository;
    private final ActividadCiiuRepository actividadCiiuRepository;
    private final NichoRepository nichoRepository;
    private final ProveedorActividadRepository proveedorActividadRepository;
    private final ProveedorClasificacionRepository clasificacionRepository;
    private final IntegrantePluralidadRepository integranteRepository;
    private final ClasificadorProveedor clasificador;

    @Override
    public ProveedorClasificacion clasificar(
            Long idProveedor,
            TipoProveedor tipo,
            List<ActividadDeclarada> actividades,
            Set<String> codigosOficiales
    ) {
        Proveedor proveedor = proveedorRepository.findById(idProveedor)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un proveedor con el identificador: " + idProveedor));

        List<ActividadDeclarada> lista = actividades == null ? List.of() : actividades;

        Map<String, ActividadCiiu> catalogo = actividadCiiuRepository
                .findAllById(lista.stream().map(ActividadDeclarada::codigo)
                        .filter(Objects::nonNull).map(String::trim).toList())
                .stream()
                .collect(Collectors.toMap(ActividadCiiu::getCodigo, Function.identity()));

        List<ClasificadorProveedor.Integrante> integrantes = tipo != null && tipo.isEsPluralidad()
                ? cargarIntegrantes(idProveedor)
                : List.of();

        ClasificadorProveedor.Resultado resultado = clasificador.clasificar(
                tipo,
                lista,
                codigosOficiales,
                catalogo,
                nichoRepository.findByActivoTrueOrderByNombreAsc(),
                integrantes
        );

        guardarActividades(proveedor, lista, codigosOficiales);

        ProveedorClasificacion clasificacion = clasificacionRepository
                .findByProveedorIdProveedor(idProveedor)
                .orElseGet(() -> ProveedorClasificacion.builder().proveedor(proveedor).build());

        clasificacion.setTipoProveedor(tipo);
        clasificacion.setEstado(resultado.estado());
        clasificacion.setNichos(new HashSet<>(resultado.nichos()));
        clasificacion.setMotivosRevision(unir(resultado.motivos()));
        clasificacion.setRequisitosAdicionales(unir(resultado.requisitosAdicionales()));
        clasificacion.setVersionCiiu(ActividadCiiu.VERSION_VIGENTE);
        clasificacion.setFechaClasificacion(LocalDateTime.now());
        // Una reclasificación invalida cualquier resolución manual anterior.
        clasificacion.setJustificacion(null);
        clasificacion.setResueltoPor(null);
        clasificacion.setFechaResolucion(null);

        return clasificacionRepository.save(clasificacion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProveedorClasificacion> obtener(Long idProveedor) {
        return clasificacionRepository.findByProveedorIdProveedor(idProveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorClasificacion> listarEnRevision() {
        return clasificacionRepository
                .findByEstadoOrderByFechaClasificacionAsc(EstadoClasificacion.EN_REVISION);
    }

    @Override
    public ProveedorClasificacion resolverManualmente(
            Long idProveedor,
            Set<Long> idsNichos,
            String justificacion,
            String usuario
    ) {
        ProveedorClasificacion clasificacion = obtenerEnRevision(idProveedor);
        validarResolucion(justificacion, usuario);

        if (idsNichos == null || idsNichos.isEmpty()) {
            throw new IllegalArgumentException("Debe asignar al menos un nicho.");
        }

        List<Nicho> nichos = nichoRepository.findAllById(idsNichos);
        if (nichos.size() != idsNichos.size()
                || nichos.stream().anyMatch(n -> !Boolean.TRUE.equals(n.getActivo()))) {
            throw new IllegalArgumentException("Alguno de los nichos no existe o está inactivo.");
        }

        clasificacion.setNichos(new HashSet<>(nichos));
        return cerrar(clasificacion, EstadoClasificacion.MANUAL, justificacion, usuario);
    }

    @Override
    public ProveedorClasificacion rechazar(Long idProveedor, String justificacion, String usuario) {
        ProveedorClasificacion clasificacion = obtenerEnRevision(idProveedor);
        validarResolucion(justificacion, usuario);

        clasificacion.getNichos().clear();
        return cerrar(clasificacion, EstadoClasificacion.RECHAZADA, justificacion, usuario);
    }

    private List<ClasificadorProveedor.Integrante> cargarIntegrantes(Long idPluralidad) {
        return integranteRepository.findByPluralidadIdProveedor(idPluralidad).stream()
                .map(i -> {
                    Long idIntegrante = i.getIntegrante().getIdProveedor();
                    Set<Nicho> nichos = clasificacionRepository.findByProveedorIdProveedor(idIntegrante)
                            .filter(c -> c.getEstado() == EstadoClasificacion.AUTOMATICA
                                    || c.getEstado() == EstadoClasificacion.MANUAL)
                            .map(ProveedorClasificacion::getNichos)
                            .orElse(null);
                    return new ClasificadorProveedor.Integrante(
                            i.getIntegrante().getRazonSocial(),
                            i.getPorcentajeParticipacion(),
                            nichos
                    );
                })
                .toList();
    }

    private void guardarActividades(
            Proveedor proveedor,
            List<ActividadDeclarada> actividades,
            Set<String> codigosOficiales
    ) {
        proveedorActividadRepository.deleteByProveedorIdProveedor(proveedor.getIdProveedor());

        List<ProveedorActividad> nuevas = actividades.stream()
                .filter(a -> a.codigo() != null && a.tipo() != null)
                .map(a -> ProveedorActividad.builder()
                        .proveedor(proveedor)
                        .codigoCiiu(a.codigo().trim())
                        .tipo(a.tipo())
                        .verificada(codigosOficiales != null
                                && codigosOficiales.contains(a.codigo().trim()))
                        .build())
                .toList();

        proveedorActividadRepository.saveAll(nuevas);
    }

    private ProveedorClasificacion obtenerEnRevision(Long idProveedor) {
        ProveedorClasificacion clasificacion = clasificacionRepository
                .findByProveedorIdProveedor(idProveedor)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El proveedor no tiene una clasificación registrada."));

        if (clasificacion.getEstado() != EstadoClasificacion.EN_REVISION) {
            throw new IllegalStateException(
                    "Solo se pueden resolver clasificaciones en revisión (estado actual: "
                            + clasificacion.getEstado() + ").");
        }

        return clasificacion;
    }

    private void validarResolucion(String justificacion, String usuario) {
        if (justificacion == null || justificacion.trim().length() < MIN_JUSTIFICACION) {
            throw new IllegalArgumentException(
                    "La justificación es obligatoria (mínimo " + MIN_JUSTIFICACION + " caracteres).");
        }

        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("Debe registrarse el usuario que resuelve.");
        }
    }

    private ProveedorClasificacion cerrar(
            ProveedorClasificacion clasificacion,
            EstadoClasificacion estado,
            String justificacion,
            String usuario
    ) {
        clasificacion.setEstado(estado);
        clasificacion.setJustificacion(justificacion.trim());
        clasificacion.setResueltoPor(usuario.trim());
        clasificacion.setFechaResolucion(LocalDateTime.now());
        return clasificacionRepository.save(clasificacion);
    }

    private static String unir(List<String> textos) {
        return textos.isEmpty() ? null : String.join(SEPARADOR, textos);
    }
}
