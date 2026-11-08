package com.epcore.service.impl;

import com.epcore.entity.Permiso;
import com.epcore.entity.Rol;
import com.epcore.entity.RolSistema;
import com.epcore.repository.RolSistemaRepository;
import com.epcore.security.SeparacionFunciones;
import com.epcore.service.RolSistemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class RolSistemaServiceImpl implements RolSistemaService {

    private static final Pattern CODIGO = Pattern.compile("[A-Z][A-Z0-9_]{2,39}");

    private final RolSistemaRepository rolSistemaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RolSistema> listar() {
        return rolSistemaRepository.findAllByOrderByPredefinidoDescNombreAsc();
    }

    @Override
    public RolSistema crear(String codigo, String nombre, String descripcion,
                            Set<Permiso> permisos, BigDecimal limiteAprobacion, String usuario) {
        String cod = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);

        if (!CODIGO.matcher(cod).matches()) {
            throw new IllegalArgumentException(
                    "El código debe tener entre 3 y 40 caracteres: mayúsculas, números y guion bajo.");
        }
        if (esCodigoReservado(cod)) {
            throw new IllegalArgumentException("El código " + cod + " está reservado para un rol del sistema.");
        }
        if (rolSistemaRepository.existsByCodigo(cod)) {
            throw new IllegalArgumentException("Ya existe un rol con el código " + cod + ".");
        }

        RolSistema rol = RolSistema.builder().codigo(cod).predefinido(false).activo(true).build();
        aplicar(rol, nombre, descripcion, permisos, limiteAprobacion, usuario);
        return rolSistemaRepository.save(rol);
    }

    @Override
    public RolSistema actualizar(Long idRol, String nombre, String descripcion,
                                 Set<Permiso> permisos, BigDecimal limiteAprobacion, String usuario) {
        RolSistema rol = obtener(idRol);

        if (Rol.PROVEEDOR.name().equals(rol.getCodigo())
                && (permisos == null || !permisos.equals(EnumSet.of(Permiso.PORTAL_PROVEEDOR)))) {
            throw new IllegalArgumentException("El rol de proveedor solo puede tener acceso al portal.");
        }
        if (Rol.ADMIN.name().equals(rol.getCodigo())
                && (permisos == null || !permisos.contains(Permiso.ROLES_GESTIONAR)
                || !permisos.contains(Permiso.USUARIOS_GESTIONAR))) {
            throw new IllegalArgumentException(
                    "El Administrador debe conservar la gestión de usuarios y roles para no dejar el sistema sin control.");
        }

        aplicar(rol, nombre, descripcion, permisos, limiteAprobacion, usuario);
        return rolSistemaRepository.save(rol);
    }

    @Override
    public RolSistema desactivar(Long idRol, String usuario) {
        RolSistema rol = obtener(idRol);

        if (Boolean.TRUE.equals(rol.getPredefinido())) {
            throw new IllegalArgumentException("Los roles predefinidos no se pueden desactivar; ajuste sus permisos.");
        }

        rol.setActivo(false);
        rol.setFechaModificacion(LocalDateTime.now());
        rol.setModificadoPor(usuario);
        return rolSistemaRepository.save(rol);
    }

    private void aplicar(RolSistema rol, String nombre, String descripcion,
                         Set<Permiso> permisos, BigDecimal limiteAprobacion, String usuario) {
        if (nombre == null || nombre.trim().length() < 3) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio (mínimo 3 caracteres).");
        }
        if (limiteAprobacion != null && limiteAprobacion.signum() <= 0) {
            throw new IllegalArgumentException("El límite de aprobación debe ser mayor que cero o quedar vacío.");
        }

        List<String> conflictos = SeparacionFunciones.validarRol(permisos);
        if (!conflictos.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", conflictos));
        }

        rol.setNombre(nombre.trim());
        rol.setDescripcion(descripcion == null ? null : descripcion.trim());
        rol.setPermisos(EnumSet.copyOf(permisos));
        rol.setLimiteAprobacion(limiteAprobacion);
        rol.setFechaModificacion(LocalDateTime.now());
        rol.setModificadoPor(usuario);
    }

    private RolSistema obtener(Long idRol) {
        if (idRol == null || idRol <= 0) {
            throw new IllegalArgumentException("El identificador del rol no es válido.");
        }
        return rolSistemaRepository.findById(idRol)
                .orElseThrow(() -> new IllegalArgumentException("No existe un rol con el identificador " + idRol + "."));
    }

    private static boolean esCodigoReservado(String codigo) {
        for (Rol r : Rol.values()) {
            if (r.name().equals(codigo)) {
                return true;
            }
        }
        return false;
    }
}
