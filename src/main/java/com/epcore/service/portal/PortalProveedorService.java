package com.epcore.service.portal;

import com.epcore.entity.Proveedor;
import com.epcore.entity.Rol;
import com.epcore.entity.Usuario;
import com.epcore.entity.proceso.Oferta;
import com.epcore.entity.proceso.Proceso;
import com.epcore.entity.proceso.ProcesoEnums.EstadoProceso;
import com.epcore.entity.proceso.ProcesoEnums.Visibilidad;
import com.epcore.repository.ProveedorRepository;
import com.epcore.repository.UsuarioRepository;
import com.epcore.repository.proceso.OfertaRepository;
import com.epcore.repository.proceso.ProcesoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reglas del portal de proveedores. El proveedor siempre se obtiene del usuario
 * autenticado; nunca de un parámetro de la petición.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PortalProveedorService {

    private static final Pattern NIT = Pattern.compile("^(\\d{3})\\.?(\\d{3})\\.?(\\d{3})-(\\d)$");
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<EstadoProceso> VISIBLES =
            EnumSet.of(EstadoProceso.PUBLICADO, EstadoProceso.EN_EVALUACION, EstadoProceso.ADJUDICADO, EstadoProceso.DESIERTO);

    private final UsuarioRepository usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProcesoRepository procesoRepository;
    private final OfertaRepository ofertaRepository;
    private final PasswordEncoder passwordEncoder;

    public record Registro(String razonSocial, String nit, String ciudad, String representante, String telefono,
                           String correo, String clave, String claveConfirmacion, boolean autorizacionDatos) { }

    /** Proveedor del usuario autenticado. */
    @Transactional(readOnly = true)
    public Proveedor proveedorDe(String correoUsuario) {
        Usuario u = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado."));
        if (u.getRol() != Rol.PROVEEDOR || u.getProveedor() == null) {
            throw new IllegalStateException("El usuario no está vinculado a un proveedor.");
        }
        return u.getProveedor();
    }

    /** Dígito de verificación del NIT según el algoritmo de la DIAN. */
    public static int digitoVerificacion(String nueveDigitos) {
        int[] pesos = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};
        int suma = 0;
        for (int i = 0; i < nueveDigitos.length(); i++) {
            suma += Character.getNumericValue(nueveDigitos.charAt(nueveDigitos.length() - 1 - i)) * pesos[i];
        }
        int r = suma % 11;
        return r < 2 ? r : 11 - r;
    }

    /** Registro público de un proveedor: crea la empresa y su primer usuario del portal. */
    public Usuario registrar(Registro r) {
        List<String> errores = new ArrayList<>();
        if (r.razonSocial() == null || r.razonSocial().trim().length() < 3) errores.add("Escriba la razón social.");
        Matcher m = r.nit() == null ? null : NIT.matcher(r.nit().trim());
        String nitNormalizado = null;
        if (m == null || !m.matches()) {
            errores.add("NIT con formato 901.234.567-7.");
        } else {
            String base = m.group(1) + m.group(2) + m.group(3);
            if (digitoVerificacion(base) != Integer.parseInt(m.group(4))) {
                errores.add("El dígito de verificación no corresponde al NIT.");
            } else {
                nitNormalizado = m.group(1) + "." + m.group(2) + "." + m.group(3) + "-" + m.group(4);
                if (proveedorRepository.findByNit(nitNormalizado).isPresent())
                    errores.add("Ese NIT ya está registrado. Si es su empresa, contacte al comprador.");
            }
        }
        if (r.ciudad() == null || r.ciudad().isBlank()) errores.add("La ciudad es obligatoria.");
        if (r.representante() == null || r.representante().isBlank()) errores.add("El representante legal es obligatorio.");
        if (r.correo() == null || !CORREO.matcher(r.correo().trim()).matches()) errores.add("Correo no válido.");
        else if (usuarioRepository.findByCorreo(r.correo().trim().toLowerCase(Locale.ROOT)).isPresent())
            errores.add("Ese correo ya tiene una cuenta.");
        String clave = r.clave() == null ? "" : r.clave();
        if (clave.length() < 8 || !clave.matches(".*[A-Za-z].*") || !clave.matches(".*\\d.*"))
            errores.add("La contraseña debe tener mínimo 8 caracteres, con letras y números.");
        else if (!clave.equals(r.claveConfirmacion())) errores.add("Las contraseñas no coinciden.");
        if (!r.autorizacionDatos()) errores.add("Debe autorizar el tratamiento de datos (Ley 1581 de 2012).");
        if (!errores.isEmpty()) throw new IllegalArgumentException(String.join(" ", errores));

        Proveedor p = proveedorRepository.save(Proveedor.builder()
                .tipoPersona("JURIDICA").nit(nitNormalizado).razonSocial(r.razonSocial().trim())
                .representanteLegal(r.representante().trim()).correo(r.correo().trim().toLowerCase(Locale.ROOT))
                .telefono(r.telefono() == null ? null : r.telefono().trim()).ciudad(r.ciudad().trim()).pais("Colombia")
                .autorizacionDatos(true).estado("PENDIENTE_VERIFICACION").build());

        Usuario u = new Usuario();
        u.setNombre(r.representante().trim());
        u.setCorreo(r.correo().trim().toLowerCase(Locale.ROOT));
        u.setPassword(passwordEncoder.encode(clave));
        u.setRol(Rol.PROVEEDOR);
        u.setActivo(true);
        u.setProveedor(p);
        return usuarioRepository.save(u);
    }

    /** Un proveedor solo ve procesos publicados que son abiertos, públicos o a los que fue invitado. */
    public boolean puedeVer(Proceso pr, Long idProveedor) {
        if (!VISIBLES.contains(pr.getEstado())) return false;
        if (pr.getVisibilidad() == Visibilidad.ABIERTO || pr.getVisibilidad() == Visibilidad.SECOP) return true;
        return pr.getInvitados().stream().anyMatch(p -> p.getIdProveedor().equals(idProveedor));
    }

    @Transactional(readOnly = true)
    public List<Proceso> procesosVisibles(Long idProveedor) {
        return procesoRepository.findAllByOrderByFechaCreacionDesc().stream()
                .filter(pr -> puedeVer(pr, idProveedor))
                .sorted(Comparator.comparing(Proceso::getFechaCierre))
                .toList();
    }

    @Transactional(readOnly = true)
    public Proceso procesoVisible(Long idProceso, Long idProveedor) {
        Proceso pr = procesoRepository.findById(idProceso).orElse(null);
        if (pr == null || !puedeVer(pr, idProveedor)) {
            // Misma respuesta si no existe o si no tiene acceso: no se revela la existencia del proceso.
            throw new IllegalArgumentException("Proceso no disponible.");
        }
        return pr;
    }

    /** Solo la oferta propia; nunca las de otros proponentes. */
    @Transactional(readOnly = true)
    public Optional<Oferta> miOferta(Long idProceso, Long idProveedor) {
        return ofertaRepository.findByProcesoIdProcesoAndProveedorIdProveedor(idProceso, idProveedor);
    }

    /** Resultado visible para el proveedor: solo el suyo, sin revelar al ganador. */
    public String miResultado(Proceso pr, Long idProveedor, boolean presentoOferta) {
        if (!presentoOferta) return null;
        return switch (pr.getEstado()) {
            case EN_EVALUACION -> "En evaluación";
            case ADJUDICADO -> pr.getAdjudicadoA() != null && pr.getAdjudicadoA().getIdProveedor().equals(idProveedor)
                    ? "Adjudicado a su empresa" : "No adjudicado";
            case DESIERTO -> "Proceso declarado desierto";
            default -> null;
        };
    }
}
