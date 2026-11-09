package com.epcore.service.catalogo;

import com.epcore.entity.*;
import com.epcore.multicliente.ClientesConfigurados;
import com.epcore.repository.ConsultaSarlaftRepository;
import com.epcore.repository.EntradaListaRestrictivaRepository;
import com.epcore.repository.ProveedorRepository;
import com.epcore.service.notificacion.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listas restrictivas OFAC (SDN) y ONU (lista consolidada): carga manual del
 * archivo oficial o descarga automática diaria (desactivada por defecto), y
 * cruce con los proveedores. Una coincidencia exacta del nombre normalizado
 * genera una POSIBLE_COINCIDENCIA para revisión del oficial de cumplimiento;
 * el sistema nunca bloquea ni descarta por sí solo.
 */
@Service
public class ListasRestrictivasService {

    private static final Logger log = LoggerFactory.getLogger(ListasRestrictivasService.class);
    public static final String USUARIO_SISTEMA = "sistema (cruce automático de listas)";
    private static final Set<String> SUFIJOS = Set.of("SAS", "SA", "LTDA", "LIMITADA", "SOCIEDAD", "POR", "ACCIONES",
            "SIMPLIFICADA", "ANONIMA", "CIA", "Y", "EU", "EIRL", "INC", "LLC", "LTD", "CORP", "CO");

    private final EntradaListaRestrictivaRepository entradas;
    private final ProveedorRepository proveedores;
    private final ConsultaSarlaftRepository consultas;
    private final NotificacionService notificaciones;
    private final ClientesConfigurados clientes;
    private final Clock reloj;
    private final boolean automatico;
    private final String urlOfac;
    private final String urlOnu;

    public ListasRestrictivasService(EntradaListaRestrictivaRepository entradas, ProveedorRepository proveedores,
                                     ConsultaSarlaftRepository consultas, NotificacionService notificaciones,
                                     ClientesConfigurados clientes, Clock reloj,
                                     @Value("${epcore.listas.actualizacion-automatica:false}") boolean automatico,
                                     @Value("${epcore.listas.url-ofac:https://www.treasury.gov/ofac/downloads/sdn.csv}") String urlOfac,
                                     @Value("${epcore.listas.url-onu:https://scsanctions.un.org/resources/xml/en/consolidated.xml}") String urlOnu) {
        this.entradas = entradas;
        this.proveedores = proveedores;
        this.consultas = consultas;
        this.notificaciones = notificaciones;
        this.clientes = clientes;
        this.reloj = reloj;
        this.automatico = automatico;
        this.urlOfac = urlOfac;
        this.urlOnu = urlOnu;
    }

    public record Resultado(ListaRestrictiva lista, int nombres, int coincidencias) { }

    /** Nombre en mayúsculas, sin tildes, sin signos y sin sufijos societarios. */
    public static String normalizar(String nombre) {
        if (nombre == null) return "";
        String s = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT).replace(".", "").replaceAll("[^A-Z0-9 ]", " ");
        List<String> partes = new ArrayList<>();
        for (String t : s.trim().split("\\s+")) if (!t.isEmpty() && !SUFIJOS.contains(t)) partes.add(t);
        return String.join(" ", partes);
    }

    /** OFAC SDN CSV: el nombre es la segunda columna entre comillas. */
    public static List<String> parsearOfac(InputStream in) throws IOException {
        List<String> nombres = new ArrayList<>();
        Matcher m = Pattern.compile("^\\d+,\"([^\"]+)\"", Pattern.MULTILINE)
                .matcher(new String(in.readAllBytes(), StandardCharsets.ISO_8859_1));
        while (m.find()) nombres.add(m.group(1).replace(",", " "));
        return nombres;
    }

    /** ONU XML consolidado: nombres de personas (FIRST..FOURTH_NAME) y de entidades (FIRST_NAME). */
    public static List<String> parsearOnu(InputStream in) throws IOException {
        String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        List<String> nombres = new ArrayList<>();
        Matcher bloque = Pattern.compile("<(INDIVIDUAL|ENTITY)>(.*?)</\\1>", Pattern.DOTALL).matcher(xml);
        while (bloque.find()) {
            StringBuilder n = new StringBuilder();
            for (String tag : List.of("FIRST_NAME", "SECOND_NAME", "THIRD_NAME", "FOURTH_NAME")) {
                Matcher t = Pattern.compile("<" + tag + ">([^<]*)</" + tag + ">").matcher(bloque.group(2));
                if (t.find() && !t.group(1).isBlank()) n.append(t.group(1).trim()).append(' ');
            }
            if (!n.isEmpty()) nombres.add(n.toString().trim());
        }
        return nombres;
    }

    /** Reemplaza la lista y cruza con los proveedores de la base actual. */
    @Transactional
    public Resultado cargarYCruzar(ListaRestrictiva lista, List<String> nombres) {
        if (lista != ListaRestrictiva.OFAC && lista != ListaRestrictiva.ONU) {
            throw new IllegalArgumentException("Solo se cargan automáticamente las listas OFAC y ONU.");
        }
        if (nombres.size() < 10) {
            throw new IllegalArgumentException("El archivo tiene muy pocos nombres (" + nombres.size() + "): verifique que sea la lista oficial.");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        entradas.borrarLista(lista);
        Set<String> vistos = new HashSet<>();
        List<EntradaListaRestrictiva> lote = new ArrayList<>();
        for (String n : nombres) {
            String norm = normalizar(n);
            if (norm.length() >= 4 && vistos.add(norm)) {
                lote.add(EntradaListaRestrictiva.builder().lista(lista).nombre(n.length() > 300 ? n.substring(0, 300) : n)
                        .nombreNormalizado(norm.length() > 300 ? norm.substring(0, 300) : norm).fechaCarga(ahora).build());
            }
        }
        entradas.saveAll(lote);
        return new Resultado(lista, lote.size(), cruzar(lista));
    }

    /** Coincidencia exacta del nombre normalizado de la empresa o del representante legal. */
    int cruzar(ListaRestrictiva lista) {
        int coincidencias = 0;
        for (Proveedor p : proveedores.findAll()) {
            List<String> candidatos = new ArrayList<>();
            for (String n : new String[]{p.getRazonSocial(), p.getRepresentanteLegal()}) {
                String norm = normalizar(n);
                if (norm.length() >= 4) candidatos.add(norm);
            }
            if (candidatos.isEmpty()) continue;
            List<EntradaListaRestrictiva> hits = entradas.findByNombreNormalizadoIn(candidatos).stream()
                    .filter(e -> e.getLista() == lista).toList();
            if (hits.isEmpty()) continue;

            boolean yaAlertado = consultas.findFirstByProveedorIdProveedorAndListaOrderByFechaConsultaDesc(p.getIdProveedor(), lista)
                    .map(c -> c.getResultado() != ResultadoSarlaft.SIN_COINCIDENCIA && USUARIO_SISTEMA.equals(c.getUsuarioResponsable()))
                    .orElse(false);
            if (yaAlertado) continue;

            consultas.save(ConsultaSarlaft.builder().proveedor(p).lista(lista)
                    .documentoConsultado(p.getNit() == null ? "SIN NIT" : p.getNit())
                    .nombreConsultado(p.getRazonSocial()).resultado(ResultadoSarlaft.POSIBLE_COINCIDENCIA)
                    .observacion("Coincidencia automática del nombre con la lista " + lista + ": «" + hits.get(0).getNombre()
                            + "». Requiere verificación del oficial de cumplimiento (puede ser homónimo).")
                    .usuarioResponsable(USUARIO_SISTEMA).fechaConsulta(LocalDateTime.now(reloj)).build());
            notificaciones.notificarRol("lista-" + lista + "-prov-" + p.getIdProveedor() + "-" + LocalDateTime.now(reloj).toLocalDate(),
                    Rol.OFICIAL_CUMPLIMIENTO, "Posible coincidencia en lista " + lista,
                    p.getRazonSocial() + " coincide con un nombre de la lista " + lista + ". Verifique si es homónimo.",
                    "/proveedores/" + p.getIdProveedor() + "/expediente", true);
            coincidencias++;
        }
        return coincidencias;
    }

    /** Descarga diaria de las listas oficiales (solo si epcore.listas.actualizacion-automatica=true). */
    @Scheduled(cron = "${epcore.listas.cron:0 30 3 * * *}", zone = "America/Bogota")
    public void actualizacionDiaria() {
        if (!automatico) return;
        try {
            List<String> ofac = parsearOfac(descargar(urlOfac));
            List<String> onu = parsearOnu(descargar(urlOnu));
            clientes.paraCadaCliente(id -> {
                Resultado a = cargarYCruzar(ListaRestrictiva.OFAC, ofac);
                Resultado b = cargarYCruzar(ListaRestrictiva.ONU, onu);
                log.info("Listas actualizadas en {}: OFAC {} nombres ({} alertas), ONU {} nombres ({} alertas)",
                        id, a.nombres(), a.coincidencias(), b.nombres(), b.coincidencias());
            });
        } catch (Exception e) {
            log.error("No se pudieron actualizar las listas restrictivas: {}", e.getMessage());
        }
    }

    private static InputStream descargar(String url) throws IOException, InterruptedException {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<InputStream> r = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(5)).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
        if (r.statusCode() != 200) throw new IOException("HTTP " + r.statusCode() + " descargando " + url);
        return r.body();
    }

    @Transactional(readOnly = true)
    public Map<ListaRestrictiva, Long> conteos() {
        Map<ListaRestrictiva, Long> m = new LinkedHashMap<>();
        m.put(ListaRestrictiva.OFAC, entradas.countByLista(ListaRestrictiva.OFAC));
        m.put(ListaRestrictiva.ONU, entradas.countByLista(ListaRestrictiva.ONU));
        return m;
    }
}
