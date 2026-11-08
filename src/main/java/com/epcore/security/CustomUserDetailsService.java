package com.epcore.security;

import com.epcore.repository.RolSistemaRepository;

import com.epcore.entity.Usuario;
import com.epcore.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final LoginAttemptService loginAttemptService;
    private final RolSistemaRepository rolSistemaRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository,
                                    LoginAttemptService loginAttemptService,
                                    RolSistemaRepository rolSistemaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.loginAttemptService = loginAttemptService;
        this.rolSistemaRepository = rolSistemaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if (!usuario.getActivo()) {
            throw new UsernameNotFoundException("Usuario inactivo");
        }

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getPassword())
                .authorities(autoridades(usuario))
                .accountLocked(loginAttemptService.estaBloqueado(usuario.getCorreo()))
                .build();
    }

    /**
     * ROLE_<rol> (compatibilidad con las vistas) + un permiso por autoridad.
     * Los permisos salen del rol configurado en la base de datos; si aún no
     * existe, se usan los predeterminados. Un rol inactivo no otorga permisos.
     */
    private java.util.List<org.springframework.security.core.GrantedAuthority> autoridades(Usuario usuario) {
        java.util.Set<com.epcore.entity.Permiso> permisos;

        if (usuario.getRol() == com.epcore.entity.Rol.PERSONALIZADO) {
            com.epcore.entity.RolSistema perfil = usuario.getPerfil();
            permisos = perfil != null && Boolean.TRUE.equals(perfil.getActivo())
                    ? perfil.permisosComoEnumSet()
                    : java.util.EnumSet.noneOf(com.epcore.entity.Permiso.class);
        } else {
            permisos = rolSistemaRepository.findByCodigo(usuario.getRol().name())
                    .map(r -> Boolean.TRUE.equals(r.getActivo())
                            ? r.permisosComoEnumSet()
                            : java.util.EnumSet.noneOf(com.epcore.entity.Permiso.class))
                    .orElseGet(() -> PerfilesPredeterminados.permisosDe(usuario.getRol()));
        }

        java.util.List<org.springframework.security.core.GrantedAuthority> lista = new java.util.ArrayList<>();
        lista.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
        permisos.forEach(pm -> lista.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(pm.name())));
        return lista;
    }
}
