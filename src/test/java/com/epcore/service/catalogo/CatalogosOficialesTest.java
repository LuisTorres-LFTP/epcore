package com.epcore.service.catalogo;

import com.epcore.entity.*;
import com.epcore.multicliente.ClientesConfigurados;
import com.epcore.repository.*;
import com.epcore.service.notificacion.NotificacionService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CatalogosOficialesTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneId.of("America/Bogota"));

    private static ByteArrayInputStream csv(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    // ---------- CIIU ----------

    private final ActividadCiiuRepository ciiu = mock(ActividadCiiuRepository.class);
    private final ProveedorActividadRepository actividades = mock(ProveedorActividadRepository.class);
    private final ProveedorClasificacionRepository clasificaciones = mock(ProveedorClasificacionRepository.class);
    private final ImportadorCiiuService importador = new ImportadorCiiuService(ciiu, actividades, clasificaciones);

    private static String catalogo(int desde, int cantidad) {
        StringBuilder sb = new StringBuilder("codigo;descripcion\nSECCIÓN A;Agricultura\n");
        IntStream.range(desde, desde + cantidad).forEach(i -> sb.append(String.format("%04d;Actividad %d%n", i, i)));
        return sb.toString();
    }

    @Test
    void ciiuAgregaActualizaYRetiraSinBorrar() throws Exception {
        ActividadCiiu igual = ActividadCiiu.builder().codigo("0111").descripcion("Actividad 111").activo(true).build();
        ActividadCiiu cambiada = ActividadCiiu.builder().codigo("0112").descripcion("Texto viejo").activo(true).build();
        ActividadCiiu retirada = ActividadCiiu.builder().codigo("9999").descripcion("Ya no existe").activo(true).build();
        when(ciiu.findAll()).thenReturn(List.of(igual, cambiada, retirada));

        Proveedor p = Proveedor.builder().idProveedor(7L).build();
        ProveedorActividad pa = mock(ProveedorActividad.class);
        when(pa.getCodigoCiiu()).thenReturn("9999");
        when(pa.getProveedor()).thenReturn(p);
        when(actividades.findAll()).thenReturn(List.of(pa));
        ProveedorClasificacion c = new ProveedorClasificacion();
        c.setEstado(EstadoClasificacion.AUTOMATICA);
        when(clasificaciones.findByProveedorIdProveedor(7L)).thenReturn(Optional.of(c));

        ImportadorCiiuService.Resumen r = importador.importar(csv(catalogo(111, 12)), null);

        assertEquals(10, r.nuevos().size());              // 0113..0122
        assertEquals(List.of("0112"), r.modificados());
        assertEquals(List.of("9999"), r.retirados());
        assertFalse(retirada.getActivo(), "el código retirado se desactiva, no se borra");
        verify(ciiu, never()).delete(any());
        assertEquals("Actividad 112", cambiada.getDescripcion());
        assertEquals(1, r.proveedoresAfectados());
        assertEquals(EstadoClasificacion.EN_REVISION, c.getEstado());
        assertTrue(c.getMotivosRevision().contains("CIIU"));
        assertEquals(1, r.lineasIgnoradas().size(), "la fila de sección se reporta como ignorada");
    }

    @Test
    void ciiuRechazaArchivoIncompleto() {
        assertThrows(IllegalArgumentException.class, () -> importador.importar(csv(catalogo(111, 5)), null));
        verify(ciiu, never()).save(any());
    }

    // ---------- Listas restrictivas ----------

    private final EntradaListaRestrictivaRepository entradas = mock(EntradaListaRestrictivaRepository.class);
    private final ProveedorRepository proveedores = mock(ProveedorRepository.class);
    private final ConsultaSarlaftRepository consultas = mock(ConsultaSarlaftRepository.class);
    private final NotificacionService notificaciones = mock(NotificacionService.class);
    private final ListasRestrictivasService listas = new ListasRestrictivasService(entradas, proveedores, consultas,
            notificaciones, mock(ClientesConfigurados.class), RELOJ, false, "http://no-usar", "http://no-usar");

    @Test
    void normalizaTildesSignosYSufijos() {
        assertEquals("CONSTRUCCIONES PEREZ", ListasRestrictivasService.normalizar("Construcciones Pérez S.A.S."));
        assertEquals("CONSTRUCCIONES PEREZ", ListasRestrictivasService.normalizar("CONSTRUCCIONES PEREZ LTDA"));
    }

    @Test
    void parseaOfacYOnu() throws Exception {
        String sdn = "36,\"AEROCARIBBEAN AIRLINES\",-0- ,\"CUBA\",-0- \n2674,\"ABDUL RAHMAN, Mohammed\",\"individual\",\"SDGT\"\n";
        assertEquals(List.of("AEROCARIBBEAN AIRLINES", "ABDUL RAHMAN  Mohammed"),
                ListasRestrictivasService.parsearOfac(csv(sdn)));
        String xml = "<CONSOLIDATED_LIST><INDIVIDUALS><INDIVIDUAL><FIRST_NAME>JUAN</FIRST_NAME><SECOND_NAME>PEREZ</SECOND_NAME>"
                + "<THIRD_NAME></THIRD_NAME></INDIVIDUAL></INDIVIDUALS><ENTITIES><ENTITY><FIRST_NAME>EMPRESA FICTICIA</FIRST_NAME>"
                + "</ENTITY></ENTITIES></CONSOLIDATED_LIST>";
        assertEquals(List.of("JUAN PEREZ", "EMPRESA FICTICIA"), ListasRestrictivasService.parsearOnu(csv(xml)));
    }

    private List<String> listaDePrueba(String... extra) {
        List<String> l = new ArrayList<>();
        IntStream.range(0, 10).forEach(i -> l.add("PERSONA DE PRUEBA NUMERO " + i));
        l.addAll(List.of(extra));
        return l;
    }

    @Test
    void coincidenciaGeneraAlertaYAvisaAlOficial() {
        Proveedor p = Proveedor.builder().idProveedor(5L).razonSocial("Empresa Ficticia S.A.S.").nit("900.000.001-1")
                .representanteLegal("Ana Gómez").build();
        when(proveedores.findAll()).thenReturn(List.of(p));
        when(entradas.findByNombreNormalizadoIn(anyList())).thenAnswer(inv -> {
            List<String> nombres = inv.getArgument(0);
            return nombres.contains("EMPRESA FICTICIA")
                    ? List.of(EntradaListaRestrictiva.builder().lista(ListaRestrictiva.ONU).nombre("EMPRESA FICTICIA")
                    .nombreNormalizado("EMPRESA FICTICIA").build()) : List.of();
        });
        when(consultas.findFirstByProveedorIdProveedorAndListaOrderByFechaConsultaDesc(5L, ListaRestrictiva.ONU))
                .thenReturn(Optional.empty());

        ListasRestrictivasService.Resultado r = listas.cargarYCruzar(ListaRestrictiva.ONU, listaDePrueba("EMPRESA FICTICIA"));

        assertEquals(11, r.nombres());
        assertEquals(1, r.coincidencias());
        verify(entradas).borrarLista(ListaRestrictiva.ONU);
        verify(consultas).save(argThat(c -> c.getResultado() == ResultadoSarlaft.POSIBLE_COINCIDENCIA
                && ListasRestrictivasService.USUARIO_SISTEMA.equals(c.getUsuarioResponsable())));
        verify(notificaciones).notificarRol(anyString(), eq(Rol.OFICIAL_CUMPLIMIENTO), anyString(), anyString(),
                eq("/proveedores/5/expediente"), eq(true));
    }

    @Test
    void noRepiteAlertaYaGeneradaPorElSistema() {
        Proveedor p = Proveedor.builder().idProveedor(5L).razonSocial("Empresa Ficticia").build();
        when(proveedores.findAll()).thenReturn(List.of(p));
        when(entradas.findByNombreNormalizadoIn(anyList())).thenReturn(List.of(EntradaListaRestrictiva.builder()
                .lista(ListaRestrictiva.OFAC).nombre("EMPRESA FICTICIA").nombreNormalizado("EMPRESA FICTICIA").build()));
        when(consultas.findFirstByProveedorIdProveedorAndListaOrderByFechaConsultaDesc(5L, ListaRestrictiva.OFAC))
                .thenReturn(Optional.of(ConsultaSarlaft.builder().resultado(ResultadoSarlaft.POSIBLE_COINCIDENCIA)
                        .usuarioResponsable(ListasRestrictivasService.USUARIO_SISTEMA).build()));

        assertEquals(0, listas.cargarYCruzar(ListaRestrictiva.OFAC, listaDePrueba("EMPRESA FICTICIA")).coincidencias());
        verify(consultas, never()).save(any());
        verifyNoInteractions(notificaciones);
    }

    @Test
    void rechazaListaIncompletaOListaNoAutomatica() {
        assertThrows(IllegalArgumentException.class, () -> listas.cargarYCruzar(ListaRestrictiva.OFAC, List.of("UNO")));
        assertThrows(IllegalArgumentException.class, () -> listas.cargarYCruzar(ListaRestrictiva.PROCURADURIA, listaDePrueba()));
        verify(entradas, never()).borrarLista(any());
    }

    @Test
    void descargaAutomaticaDesactivadaPorDefecto() {
        listas.actualizacionDiaria();
        verifyNoInteractions(entradas, proveedores, consultas, notificaciones);
    }
}
