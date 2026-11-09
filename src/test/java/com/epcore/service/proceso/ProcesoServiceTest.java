package com.epcore.service.proceso;

import com.epcore.entity.InformacionFinanciera;
import com.epcore.entity.Proveedor;
import com.epcore.entity.proceso.*;
import com.epcore.entity.proceso.ProcesoEnums.*;
import com.epcore.repository.ProveedorRepository;
import com.epcore.repository.proceso.AdendaRepository;
import com.epcore.repository.proceso.OfertaRepository;
import com.epcore.repository.proceso.ProcesoRepository;
import com.epcore.service.ConsultaSarlaftService;
import com.epcore.service.ContratoExperienciaService;
import com.epcore.service.InformacionFinancieraService;
import com.epcore.service.ProveedorDocumentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProcesoServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime HOY = LocalDateTime.now(RELOJ);

    private final ProcesoRepository procesos = mock(ProcesoRepository.class);
    private final OfertaRepository ofertas = mock(OfertaRepository.class);
    private final AdendaRepository adendas = mock(AdendaRepository.class);
    private final ProveedorRepository proveedores = mock(ProveedorRepository.class);
    private final InformacionFinancieraService financiera = mock(InformacionFinancieraService.class);
    private final ContratoExperienciaService experiencia = mock(ContratoExperienciaService.class);
    private final ConsultaSarlaftService sarlaft = mock(ConsultaSarlaftService.class);
    private final ProveedorDocumentoService documentos = mock(ProveedorDocumentoService.class);

    private final ProcesoService servicio = new ProcesoService(procesos, ofertas, adendas, proveedores,
            new ReglasPorModalidad(), new EvaluadorOfertas(), financiera, experiencia, sarlaft, documentos, RELOJ);

    private final Map<Long, Proceso> bd = new HashMap<>();
    private final List<Oferta> ofertasBd = new ArrayList<>();

    @BeforeEach
    void preparar() {
        when(procesos.save(any())).thenAnswer(i -> {
            Proceso p = i.getArgument(0);
            if (p.getIdProceso() == null) p.setIdProceso((long) bd.size() + 1);
            bd.put(p.getIdProceso(), p);
            return p;
        });
        when(procesos.findById(anyLong())).thenAnswer(i -> Optional.ofNullable(bd.get(i.<Long>getArgument(0))));
        for (long id = 1; id <= 3; id++) {
            Proveedor pr = Proveedor.builder().idProveedor(id).razonSocial("Proveedor " + id).nit("90000000" + id).build();
            when(proveedores.findById(id)).thenReturn(Optional.of(pr));
        }
        when(proveedores.findAllById(any())).thenAnswer(i -> {
            List<Proveedor> l = new ArrayList<>();
            for (Object id : (Iterable<?>) i.getArgument(0)) l.add(proveedores.findById((Long) id).orElseThrow());
            return l;
        });
        when(ofertas.save(any())).thenAnswer(i -> {
            Oferta o = i.getArgument(0);
            if (o.getIdOferta() == null) { o.setIdOferta((long) ofertasBd.size() + 1); ofertasBd.add(o); }
            return o;
        });
        when(ofertas.findByProcesoIdProcesoAndProveedorIdProveedor(anyLong(), anyLong())).thenAnswer(i ->
                ofertasBd.stream().filter(o -> o.getProveedor().getIdProveedor().equals(i.getArgument(1))).findFirst());
        when(ofertas.findByProcesoIdProceso(anyLong())).thenAnswer(i -> List.copyOf(ofertasBd));
        when(ofertas.findById(anyLong())).thenAnswer(i -> ofertasBd.stream().filter(o -> o.getIdOferta().equals(i.getArgument(0))).findFirst());
        when(adendas.save(any())).thenAnswer(i -> i.getArgument(0));

        // Proveedor 1 y 2 cumplen todo; el 3 no tiene información financiera.
        when(documentos.calcularPorcentajeCumplimiento(anyLong())).thenReturn(100.0);
        when(financiera.obtenerMasReciente(anyLong())).thenAnswer(i -> i.<Long>getArgument(0) == 3L ? Optional.empty()
                : Optional.of(InformacionFinanciera.builder().patrimonio(BigDecimal.valueOf(900_000_000L)).build()));
        when(financiera.verificarCapacidadFinanciera(any(), any(), any(), any(), any())).thenAnswer(i ->
                i.getArgument(0) == null ? List.of("Sin información financiera") : List.of());
        com.epcore.entity.ContratoExperiencia c = com.epcore.entity.ContratoExperiencia.builder().idContrato(1L)
                .valor(BigDecimal.valueOf(500_000_000L)).porcentajeParticipacion(BigDecimal.valueOf(100)).build();
        com.epcore.entity.ContratoExperiencia c2 = com.epcore.entity.ContratoExperiencia.builder().idContrato(2L)
                .valor(BigDecimal.valueOf(300_000_000L)).porcentajeParticipacion(BigDecimal.valueOf(100)).build();
        when(experiencia.listarAcreditables(anyLong(), any(), anyBoolean())).thenReturn(List.of(c, c2));
        when(experiencia.sumarExperienciaAcreditable(anyLong(), any(), anyBoolean())).thenReturn(BigDecimal.valueOf(800_000_000L));
        when(sarlaft.estaHabilitado(anyLong())).thenReturn(true);
    }

    private Proceso publicado() {
        Proceso p = servicio.crearBorrador("RFP-BC-2026-044", "Aseo y cafetería", "Banco Cafetero", Modalidad.FINANCIERA,
                Visibilidad.INVITACION, BigDecimal.valueOf(600_000_000L), HOY.plusDays(10), Set.of(1L, 2L, 3L), "analista@epcore.co");
        servicio.enviarAAprobacion(p.getIdProceso(), "analista@epcore.co");
        servicio.aprobarNivel1(p.getIdProceso(), "director@epcore.co");
        return servicio.aprobarNivel2YPublicar(p.getIdProceso(), "gerente@epcore.co");
    }

    @Test
    void creaBorradorConLaPlantillaDeLaModalidad() {
        Proceso p = servicio.crearBorrador("LP-01", "Aseo sedes", "Gobernación", Modalidad.PUBLICA, Visibilidad.ABIERTO,
                BigDecimal.TEN, HOY.plusDays(5), null, "analista@epcore.co");
        assertEquals(Visibilidad.SECOP, p.getVisibilidad(), "la licitación pública se publica en SECOP II");
        assertEquals(0, p.getFactores().getDesempeno());
        assertEquals(10, p.getFactores().getIndustriaNacional());
        assertEquals(EstadoProceso.BORRADOR, p.getEstado());
    }

    @Test
    void enLicitacionPublicaNoSePuntuaElDesempeno() {
        Proceso p = servicio.crearBorrador("LP-02", "Aseo", "Alcaldía", Modalidad.PUBLICA, null,
                BigDecimal.TEN, HOY.plusDays(5), null, "analista@epcore.co");
        FactoresPuntaje f = FactoresPuntaje.builder().precio(50).tecnica(30).desempeno(20).build();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicio.ajustarTerminos(p.getIdProceso(), p.getRequisitos(), f, "analista@epcore.co"));
        assertTrue(ex.getMessage().contains("desempeño"));
    }

    @Test
    void aprobacionDualDeTerminosConSeparacionDeFunciones() {
        Proceso p = servicio.crearBorrador("RFP-1", "TI", "Banco", Modalidad.FINANCIERA, Visibilidad.ABIERTO,
                BigDecimal.TEN, HOY.plusDays(5), null, "analista@epcore.co");
        servicio.enviarAAprobacion(p.getIdProceso(), "analista@epcore.co");

        assertThrows(IllegalStateException.class, () -> servicio.aprobarNivel2YPublicar(p.getIdProceso(), "gerente@epcore.co"),
                "el gerente no aprueba antes del director");
        assertThrows(IllegalStateException.class, () -> servicio.aprobarNivel1(p.getIdProceso(), "analista@epcore.co"),
                "quien crea no aprueba");
        servicio.aprobarNivel1(p.getIdProceso(), "director@epcore.co");
        assertThrows(IllegalStateException.class, () -> servicio.aprobarNivel2YPublicar(p.getIdProceso(), "director@epcore.co"),
                "el mismo usuario no aprueba los dos niveles");
        assertEquals(EstadoProceso.PUBLICADO, servicio.aprobarNivel2YPublicar(p.getIdProceso(), "gerente@epcore.co").getEstado());
    }

    @Test
    void reglasDeLasOfertas() {
        Proceso p = publicado();
        Long id = p.getIdProceso();
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarOferta(id, 1L, BigDecimal.valueOf(700_000_000L), 10,
                false, CanalOferta.PORTAL, true, "prov1"), "no puede superar el presupuesto");
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarOferta(id, 1L, BigDecimal.valueOf(500_000_000L), 10,
                false, CanalOferta.PORTAL, false, "prov1"), "exige la declaración de conflicto de interés");

        servicio.registrarOferta(id, 1L, BigDecimal.valueOf(550_000_000L), 15, false, CanalOferta.PORTAL, true, "prov1");
        servicio.registrarOferta(id, 1L, BigDecimal.valueOf(520_000_000L), 15, false, CanalOferta.PORTAL, true, "prov1");
        assertEquals(1, ofertasBd.size(), "reemplaza su oferta antes del cierre");
        assertEquals(0, BigDecimal.valueOf(520_000_000L).compareTo(ofertasBd.get(0).getValor()));
    }

    @Test
    void adendaRegistraCambiosYJustificacion() {
        Proceso p = publicado();
        RequisitosProceso nuevos = p.getRequisitos().toBuilder().liquidezMinima(new BigDecimal("1.3")).build();
        Adenda a = servicio.emitirAdenda(p.getIdProceso(), nuevos, null, HOY.plusDays(15),
                "A solicitud de los interesados se ajusta la liquidez y se amplía el plazo.", "analista@epcore.co");
        assertEquals(1, a.getNumero());
        assertTrue(a.getCambios().contains("Liquidez mínima"));
        assertTrue(a.getCambios().contains("Fecha de cierre"));
    }

    @Test
    void cicloCompletoHastaLaAdjudicacion() {
        Proceso p = publicado();
        Long id = p.getIdProceso();
        servicio.registrarOferta(id, 1L, BigDecimal.valueOf(580_000_000L), 15, false, CanalOferta.PORTAL, true, "prov1");
        servicio.registrarOferta(id, 2L, BigDecimal.valueOf(500_000_000L), 20, false, CanalOferta.INTERNO, true, "analista");
        servicio.registrarOferta(id, 3L, BigDecimal.valueOf(450_000_000L), 10, false, CanalOferta.PORTAL, true, "prov3");
        servicio.cerrarRecepcion(id, "analista@epcore.co");

        assertThrows(IllegalStateException.class, () -> servicio.adjudicar(id, "gerente@epcore.co"),
                "falta la calificación técnica");
        assertThrows(IllegalStateException.class, () -> servicio.calificarTecnica(1L, 80, "analista@epcore.co"),
                "quien estructuró no califica");
        servicio.calificarTecnica(1L, 95, "director@epcore.co");
        servicio.calificarTecnica(2L, 60, "director@epcore.co");

        List<EvaluadorOfertas.Resultado> r = servicio.evaluar(id);
        assertFalse(r.stream().filter(x -> x.idProveedor() == 3L).findFirst().orElseThrow().habilitada(),
                "el proveedor 3 no tiene información financiera");

        assertThrows(IllegalStateException.class, () -> servicio.adjudicar(id, "analista@epcore.co"),
                "quien estructuró no adjudica");
        Proceso adj = servicio.adjudicar(id, "gerente@epcore.co");
        assertEquals(EstadoProceso.ADJUDICADO, adj.getEstado());
        assertNotNull(adj.getAdjudicadoA());
    }

    @Test
    void desiertoCuandoNadieQuedaHabilitado() {
        Proceso p = publicado();
        servicio.registrarOferta(p.getIdProceso(), 3L, BigDecimal.valueOf(450_000_000L), 10, false, CanalOferta.PORTAL, true, "prov3");
        servicio.cerrarRecepcion(p.getIdProceso(), "analista@epcore.co");
        assertThrows(IllegalStateException.class, () -> servicio.adjudicar(p.getIdProceso(), "gerente@epcore.co"));
        assertEquals(EstadoProceso.DESIERTO, servicio.declararDesierto(p.getIdProceso(), "gerente@epcore.co",
                "Ninguna oferta cumplió los requisitos habilitantes.").getEstado());
    }

    @Test
    void elEvaluadorAplicaLasFormulas() {
        FactoresPuntaje f = FactoresPuntaje.builder().precio(50).tecnica(50).build();
        List<EvaluadorOfertas.Resultado> r = new EvaluadorOfertas().evaluar(List.of(
                new EvaluadorOfertas.Entrada(1L, 1L, BigDecimal.valueOf(100), 10, 100, false, BigDecimal.ZERO, null, List.of()),
                new EvaluadorOfertas.Entrada(2L, 2L, BigDecimal.valueOf(200), 10, 100, false, BigDecimal.ZERO, null, List.of())), f);
        assertEquals(100.0, r.get(0).total());   // menor precio: 50 + 50
        assertEquals(75.0, r.get(1).total());    // 100/200 x 50 + 50
    }
}
