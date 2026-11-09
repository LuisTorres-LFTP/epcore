package com.epcore.service.proceso;

import com.epcore.entity.InformacionFinanciera;
import com.epcore.entity.Proveedor;
import com.epcore.entity.proceso.*;
import com.epcore.entity.proceso.ProcesoEnums.*;
import com.epcore.repository.ProveedorRepository;
import com.epcore.repository.proceso.AdendaRepository;
import com.epcore.repository.proceso.OfertaRepository;
import com.epcore.repository.proceso.ProcesoRepository;
import com.epcore.security.SeparacionFunciones;
import com.epcore.service.ConsultaSarlaftService;
import com.epcore.service.ContratoExperienciaService;
import com.epcore.service.InformacionFinancieraService;
import com.epcore.service.ProveedorDocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Ciclo de vida de un proceso de contratación:
 * borrador -> aprobación dual de términos -> publicado (ofertas y adendas)
 * -> evaluación (habilitación + puntaje) -> adjudicado o desierto.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProcesoService {

    private final ProcesoRepository procesoRepository;
    private final OfertaRepository ofertaRepository;
    private final AdendaRepository adendaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ReglasPorModalidad reglas;
    private final EvaluadorOfertas evaluador;
    private final InformacionFinancieraService informacionFinancieraService;
    private final ContratoExperienciaService contratoExperienciaService;
    private final ConsultaSarlaftService consultaSarlaftService;
    private final ProveedorDocumentoService proveedorDocumentoService;
    private final Clock reloj;

    // ---------- Creación y requisitos ----------

    public Proceso crearBorrador(String codigo, String nombre, String entidad, Modalidad modalidad,
                                 Visibilidad visibilidad, BigDecimal presupuesto, LocalDateTime cierre,
                                 Set<Long> invitados, String usuario) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("El código es obligatorio.");
        if (procesoRepository.existsByCodigo(codigo.trim())) throw new IllegalArgumentException("Ya existe un proceso con el código " + codigo + ".");
        if (nombre == null || nombre.isBlank() || entidad == null || entidad.isBlank())
            throw new IllegalArgumentException("El objeto y la entidad contratante son obligatorios.");
        if (presupuesto == null || presupuesto.signum() <= 0) throw new IllegalArgumentException("El presupuesto debe ser mayor que cero.");
        if (cierre == null || !cierre.isAfter(ahora())) throw new IllegalArgumentException("La fecha de cierre debe ser futura.");

        ReglasModalidad r = reglas.de(modalidad);
        Visibilidad vis = modalidad == Modalidad.PUBLICA ? Visibilidad.SECOP
                : (visibilidad == null || visibilidad == Visibilidad.SECOP ? r.visibilidadPorDefecto() : visibilidad);

        Proceso p = Proceso.builder().codigo(codigo.trim()).nombre(nombre.trim()).entidadContratante(entidad.trim())
                .modalidad(modalidad).visibilidad(vis).presupuesto(presupuesto).fechaCierre(cierre)
                .estado(EstadoProceso.BORRADOR).requisitos(r.plantillaRequisitos()).factores(r.plantillaFactores())
                .creadoPor(usuario).fechaCreacion(ahora()).build();
        if (vis == Visibilidad.INVITACION && invitados != null) {
            p.getInvitados().addAll(proveedorRepository.findAllById(invitados));
        }
        return procesoRepository.save(p);
    }

    public Proceso ajustarTerminos(Long id, RequisitosProceso requisitos, FactoresPuntaje factores, String usuario) {
        Proceso p = obtener(id);
        exigir(p, EstadoProceso.BORRADOR, "Los términos solo se ajustan en borrador; si ya está publicado, emita una adenda.");
        validarFactores(p.getModalidad(), factores);
        p.setRequisitos(requisitos);
        p.setFactores(factores);
        return procesoRepository.save(p);
    }

    // ---------- Aprobación dual de términos ----------

    public Proceso enviarAAprobacion(Long id, String usuario) {
        Proceso p = obtener(id);
        exigir(p, EstadoProceso.BORRADOR, "Solo se envían a aprobación procesos en borrador.");
        validarFactores(p.getModalidad(), p.getFactores());
        if (p.getVisibilidad() == Visibilidad.INVITACION && p.getInvitados().isEmpty()) {
            throw new IllegalStateException("Un proceso por invitación debe tener al menos un proveedor invitado.");
        }
        p.setEstado(EstadoProceso.EN_APROBACION);
        p.setAprobadoNivel1Por(null);
        p.setFechaAprobacionNivel1(null);
        return procesoRepository.save(p);
    }

    public Proceso aprobarNivel1(Long id, String usuario) {
        Proceso p = obtener(id);
        exigir(p, EstadoProceso.EN_APROBACION, "El proceso no está en aprobación.");
        if (p.getAprobadoNivel1Por() != null) throw new IllegalStateException("La primera aprobación ya fue registrada.");
        SeparacionFunciones.validarAprobacion(usuario, p.getCreadoPor(), null);
        p.setAprobadoNivel1Por(usuario);
        p.setFechaAprobacionNivel1(ahora());
        return procesoRepository.save(p);
    }

    public Proceso aprobarNivel2YPublicar(Long id, String usuario) {
        Proceso p = obtener(id);
        exigir(p, EstadoProceso.EN_APROBACION, "El proceso no está en aprobación.");
        if (p.getAprobadoNivel1Por() == null) throw new IllegalStateException("Falta la primera aprobación (Director de compras).");
        SeparacionFunciones.validarAprobacion(usuario, p.getCreadoPor(), p.getAprobadoNivel1Por());
        if (!p.getFechaCierre().isAfter(ahora())) throw new IllegalStateException("La fecha de cierre ya pasó; devuelva el proceso para ajustarla.");
        p.setAprobadoNivel2Por(usuario);
        p.setFechaPublicacion(ahora());
        p.setEstado(EstadoProceso.PUBLICADO);
        return procesoRepository.save(p);
    }

    public Proceso devolver(Long id, String usuario, String motivo) {
        Proceso p = obtener(id);
        exigir(p, EstadoProceso.EN_APROBACION, "Solo se devuelven procesos en aprobación.");
        if (motivo == null || motivo.trim().length() < 10) throw new IllegalArgumentException("Explique el motivo de la devolución.");
        p.setEstado(EstadoProceso.BORRADOR);
        p.setAprobadoNivel1Por(null);
        p.setFechaAprobacionNivel1(null);
        p.setObservacionCierre("Devuelto por " + usuario + ": " + motivo.trim());
        return procesoRepository.save(p);
    }

    // ---------- Adendas ----------

    public Adenda emitirAdenda(Long id, RequisitosProceso nuevos, FactoresPuntaje nuevosFactores,
                               LocalDateTime nuevoCierre, String justificacion, String usuario) {
        Proceso p = obtener(id);
        exigir(p, EstadoProceso.PUBLICADO, "Solo se emiten adendas sobre procesos publicados.");
        if (justificacion == null || justificacion.trim().length() < 15)
            throw new IllegalArgumentException("La justificación de la adenda es obligatoria (mínimo 15 caracteres).");

        List<String> cambios = new ArrayList<>();
        if (nuevos != null) {
            compararRequisitos(p.getRequisitos(), nuevos, cambios);
            p.setRequisitos(nuevos);
        }
        if (nuevosFactores != null) {
            validarFactores(p.getModalidad(), nuevosFactores);
            if (!mismosFactores(p.getFactores(), nuevosFactores)) cambios.add("Factores de puntaje ajustados");
            p.setFactores(nuevosFactores);
        }
        if (nuevoCierre != null && !nuevoCierre.equals(p.getFechaCierre())) {
            if (!nuevoCierre.isAfter(ahora())) throw new IllegalArgumentException("La nueva fecha de cierre debe ser futura.");
            cambios.add("Fecha de cierre: " + p.getFechaCierre() + " -> " + nuevoCierre);
            p.setFechaCierre(nuevoCierre);
        }
        if (cambios.isEmpty()) throw new IllegalArgumentException("La adenda no contiene cambios.");

        procesoRepository.save(p);
        int numero = (int) adendaRepository.countByProcesoIdProceso(id) + 1;
        return adendaRepository.save(Adenda.builder().proceso(p).numero(numero).justificacion(justificacion.trim())
                .cambios(String.join("\n", cambios)).emitidaPor(usuario).fechaEmision(ahora()).build());
    }

    // ---------- Ofertas ----------

    public Oferta registrarOferta(Long idProceso, Long idProveedor, BigDecimal valor, int plazoDias,
                                  boolean industriaNacional, CanalOferta canal, boolean declaracionConflicto,
                                  String usuario) {
        Proceso p = obtener(idProceso);
        exigir(p, EstadoProceso.PUBLICADO, "El proceso no está recibiendo ofertas.");
        if (ahora().isAfter(p.getFechaCierre())) throw new IllegalStateException("La recepción de ofertas ya cerró.");
        if (p.getModalidad() == Modalidad.PUBLICA && canal == CanalOferta.PORTAL)
            throw new IllegalStateException("En licitación pública la oferta oficial se presenta en SECOP II.");
        Proveedor prov = proveedorRepository.findById(idProveedor)
                .orElseThrow(() -> new IllegalArgumentException("No existe el proveedor " + idProveedor + "."));
        if (p.getVisibilidad() == Visibilidad.INVITACION
                && p.getInvitados().stream().noneMatch(i -> i.getIdProveedor().equals(idProveedor)))
            throw new IllegalStateException("El proveedor no fue invitado a este proceso.");
        if (valor == null || valor.signum() <= 0) throw new IllegalArgumentException("El valor ofertado debe ser mayor que cero.");
        if (valor.compareTo(p.getPresupuesto()) > 0) throw new IllegalArgumentException("La oferta supera el presupuesto oficial.");
        if (plazoDias <= 0) throw new IllegalArgumentException("El plazo debe ser mayor que cero.");
        if (!declaracionConflicto) throw new IllegalArgumentException("Debe aceptarse la declaración de ausencia de conflicto de interés.");

        // Antes del cierre el proveedor puede reemplazar su oferta.
        Oferta o = ofertaRepository.findByProcesoIdProcesoAndProveedorIdProveedor(idProceso, idProveedor)
                .orElseGet(() -> Oferta.builder().proceso(p).proveedor(prov).build());
        o.setValor(valor);
        o.setPlazoDias(plazoDias);
        o.setIndustriaNacional(industriaNacional);
        o.setCanal(canal);
        o.setDeclaracionConflicto(true);
        o.setRegistradaPor(usuario);
        o.setFechaPresentacion(ahora());
        o.setCalificacionTecnica(null);
        return ofertaRepository.save(o);
    }

    public Proceso cerrarRecepcion(Long id, String usuario) {
        Proceso p = obtener(id);
        exigir(p, EstadoProceso.PUBLICADO, "Solo se cierra la recepción de procesos publicados.");
        p.setEstado(EstadoProceso.EN_EVALUACION);
        return procesoRepository.save(p);
    }

    public Oferta calificarTecnica(Long idOferta, int calificacion, String usuario) {
        Oferta o = ofertaRepository.findById(idOferta)
                .orElseThrow(() -> new IllegalArgumentException("No existe la oferta " + idOferta + "."));
        exigir(o.getProceso(), EstadoProceso.EN_EVALUACION, "La calificación técnica se registra durante la evaluación.");
        if (calificacion < 0 || calificacion > 100) throw new IllegalArgumentException("La calificación técnica va de 0 a 100.");
        if (usuario != null && usuario.equalsIgnoreCase(o.getProceso().getCreadoPor()))
            throw new IllegalStateException("Quien estructuró el proceso no puede calificar técnicamente las ofertas.");
        o.setCalificacionTecnica(calificacion);
        o.setCalificadoPor(usuario);
        return ofertaRepository.save(o);
    }

    // ---------- Evaluación, adjudicación y desierto ----------

    @Transactional(readOnly = true)
    public List<EvaluadorOfertas.Resultado> evaluar(Long id) {
        Proceso p = obtener(id);
        List<EvaluadorOfertas.Entrada> entradas = ofertaRepository.findByProcesoIdProceso(id).stream()
                .map(o -> entrada(p, o)).toList();
        return evaluador.evaluar(entradas, p.getFactores());
    }

    /** Requisitos habilitantes que no cumple el proveedor (vacío = habilitado). */
    @Transactional(readOnly = true)
    public List<String> verificarHabilitacion(Proceso p, Long idProveedor) {
        RequisitosProceso r = p.getRequisitos();
        List<String> fallas = new ArrayList<>();

        if (Boolean.TRUE.equals(r.getExigirDocumentosAprobados())) {
            Double pct = proveedorDocumentoService.calcularPorcentajeCumplimiento(idProveedor);
            if (pct == null || pct < 100.0) fallas.add("Documentos obligatorios aprobados: " + (pct == null ? 0 : pct) + " % (se exige 100 %)");
        }

        InformacionFinanciera info = informacionFinancieraService.obtenerMasReciente(idProveedor).orElse(null);
        BigDecimal patrimonioMin = r.getPatrimonioMinimoPct() == null || r.getPatrimonioMinimoPct() == 0 ? null
                : p.getPresupuesto().multiply(BigDecimal.valueOf(r.getPatrimonioMinimoPct())).divide(BigDecimal.valueOf(100));
        fallas.addAll(informacionFinancieraService.verificarCapacidadFinanciera(info, r.getLiquidezMinima(),
                r.getEndeudamientoMaximo(), r.getCoberturaInteresesMinima(), patrimonioMin));

        List<String> codigos = codigos(r.getCodigosUnspsc());
        Map<Long, com.epcore.entity.ContratoExperiencia> contratos = new LinkedHashMap<>();
        for (String c : codigos.isEmpty() ? Collections.<String>singletonList(null) : codigos) {
            contratoExperienciaService.listarAcreditables(idProveedor, c, true)
                    .forEach(ct -> contratos.put(ct.getIdContrato(), ct));
        }
        int minimos = r.getContratosMinimos() == null ? 0 : r.getContratosMinimos();
        if (contratos.size() < minimos) fallas.add("Acredita " + contratos.size() + " contrato(s) certificados; se exigen " + minimos);
        if (r.getExperienciaMinimaPct() != null && r.getExperienciaMinimaPct() > 0) {
            BigDecimal suma = contratos.values().stream().map(com.epcore.entity.ContratoExperiencia::getValorAcreditable)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal exigida = p.getPresupuesto().multiply(BigDecimal.valueOf(r.getExperienciaMinimaPct())).divide(BigDecimal.valueOf(100));
            if (suma.compareTo(exigida) < 0) fallas.add("Experiencia acreditable " + suma.toPlainString() + " menor a la exigida " + exigida.toPlainString());
        }

        if (Boolean.TRUE.equals(r.getExigirSarlaft()) && !consultaSarlaftService.estaHabilitado(idProveedor)) {
            fallas.add("SARLAFT: " + consultaSarlaftService.calcularEstado(idProveedor));
        }
        return fallas;
    }

    public Proceso adjudicar(Long id, String usuario) {
        Proceso p = obtener(id);
        exigir(p, EstadoProceso.EN_EVALUACION, "Solo se adjudican procesos en evaluación.");
        if (usuario != null && usuario.equalsIgnoreCase(p.getCreadoPor()))
            throw new IllegalStateException("Quien estructuró el proceso no puede adjudicarlo.");

        List<EvaluadorOfertas.Resultado> res = evaluar(id);
        EvaluadorOfertas.Resultado mejor = res.stream().filter(EvaluadorOfertas.Resultado::habilitada).findFirst()
                .orElseThrow(() -> new IllegalStateException("Ninguna oferta está habilitada: declare el proceso desierto."));
        if (res.stream().anyMatch(x -> x.habilitada() && x.pendienteCalificacion()))
            throw new IllegalStateException("Falta la calificación técnica de alguna oferta habilitada.");

        p.setAdjudicadoA(proveedorRepository.findById(mejor.idProveedor()).orElseThrow());
        p.setAdjudicadoPor(usuario);
        p.setFechaAdjudicacion(ahora());
        p.setObservacionCierre("Adjudicado con " + mejor.total() + " puntos.");
        p.setEstado(EstadoProceso.ADJUDICADO);
        return procesoRepository.save(p);
    }

    public Proceso declararDesierto(Long id, String usuario, String motivo) {
        Proceso p = obtener(id);
        exigir(p, EstadoProceso.EN_EVALUACION, "Solo se declaran desiertos procesos en evaluación.");
        if (motivo == null || motivo.trim().length() < 15) throw new IllegalArgumentException("Explique el motivo (mínimo 15 caracteres).");
        p.setEstado(EstadoProceso.DESIERTO);
        p.setAdjudicadoPor(usuario);
        p.setFechaAdjudicacion(ahora());
        p.setObservacionCierre(motivo.trim());
        return procesoRepository.save(p);
    }

    @Transactional(readOnly = true)
    public Proceso obtener(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("El identificador del proceso no es válido.");
        return procesoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No existe el proceso " + id + "."));
    }

    // ---------- Apoyo ----------

    private EvaluadorOfertas.Entrada entrada(Proceso p, Oferta o) {
        Long idProv = o.getProveedor().getIdProveedor();
        List<String> fallas = new ArrayList<>(verificarHabilitacion(p, idProv));
        if (o.getValor().compareTo(p.getPresupuesto()) > 0) fallas.add("La oferta supera el presupuesto");
        List<String> codigos = codigos(p.getRequisitos().getCodigosUnspsc());
        BigDecimal exp = contratoExperienciaService.sumarExperienciaAcreditable(idProv, codigos.isEmpty() ? null : codigos.get(0), true);
        return new EvaluadorOfertas.Entrada(o.getIdOferta(), idProv, o.getValor(), o.getPlazoDias(),
                o.getCalificacionTecnica(), Boolean.TRUE.equals(o.getIndustriaNacional()), exp, null, fallas);
    }

    private void validarFactores(Modalidad m, FactoresPuntaje f) {
        List<String> e = reglas.de(m).validarFactores(f);
        if (!e.isEmpty()) throw new IllegalArgumentException(String.join(" ", e));
    }

    private static void exigir(Proceso p, EstadoProceso estado, String mensaje) {
        if (p.getEstado() != estado) throw new IllegalStateException(mensaje + " Estado actual: " + p.getEstado() + ".");
    }

    private static List<String> codigos(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split("[,\\s]+")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    private static boolean mismosFactores(FactoresPuntaje a, FactoresPuntaje b) {
        return a.getPrecio() == b.getPrecio() && a.getTecnica() == b.getTecnica() && a.getPlazo() == b.getPlazo()
                && a.getExperiencia() == b.getExperiencia() && a.getDesempeno() == b.getDesempeno()
                && a.getIndustriaNacional() == b.getIndustriaNacional();
    }

    private static void compararRequisitos(RequisitosProceso a, RequisitosProceso b, List<String> cambios) {
        cmp("Liquidez mínima", a.getLiquidezMinima(), b.getLiquidezMinima(), cambios);
        cmp("Endeudamiento máximo", a.getEndeudamientoMaximo(), b.getEndeudamientoMaximo(), cambios);
        cmp("Cobertura de intereses mínima", a.getCoberturaInteresesMinima(), b.getCoberturaInteresesMinima(), cambios);
        cmp("Patrimonio mínimo (%)", a.getPatrimonioMinimoPct(), b.getPatrimonioMinimoPct(), cambios);
        cmp("Contratos mínimos", a.getContratosMinimos(), b.getContratosMinimos(), cambios);
        cmp("Experiencia mínima (%)", a.getExperienciaMinimaPct(), b.getExperienciaMinimaPct(), cambios);
        cmp("Códigos UNSPSC", a.getCodigosUnspsc(), b.getCodigosUnspsc(), cambios);
        cmp("Exigir SARLAFT", a.getExigirSarlaft(), b.getExigirSarlaft(), cambios);
        cmp("Exigir documentos aprobados", a.getExigirDocumentosAprobados(), b.getExigirDocumentosAprobados(), cambios);
    }

    private static void cmp(String campo, Object a, Object b, List<String> cambios) {
        boolean igual = a instanceof BigDecimal x && b instanceof BigDecimal y ? x.compareTo(y) == 0 : Objects.equals(a, b);
        if (!igual) cambios.add(campo + ": " + a + " -> " + b);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
