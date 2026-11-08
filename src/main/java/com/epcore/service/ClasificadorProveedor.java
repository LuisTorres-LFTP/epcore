package com.epcore.service;

import com.epcore.entity.ActividadCiiu;
import com.epcore.entity.EstadoClasificacion;
import com.epcore.entity.Nicho;
import com.epcore.entity.TipoActividad;
import com.epcore.entity.TipoProveedor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reglas de clasificación de proveedores. No depende de la base de datos:
 * recibe los datos y devuelve el resultado, por eso se prueba de forma aislada.
 *
 * Regla general: si los CIIU declarados existen, aparecen en el documento
 * oficial y algún nicho los cubre, la clasificación es AUTOMATICA.
 * Cualquier excepción la deja EN_REVISION con los motivos explicados.
 */
@Component
public class ClasificadorProveedor {

    private static final Pattern CODIGO_CIIU = Pattern.compile("\\d{4}");

    /** Actividad declarada por el proveedor. */
    public record ActividadDeclarada(String codigo, TipoActividad tipo) { }

    /** Nichos de un integrante de consorcio o unión temporal (null si no está clasificado). */
    public record Integrante(String nombre, BigDecimal porcentaje, Set<Nicho> nichos) { }

    public record Resultado(
            EstadoClasificacion estado,
            Set<Nicho> nichos,
            List<String> motivos,
            List<String> requisitosAdicionales
    ) { }

    /**
     * @param codigosOficiales códigos CIIU leídos del RUT o de la Cámara de Comercio;
     *                         null si todavía no se ha cargado el documento.
     * @param integrantes      solo para consorcios y uniones temporales.
     */
    public Resultado clasificar(
            TipoProveedor tipo,
            List<ActividadDeclarada> actividades,
            Set<String> codigosOficiales,
            Map<String, ActividadCiiu> catalogo,
            List<Nicho> nichosActivos,
            List<Integrante> integrantes
    ) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de proveedor es obligatorio.");
        }

        List<String> motivos = new ArrayList<>();
        List<String> requisitos = new ArrayList<>();
        Set<Nicho> nichos = new LinkedHashSet<>();

        if (tipo.isEsPluralidad()) {
            clasificarPluralidad(tipo, integrantes, nichos, motivos);
            return resultado(nichos, motivos, requisitos);
        }

        if (tipo == TipoProveedor.EXTRANJERA_SIN_SUCURSAL) {
            motivos.add("Empresa extranjera sin RUT colombiano: un analista debe clasificarla "
                    + "con el código internacional (ISIC) y sus documentos apostillados.");
            return resultado(nichos, motivos, requisitos);
        }

        List<ActividadDeclarada> lista = actividades == null ? List.of() : actividades;
        validarEstructuraRut(lista, motivos);

        boolean hayActividadEspecifica = false;

        for (ActividadDeclarada actividad : lista) {
            String codigo = actividad.codigo() == null ? "" : actividad.codigo().trim();

            if (!CODIGO_CIIU.matcher(codigo).matches()) {
                motivos.add("El código CIIU «" + codigo + "» no tiene 4 dígitos.");
                continue;
            }

            ActividadCiiu ciiu = catalogo.get(codigo);
            if (ciiu == null || !Boolean.TRUE.equals(ciiu.getActivo())) {
                motivos.add("El código CIIU " + codigo + " no existe en la "
                        + ActividadCiiu.VERSION_VIGENTE + " vigente.");
                continue;
            }

            verificarContraDocumento(tipo, codigo, codigosOficiales, motivos);

            if (Boolean.TRUE.equals(ciiu.getRequiereRevisionLegal())) {
                motivos.add("La actividad " + codigo + " (" + ciiu.getDescripcion()
                        + ") tiene restricciones legales: requiere revisión del área jurídica.");
            }

            if (ciiu.getRequisitoRegulatorio() != null && !ciiu.getRequisitoRegulatorio().isBlank()) {
                requisitos.add(ciiu.getRequisitoRegulatorio() + " (actividad " + codigo + ")");
            }

            if (!Boolean.TRUE.equals(ciiu.getGenerica())) {
                hayActividadEspecifica = true;
            }

            nichosActivos.stream()
                    .filter(n -> Boolean.TRUE.equals(n.getActivo()) && n.cubre(codigo))
                    .forEach(nichos::add);
        }

        if (!lista.isEmpty() && !hayActividadEspecifica && motivos.isEmpty()) {
            motivos.add("Todas las actividades declaradas son genéricas: el nicho se define "
                    + "con los códigos UNSPSC y la experiencia comprobada.");
        }

        if (nichos.isEmpty() && motivos.isEmpty()) {
            motivos.add("Ninguna regla de nicho cubre los códigos CIIU declarados.");
        }

        if (tipo.isTieneRut() && codigosOficiales == null) {
            motivos.add("Falta cargar el " + tipo.getDocumentoVerificacion()
                    + " para verificar la actividad declarada.");
        }

        return resultado(nichos, motivos, requisitos);
    }

    private void validarEstructuraRut(List<ActividadDeclarada> lista, List<String> motivos) {
        if (lista.isEmpty()) {
            motivos.add("No declaró ninguna actividad económica (CIIU).");
            return;
        }

        Map<TipoActividad, Integer> conteo = new EnumMap<>(TipoActividad.class);
        Set<String> vistos = new LinkedHashSet<>();

        for (ActividadDeclarada a : lista) {
            if (a.tipo() == null) {
                motivos.add("Cada actividad debe indicar si es principal, secundaria u otra.");
                return;
            }
            conteo.merge(a.tipo(), 1, Integer::sum);
            if (a.codigo() != null && !vistos.add(a.codigo().trim())) {
                motivos.add("El código CIIU " + a.codigo().trim() + " está repetido.");
            }
        }

        if (conteo.getOrDefault(TipoActividad.PRINCIPAL, 0) != 1) {
            motivos.add("Debe haber exactamente una actividad principal, como en el RUT.");
        }

        for (TipoActividad t : TipoActividad.values()) {
            if (conteo.getOrDefault(t, 0) > t.getMaximo()) {
                motivos.add("El RUT admite máximo " + t.getMaximo() + " actividad(es) de tipo " + t + ".");
            }
        }
    }

    private void verificarContraDocumento(
            TipoProveedor tipo,
            String codigo,
            Set<String> codigosOficiales,
            List<String> motivos
    ) {
        if (codigosOficiales == null) {
            return; // se reporta una sola vez al final si el documento es obligatorio
        }

        if (!codigosOficiales.contains(codigo)) {
            motivos.add("El CIIU " + codigo + " no aparece en el " + tipo.getDocumentoVerificacion()
                    + " (posible RUT o matrícula desactualizados).");
        }
    }

    private void clasificarPluralidad(
            TipoProveedor tipo,
            List<Integrante> integrantes,
            Set<Nicho> nichos,
            List<String> motivos
    ) {
        if (integrantes == null || integrantes.size() < 2) {
            motivos.add("Un " + tipo.getDescripcion().toLowerCase()
                    + " debe tener al menos dos integrantes registrados.");
            return;
        }

        BigDecimal suma = integrantes.stream()
                .map(i -> i.porcentaje() == null ? BigDecimal.ZERO : i.porcentaje())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (suma.compareTo(BigDecimal.valueOf(100)) != 0) {
            motivos.add("Los porcentajes de participación suman " + suma.stripTrailingZeros().toPlainString()
                    + " %; deben sumar 100 %.");
        }

        for (Integrante integrante : integrantes) {
            if (integrante.nichos() == null || integrante.nichos().isEmpty()) {
                motivos.add("El integrante " + integrante.nombre()
                        + " todavía no tiene una clasificación aprobada.");
            } else {
                nichos.addAll(integrante.nichos());
            }
        }
    }

    private Resultado resultado(Set<Nicho> nichos, List<String> motivos, List<String> requisitos) {
        EstadoClasificacion estado = motivos.isEmpty()
                ? EstadoClasificacion.AUTOMATICA
                : EstadoClasificacion.EN_REVISION;

        return new Resultado(estado, nichos, List.copyOf(motivos), List.copyOf(requisitos));
    }
}
