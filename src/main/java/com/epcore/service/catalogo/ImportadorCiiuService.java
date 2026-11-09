package com.epcore.service.catalogo;

import com.epcore.entity.ActividadCiiu;
import com.epcore.entity.EstadoClasificacion;
import com.epcore.entity.ProveedorActividad;
import com.epcore.repository.ActividadCiiuRepository;
import com.epcore.repository.ProveedorActividadRepository;
import com.epcore.repository.ProveedorClasificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Importación controlada del catálogo CIIU oficial (DANE) desde un CSV
 * "codigo;descripcion" (también acepta coma). Compara con el catálogo actual:
 * agrega los nuevos, actualiza descripciones y DESACTIVA (no borra) los que
 * ya no aparecen; los proveedores que usan un código retirado pasan a revisión.
 */
@Service
@Transactional
public class ImportadorCiiuService {

    private static final Pattern CODIGO = Pattern.compile("\\d{4}");

    public record Resumen(List<String> nuevos, List<String> modificados, List<String> retirados,
                          int proveedoresAfectados, List<String> lineasIgnoradas) { }

    private final ActividadCiiuRepository ciiu;
    private final ProveedorActividadRepository actividades;
    private final ProveedorClasificacionRepository clasificaciones;

    public ImportadorCiiuService(ActividadCiiuRepository ciiu, ProveedorActividadRepository actividades,
                                 ProveedorClasificacionRepository clasificaciones) {
        this.ciiu = ciiu;
        this.actividades = actividades;
        this.clasificaciones = clasificaciones;
    }

    public Resumen importar(InputStream csv, String version) throws IOException {
        Lectura lectura = leer(csv);
        Map<String, String> oficial = lectura.filas();
        if (oficial.size() < 10) {
            throw new IllegalArgumentException("El archivo tiene muy pocos códigos válidos (" + oficial.size()
                    + "): verifique que sea el catálogo completo con el formato codigo;descripcion.");
        }
        String ver = version == null || version.isBlank() ? ActividadCiiu.VERSION_VIGENTE : version.trim();

        List<String> nuevos = new ArrayList<>(), modificados = new ArrayList<>(), retirados = new ArrayList<>();
        Map<String, ActividadCiiu> actuales = new HashMap<>();
        ciiu.findAll().forEach(a -> actuales.put(a.getCodigo(), a));

        oficial.forEach((codigo, descripcion) -> {
            ActividadCiiu a = actuales.get(codigo);
            if (a == null) {
                ciiu.save(ActividadCiiu.builder().codigo(codigo).descripcion(descripcion).version(ver).build());
                nuevos.add(codigo);
            } else if (!descripcion.equals(a.getDescripcion()) || !Boolean.TRUE.equals(a.getActivo())) {
                a.setDescripcion(descripcion);
                a.setActivo(true);
                a.setVersion(ver);
                modificados.add(codigo);
            }
        });

        actuales.values().stream()
                .filter(a -> Boolean.TRUE.equals(a.getActivo()) && !oficial.containsKey(a.getCodigo()))
                .forEach(a -> { a.setActivo(false); retirados.add(a.getCodigo()); });

        // Proveedores con actividades en códigos retirados: su clasificación pasa a revisión.
        Set<Long> afectados = new HashSet<>();
        if (!retirados.isEmpty()) {
            for (ProveedorActividad pa : actividades.findAll()) {
                if (retirados.contains(pa.getCodigoCiiu())) afectados.add(pa.getProveedor().getIdProveedor());
            }
            for (Long id : afectados) {
                clasificaciones.findByProveedorIdProveedor(id).ifPresent(c -> {
                    c.setEstado(EstadoClasificacion.EN_REVISION);
                    String motivo = "Actualización del catálogo CIIU: uno o más códigos del proveedor fueron retirados.";
                    c.setMotivosRevision(c.getMotivosRevision() == null ? motivo : c.getMotivosRevision() + " | " + motivo);
                });
            }
        }
        Collections.sort(nuevos); Collections.sort(modificados); Collections.sort(retirados);
        return new Resumen(nuevos, modificados, retirados, afectados.size(), lectura.ignoradas());
    }

    record Lectura(Map<String, String> filas, List<String> ignoradas) { }

    Lectura leer(InputStream csv) throws IOException {
        Map<String, String> filas = new LinkedHashMap<>();
        List<String> ignoradas = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String linea;
            int n = 0;
            while ((linea = r.readLine()) != null) {
                n++;
                String l = linea.replace("﻿", "").trim();
                if (l.isEmpty()) continue;
                String[] p = l.split("[;,\\t]", 2);
                String codigo = p[0].replace("\"", "").trim();
                if (!CODIGO.matcher(codigo).matches() || p.length < 2) {
                    if (n > 1) ignoradas.add("Línea " + n + ": " + (l.length() > 60 ? l.substring(0, 60) + "…" : l));
                    continue; // encabezado o fila de sección/división
                }
                String desc = p[1].replace("\"", "").trim();
                if (desc.length() > 300) desc = desc.substring(0, 300);
                if (!desc.isEmpty()) filas.put(codigo, desc);
            }
        }
        return new Lectura(filas, ignoradas.size() > 20 ? List.copyOf(ignoradas.subList(0, 20)) : List.copyOf(ignoradas));
    }
}
