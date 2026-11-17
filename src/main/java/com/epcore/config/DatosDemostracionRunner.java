package com.epcore.config;

import com.epcore.entity.Rol;
import com.epcore.entity.Usuario;
import com.epcore.entity.proceso.Proceso;
import com.epcore.entity.proceso.ProcesoEnums.CanalOferta;
import com.epcore.entity.proceso.ProcesoEnums.Modalidad;
import com.epcore.entity.proceso.ProcesoEnums.Visibilidad;
import com.epcore.repository.UsuarioRepository;
import com.epcore.service.portal.PortalProveedorService;
import com.epcore.service.proceso.ProcesoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Modo demostración: crea un usuario por rol, dos proveedores del portal y tres
 * procesos de contratación en distintas etapas, para poder mostrar el flujo
 * completo de punta a punta.
 *
 * Solo se ejecuta con DEMO_HABILITADO=true y nunca en una instalación real.
 * La clave de todos los usuarios de demostración se toma de DEMO_CLAVE (no está
 * en el código). Si los datos ya existen, no hace nada.
 *
 * Los datos se crean con los mismos servicios que usa la aplicación, de modo que
 * pasan por las mismas validaciones, aprobaciones y reglas de separación de funciones.
 */
@Component
@Order(20)
@ConditionalOnProperty(name = "epcore.demo.habilitado", havingValue = "true")
public class DatosDemostracionRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemostracionRunner.class);

    static final String DOMINIO = "@demo.epcore.test";
    static final String ANALISTA = "analista" + DOMINIO;
    static final String DIRECTOR = "director" + DOMINIO;
    static final String GERENTE = "gerente" + DOMINIO;
    static final String PROVEEDOR_1 = "proveedor1" + DOMINIO;
    static final String PROVEEDOR_2 = "proveedor2" + DOMINIO;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PortalProveedorService portalProveedorService;
    private final ProcesoService procesoService;
    private final Clock reloj;
    private final String clave;

    public DatosDemostracionRunner(UsuarioRepository usuarioRepository,
                                   PasswordEncoder passwordEncoder,
                                   PortalProveedorService portalProveedorService,
                                   ProcesoService procesoService,
                                   Clock reloj,
                                   @Value("${epcore.demo.clave:}") String clave) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.portalProveedorService = portalProveedorService;
        this.procesoService = procesoService;
        this.reloj = reloj;
        this.clave = clave;
    }

    @Override
    public void run(String... args) {
        if (clave == null || clave.length() < 8 || !clave.matches(".*[A-Za-z].*") || !clave.matches(".*\\d.*")) {
            log.warn("Modo demostración activo, pero DEMO_CLAVE no está definida o no tiene mínimo 8 caracteres "
                    + "con letras y números. No se crearon datos de demostración.");
            return;
        }
        if (usuarioRepository.findByCorreo(ANALISTA).isPresent()) {
            log.info("Los datos de demostración ya existen; no se crean de nuevo.");
            return;
        }

        crearUsuario("Andrea Gómez (demo)", ANALISTA, Rol.ANALISTA_COMPRAS);
        crearUsuario("Diego Ramírez (demo)", DIRECTOR, Rol.DIRECTOR_COMPRAS);
        crearUsuario("Gloria Pérez (demo)", GERENTE, Rol.GERENTE);
        crearUsuario("Óscar Castro (demo)", "cumplimiento" + DOMINIO, Rol.OFICIAL_CUMPLIMIENTO);
        crearUsuario("Elena Rojas (demo)", "evaluador" + DOMINIO, Rol.EVALUADOR_TECNICO);
        crearUsuario("Mario Díaz (demo)", "auditor" + DOMINIO, Rol.AUDITOR);
        crearUsuario("Administrador (demo)", "admin" + DOMINIO, Rol.ADMIN);

        Long proveedor1 = registrarProveedor("Tecnología Andina Demo S.A.S.", "900123456", "Bogotá",
                "Laura Restrepo", PROVEEDOR_1);
        Long proveedor2 = registrarProveedor("Servicios Integrales Demo S.A.S.", "900654321", "Medellín",
                "Carlos Ospina", PROVEEDOR_2);

        LocalDateTime cierre = LocalDateTime.now(reloj).plusDays(15).withHour(17).withMinute(0).withSecond(0).withNano(0);
        Set<Long> invitados = Set.of(proveedor1, proveedor2);

        // 1. Borrador: el analista lo está estructurando.
        procesoService.crearBorrador("DEMO-2026-001", "Mantenimiento preventivo de equipos de cómputo",
                "EpCore Demo S.A.S.", Modalidad.PRIVADA, Visibilidad.INVITACION,
                new BigDecimal("45000000"), cierre, invitados, ANALISTA);

        // 2. En aprobación: enviado al Director de compras (1/2).
        Proceso enAprobacion = procesoService.crearBorrador("DEMO-2026-002", "Suministro de papelería institucional",
                "EpCore Demo S.A.S.", Modalidad.PRIVADA, Visibilidad.INVITACION,
                new BigDecimal("18000000"), cierre, invitados, ANALISTA);
        procesoService.enviarAAprobacion(enAprobacion.getIdProceso(), ANALISTA);

        // 3. Publicado con aprobación dual y una oferta recibida desde el portal.
        Proceso publicado = procesoService.crearBorrador("DEMO-2026-003", "Servicio de soporte de mesa de ayuda",
                "EpCore Demo S.A.S.", Modalidad.PRIVADA, Visibilidad.INVITACION,
                new BigDecimal("120000000"), cierre, invitados, ANALISTA);
        procesoService.enviarAAprobacion(publicado.getIdProceso(), ANALISTA);
        procesoService.aprobarNivel1(publicado.getIdProceso(), DIRECTOR);
        procesoService.aprobarNivel2YPublicar(publicado.getIdProceso(), GERENTE);
        procesoService.registrarOferta(publicado.getIdProceso(), proveedor1, new BigDecimal("98500000"), 30,
                true, CanalOferta.PORTAL, true, PROVEEDOR_1);

        log.info("Datos de demostración creados: 7 usuarios internos, 2 proveedores del portal y 3 procesos. "
                + "Los usuarios terminan en {} y usan la clave definida en DEMO_CLAVE.", DOMINIO);
    }

    private void crearUsuario(String nombre, String correo, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode(clave));
        u.setRol(rol);
        u.setActivo(true);
        usuarioRepository.save(u);
    }

    private Long registrarProveedor(String razonSocial, String nueveDigitos, String ciudad,
                                    String representante, String correo) {
        String nit = nueveDigitos.substring(0, 3) + "." + nueveDigitos.substring(3, 6) + "."
                + nueveDigitos.substring(6) + "-" + PortalProveedorService.digitoVerificacion(nueveDigitos);
        Usuario usuario = portalProveedorService.registrar(new PortalProveedorService.Registro(
                razonSocial, nit, ciudad, representante, "6015550000", correo, clave, clave, true));
        return usuario.getProveedor().getIdProveedor();
    }
}
