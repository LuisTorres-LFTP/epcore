package com.epcore.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee el texto del documento y verifica que el NIT y las fechas coincidan
 * con lo registrado. Si todo cumple, el documento puede aprobarse sin
 * intervención humana; si no, pasa a la aprobación dual (Director → Gerente).
 */
@Component
public class ValidadorDocumentoPdf {

    public record Resultado(boolean automatico, List<String> verificaciones, String motivo) { }

    private static final Map<String, Integer> MESES = Map.ofEntries(
            Map.entry("enero", 1), Map.entry("febrero", 2), Map.entry("marzo", 3), Map.entry("abril", 4),
            Map.entry("mayo", 5), Map.entry("junio", 6), Map.entry("julio", 7), Map.entry("agosto", 8),
            Map.entry("septiembre", 9), Map.entry("setiembre", 9), Map.entry("octubre", 10),
            Map.entry("noviembre", 11), Map.entry("diciembre", 12));

    private static final Pattern DMY = Pattern.compile("\\b(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{4})\\b");
    private static final Pattern YMD = Pattern.compile("\\b(\\d{4})[/.\\-](\\d{1,2})[/.\\-](\\d{1,2})\\b");
    private static final Pattern TEXTO = Pattern.compile("(\\d{1,2})\\s+de\\s+([a-záéíóú]+)\\s+(?:de|del)\\s+(\\d{4})");

    public Resultado validar(byte[] contenido, String extension, String nitProveedor,
                             LocalDate fechaExpedicion, LocalDate fechaVencimiento, LocalDate hoy) {
        List<String> ok = new ArrayList<>();
        List<String> fallas = new ArrayList<>();

        if (!"pdf".equalsIgnoreCase(extension)) {
            return new Resultado(false, ok,
                    "El archivo no es PDF (imagen u ofimática): el sistema no puede leerlo y requiere aprobación dual.");
        }

        String texto = extraerTexto(contenido);
        if (texto.replaceAll("\\s", "").length() < 20) {
            return new Resultado(false, ok,
                    "El PDF no tiene texto legible (parece escaneado): requiere aprobación dual.");
        }
        ok.add("Texto del documento legible");

        String nit = nitProveedor == null ? "" : nitProveedor.replaceAll("\\D", "");
        String nitBase = nit.length() > 9 ? nit.substring(0, 9) : nit;
        String digitos = texto.replaceAll("[.\\s\\-]", "");
        if (!nitBase.isEmpty() && digitos.contains(nitBase)) {
            ok.add("NIT " + nitProveedor + " encontrado en el documento");
        } else {
            fallas.add("No se encontró el NIT " + nitProveedor + " en el documento");
        }

        Set<LocalDate> fechas = extraerFechas(texto);
        if (fechaExpedicion == null) {
            fallas.add("No se registró la fecha de expedición");
        } else if (fechas.contains(fechaExpedicion)) {
            ok.add("Fecha de expedición " + fechaExpedicion + " encontrada en el documento");
        } else {
            fallas.add("La fecha de expedición " + fechaExpedicion + " no aparece en el documento"
                    + (fechas.isEmpty() ? " (no se leyeron fechas)" : " (fechas leídas: " + fechas + ")"));
        }

        if (fechaExpedicion != null && fechaExpedicion.isAfter(hoy)) {
            fallas.add("La fecha de expedición es futura");
        }
        if (fechaVencimiento != null && fechaVencimiento.isBefore(hoy)) {
            fallas.add("El documento está vencido");
        } else {
            ok.add("Documento vigente");
        }

        return fallas.isEmpty()
                ? new Resultado(true, ok, "")
                : new Resultado(false, ok, String.join("; ", fallas) + ". Requiere aprobación dual.");
    }

    String extraerTexto(byte[] contenido) {
        try (PDDocument pdf = Loader.loadPDF(contenido)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(Math.min(5, pdf.getNumberOfPages()));
            return stripper.getText(pdf);
        } catch (Exception e) {
            return "";
        }
    }

    static Set<LocalDate> extraerFechas(String texto) {
        Set<LocalDate> fechas = new TreeSet<>();
        String t = texto.toLowerCase(Locale.ROOT);
        Matcher m = DMY.matcher(t);
        while (m.find()) agregar(fechas, m.group(3), m.group(2), m.group(1));
        m = YMD.matcher(t);
        while (m.find()) agregar(fechas, m.group(1), m.group(2), m.group(3));
        m = TEXTO.matcher(t);
        while (m.find()) {
            Integer mes = MESES.get(m.group(2));
            if (mes != null) agregar(fechas, m.group(3), String.valueOf(mes), m.group(1));
        }
        return fechas;
    }

    private static void agregar(Set<LocalDate> fechas, String anio, String mes, String dia) {
        try {
            int y = Integer.parseInt(anio);
            if (y > 1990 && y < 2100) {
                fechas.add(LocalDate.of(y, Integer.parseInt(mes), Integer.parseInt(dia)));
            }
        } catch (RuntimeException ignored) {
            // fecha imposible, p. ej. 31/02
        }
    }
}
