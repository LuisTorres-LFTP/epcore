package com.epcore.service;

import com.epcore.entity.ProveedorDocumento;
import com.epcore.repository.ProveedorDocumentoRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AprobacionDocumentosTest {

    private final ProveedorDocumentoRepository repo = mock(ProveedorDocumentoRepository.class);
    private final DocumentoAprobacionService servicio = new DocumentoAprobacionService(repo);
    private final ValidadorDocumentoPdf validador = new ValidadorDocumentoPdf();

    private ProveedorDocumento doc(String estado) {
        ProveedorDocumento d = ProveedorDocumento.builder().idDocumento(1L).estadoValidacion(estado)
                .cargadoPor("ana.analista@epcore.co").tieneVencimiento(true)
                .fechaVencimiento(LocalDate.now().plusDays(60)).build();
        when(repo.findById(1L)).thenReturn(Optional.of(d));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        return d;
    }

    // ---------- Aprobación dual ----------

    @Test
    void flujoCompletoDirectorLuegoGerente() {
        ProveedorDocumento d = doc(DocumentoAprobacionService.PENDIENTE);

        servicio.aprobarNivel1(1L, "jorge.director@epcore.co");
        assertEquals(DocumentoAprobacionService.EN_APROBACION, d.getEstadoValidacion());
        assertEquals("jorge.director@epcore.co", d.getAprobadoNivel1Por());

        servicio.aprobarNivel2(1L, "laura.gerente@epcore.co");
        assertEquals(DocumentoAprobacionService.APROBADO, d.getEstadoValidacion());
        assertNotNull(d.getFechaAprobacionNivel2());
    }

    @Test
    void elGerenteNoPuedeAprobarAntesQueElDirector() {
        doc(DocumentoAprobacionService.PENDIENTE);
        assertThrows(IllegalStateException.class, () -> servicio.aprobarNivel2(1L, "laura.gerente@epcore.co"));
    }

    @Test
    void nadieApruebaLoQueCargo() {
        doc(DocumentoAprobacionService.PENDIENTE);
        assertThrows(IllegalStateException.class, () -> servicio.aprobarNivel1(1L, "ana.analista@epcore.co"));
    }

    @Test
    void elMismoUsuarioNoApruebaLosDosNiveles() {
        doc(DocumentoAprobacionService.PENDIENTE);
        servicio.aprobarNivel1(1L, "jorge.director@epcore.co");
        assertThrows(IllegalStateException.class, () -> servicio.aprobarNivel2(1L, "jorge.director@epcore.co"));
    }

    @Test
    void noSeApruebaUnDocumentoVencido() {
        ProveedorDocumento d = doc(DocumentoAprobacionService.PENDIENTE);
        d.setFechaVencimiento(LocalDate.now().minusDays(1));
        assertThrows(IllegalStateException.class, () -> servicio.aprobarNivel1(1L, "jorge.director@epcore.co"));
    }

    @Test
    void rechazarExigeMotivoYQuedaRegistrado() {
        ProveedorDocumento d = doc(DocumentoAprobacionService.EN_APROBACION);
        assertThrows(IllegalArgumentException.class, () -> servicio.rechazar(1L, "laura.gerente@epcore.co", "mal"));

        servicio.rechazar(1L, "laura.gerente@epcore.co", "El documento está ilegible y sin fecha.");
        assertEquals(DocumentoAprobacionService.RECHAZADO, d.getEstadoValidacion());
        assertEquals("laura.gerente@epcore.co", d.getRechazadoPor());
    }

    @Test
    void noSeAprueboUnDocumentoYaAprobado() {
        doc(DocumentoAprobacionService.APROBADO);
        assertThrows(IllegalStateException.class, () -> servicio.aprobarNivel1(1L, "jorge.director@epcore.co"));
    }

    // ---------- Validación automática de PDF ----------

    private static byte[] pdf(String... lineas) throws Exception {
        try (PDDocument d = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            d.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(d, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                for (String l : lineas) {
                    cs.showText(l);
                    cs.newLineAtOffset(0, -18);
                }
                cs.endText();
            }
            d.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void apruebaAutomaticamenteCuandoNitYFechaCoinciden() throws Exception {
        byte[] archivo = pdf("CAMARA DE COMERCIO DE ARMENIA", "Razon social: Brillo Express Ltda",
                "NIT: 900.640.118-2", "Fecha de expedicion: 03/11/2026");

        ValidadorDocumentoPdf.Resultado r = validador.validar(archivo, "pdf", "900.640.118-2",
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 12, 3), LocalDate.of(2026, 11, 9));

        assertTrue(r.automatico(), r.motivo());
    }

    @Test
    void pasaAAprobacionDualSiElNitNoCoincide() throws Exception {
        byte[] archivo = pdf("Certificado", "NIT: 901.111.222-3", "Expedido el 3 de noviembre de 2026");

        ValidadorDocumentoPdf.Resultado r = validador.validar(archivo, "pdf", "900.640.118-2",
                LocalDate.of(2026, 11, 3), null, LocalDate.of(2026, 11, 9));

        assertFalse(r.automatico());
        assertTrue(r.motivo().contains("No se encontró el NIT"));
    }

    @Test
    void leeFechasEscritasEnTexto() throws Exception {
        byte[] archivo = pdf("NIT 900640118", "Armenia, 3 de noviembre de 2026");

        ValidadorDocumentoPdf.Resultado r = validador.validar(archivo, "pdf", "900.640.118-2",
                LocalDate.of(2026, 11, 3), null, LocalDate.of(2026, 11, 9));

        assertTrue(r.automatico(), r.motivo());
    }

    @Test
    void unaImagenSiempreRequiereAprobacionDual() {
        ValidadorDocumentoPdf.Resultado r = validador.validar(new byte[]{1, 2, 3}, "jpg", "900.640.118-2",
                LocalDate.of(2026, 11, 3), null, LocalDate.of(2026, 11, 9));

        assertFalse(r.automatico());
    }

    @Test
    void documentoVencidoNoSeApruebaAutomaticamente() throws Exception {
        byte[] archivo = pdf("NIT: 900.640.118-2", "Fecha: 03/01/2025");

        ValidadorDocumentoPdf.Resultado r = validador.validar(archivo, "pdf", "900.640.118-2",
                LocalDate.of(2025, 1, 3), LocalDate.of(2025, 12, 31), LocalDate.of(2026, 11, 9));

        assertFalse(r.automatico());
        assertTrue(r.motivo().contains("vencido"));
    }
}
