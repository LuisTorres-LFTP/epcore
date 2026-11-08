package com.epcore.service;

import com.epcore.entity.ActividadCiiu;
import com.epcore.entity.EstadoClasificacion;
import com.epcore.entity.Nicho;
import com.epcore.entity.TipoProveedor;
import com.epcore.service.ClasificadorProveedor.ActividadDeclarada;
import com.epcore.service.ClasificadorProveedor.Integrante;
import com.epcore.service.ClasificadorProveedor.Resultado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.epcore.entity.TipoActividad.*;
import static org.junit.jupiter.api.Assertions.*;

class ClasificadorProveedorTest {

    private final ClasificadorProveedor clasificador = new ClasificadorProveedor();

    private static final Nicho ASEO = Nicho.builder().idNicho(1L).nombre("Aseo y limpieza")
            .prefijosCiiu(Set.of("8121", "8129")).activo(true).build();
    private static final Nicho TECNOLOGIA = Nicho.builder().idNicho(2L).nombre("Tecnología")
            .prefijosCiiu(Set.of("62")).activo(true).build();
    private static final Nicho SEGURIDAD = Nicho.builder().idNicho(3L).nombre("Seguridad privada")
            .prefijosCiiu(Set.of("8010")).activo(true).build();
    private static final List<Nicho> NICHOS = List.of(ASEO, TECNOLOGIA, SEGURIDAD);

    private static final Map<String, ActividadCiiu> CATALOGO = Map.of(
            "8121", ciiu("8121", false, null, false),
            "6201", ciiu("6201", false, null, false),
            "8010", ciiu("8010", false, "Licencia de la Superintendencia de Vigilancia", false),
            "4690", ciiu("4690", true, null, false),
            "8299", ciiu("8299", true, null, false),
            "7820", ciiu("7820", false, null, true)
    );

    private static ActividadCiiu ciiu(String codigo, boolean generica, String requisito, boolean legal) {
        return ActividadCiiu.builder().codigo(codigo).descripcion("Actividad " + codigo)
                .generica(generica).requisitoRegulatorio(requisito)
                .requiereRevisionLegal(legal).activo(true).build();
    }

    private Resultado sociedad(List<ActividadDeclarada> actividades, Set<String> oficiales) {
        return clasificador.clasificar(TipoProveedor.PERSONA_JURIDICA, actividades, oficiales,
                CATALOGO, NICHOS, null);
    }

    @Test
    void clasificaAutomaticamenteCuandoCoincideConLaCamara() {
        Resultado r = sociedad(List.of(new ActividadDeclarada("8121", PRINCIPAL)), Set.of("8121"));

        assertEquals(EstadoClasificacion.AUTOMATICA, r.estado());
        assertEquals(Set.of(ASEO), r.nichos());
        assertTrue(r.motivos().isEmpty());
    }

    @Test
    void empresaMultiactividadQuedaEnVariosNichos() {
        Resultado r = sociedad(List.of(
                new ActividadDeclarada("8121", PRINCIPAL),
                new ActividadDeclarada("6201", SECUNDARIA)), Set.of("8121", "6201"));

        assertEquals(EstadoClasificacion.AUTOMATICA, r.estado());
        assertEquals(Set.of(ASEO, TECNOLOGIA), r.nichos());
    }

    @Test
    void ciiuQueNoApareceEnLaCamaraVaARevision() {
        Resultado r = sociedad(List.of(new ActividadDeclarada("8121", PRINCIPAL)), Set.of("4690"));

        assertEquals(EstadoClasificacion.EN_REVISION, r.estado());
        assertTrue(r.motivos().get(0).contains("no aparece en el Certificado de Cámara de Comercio"));
    }

    @Test
    void sinCertificadoCargadoQuedaPendienteDeVerificar() {
        Resultado r = sociedad(List.of(new ActividadDeclarada("8121", PRINCIPAL)), null);

        assertEquals(EstadoClasificacion.EN_REVISION, r.estado());
        assertTrue(r.motivos().get(0).startsWith("Falta cargar el Certificado de Cámara de Comercio"));
    }

    @Test
    void profesionalIndependienteSeVerificaConElRut() {
        // No exige Cámara de Comercio: basta con que el CIIU aparezca en el RUT.
        Resultado r = clasificador.clasificar(TipoProveedor.PERSONA_NATURAL_NO_COMERCIANTE,
                List.of(new ActividadDeclarada("6201", PRINCIPAL)), Set.of("6201"), CATALOGO, NICHOS, null);
        assertEquals(EstadoClasificacion.AUTOMATICA, r.estado());

        Resultado sinRut = clasificador.clasificar(TipoProveedor.PERSONA_NATURAL_NO_COMERCIANTE,
                List.of(new ActividadDeclarada("6201", PRINCIPAL)), null, CATALOGO, NICHOS, null);
        assertTrue(sinRut.motivos().get(0).startsWith("Falta cargar el RUT"));

        Resultado mal = clasificador.clasificar(TipoProveedor.PERSONA_NATURAL_NO_COMERCIANTE,
                List.of(new ActividadDeclarada("6201", PRINCIPAL)), Set.of("8121"), CATALOGO, NICHOS, null);
        assertTrue(mal.motivos().get(0).contains("no aparece en el RUT"));
    }

    @Test
    void actividadesGenericasVanARevision() {
        Resultado r = sociedad(List.of(
                new ActividadDeclarada("4690", PRINCIPAL),
                new ActividadDeclarada("8299", SECUNDARIA)), Set.of("4690", "8299"));

        assertEquals(EstadoClasificacion.EN_REVISION, r.estado());
        assertTrue(r.motivos().get(0).contains("genéricas"));
    }

    @Test
    void genericaMasEspecificaSeClasificaPorLaEspecifica() {
        Resultado r = sociedad(List.of(
                new ActividadDeclarada("4690", PRINCIPAL),
                new ActividadDeclarada("8121", SECUNDARIA)), Set.of("4690", "8121"));

        assertEquals(EstadoClasificacion.AUTOMATICA, r.estado());
        assertEquals(Set.of(ASEO), r.nichos());
    }

    @Test
    void actividadReguladaAgregaRequisitoSinBloquear() {
        Resultado r = sociedad(List.of(new ActividadDeclarada("8010", PRINCIPAL)), Set.of("8010"));

        assertEquals(EstadoClasificacion.AUTOMATICA, r.estado());
        assertEquals(1, r.requisitosAdicionales().size());
        assertTrue(r.requisitosAdicionales().get(0).startsWith("Licencia de la Superintendencia"));
    }

    @Test
    void serviciosTemporalesRequierenRevisionLegal() {
        Resultado r = sociedad(List.of(new ActividadDeclarada("7820", PRINCIPAL)), Set.of("7820"));

        assertEquals(EstadoClasificacion.EN_REVISION, r.estado());
        assertTrue(r.motivos().stream().anyMatch(m -> m.contains("área jurídica")));
    }

    @Test
    void validaLaEstructuraDelRut() {
        Resultado r = sociedad(List.of(
                new ActividadDeclarada("8121", SECUNDARIA),
                new ActividadDeclarada("6201", SECUNDARIA)), Set.of("8121", "6201"));

        assertEquals(EstadoClasificacion.EN_REVISION, r.estado());
        assertTrue(r.motivos().contains("Debe haber exactamente una actividad principal, como en el RUT."));
        assertTrue(r.motivos().stream().anyMatch(m -> m.startsWith("El RUT admite máximo 1")));
    }

    @Test
    void codigoInexistenteVaARevision() {
        Resultado r = sociedad(List.of(new ActividadDeclarada("9999", PRINCIPAL)), Set.of("9999"));

        assertEquals(EstadoClasificacion.EN_REVISION, r.estado());
        assertTrue(r.motivos().get(0).contains("no existe"));
    }

    @Test
    void consorcioSeClasificaPorSusIntegrantes() {
        Resultado r = clasificador.clasificar(TipoProveedor.CONSORCIO, null, null, CATALOGO, NICHOS, List.of(
                new Integrante("Limpieza Total SAS", BigDecimal.valueOf(60), Set.of(ASEO)),
                new Integrante("TecnoRed Ltda", BigDecimal.valueOf(40), Set.of(TECNOLOGIA))));

        assertEquals(EstadoClasificacion.AUTOMATICA, r.estado());
        assertEquals(Set.of(ASEO, TECNOLOGIA), r.nichos());
    }

    @Test
    void consorcioConPorcentajesIncorrectosOIntegranteSinClasificar() {
        Resultado r = clasificador.clasificar(TipoProveedor.UNION_TEMPORAL, null, null, CATALOGO, NICHOS, List.of(
                new Integrante("Limpieza Total SAS", BigDecimal.valueOf(60), Set.of(ASEO)),
                new Integrante("Nueva SAS", BigDecimal.valueOf(30), null)));

        assertEquals(EstadoClasificacion.EN_REVISION, r.estado());
        assertEquals(2, r.motivos().size());
    }

    @Test
    void extranjeraSinSucursalSiempreEsManual() {
        Resultado r = clasificador.clasificar(TipoProveedor.EXTRANJERA_SIN_SUCURSAL,
                List.of(new ActividadDeclarada("6201", PRINCIPAL)), null, CATALOGO, NICHOS, null);

        assertEquals(EstadoClasificacion.EN_REVISION, r.estado());
        assertTrue(r.motivos().get(0).contains("ISIC"));
    }
}
