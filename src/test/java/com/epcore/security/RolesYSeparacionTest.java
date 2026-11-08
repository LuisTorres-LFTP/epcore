package com.epcore.security;

import com.epcore.entity.Permiso;
import com.epcore.entity.Rol;
import com.epcore.entity.RolSistema;
import com.epcore.repository.RolSistemaRepository;
import com.epcore.service.impl.RolSistemaServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static com.epcore.entity.Permiso.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RolesYSeparacionTest {

    private final RolSistemaRepository repo = mock(RolSistemaRepository.class);
    private final RolSistemaServiceImpl servicio = new RolSistemaServiceImpl(repo);

    @Test
    void losRolesPredeterminadosRespetanLaSeparacionDeFunciones() {
        PerfilesPredeterminados.todos().forEach((rol, perfil) ->
                assertTrue(SeparacionFunciones.validarRol(perfil.permisos()).isEmpty(),
                        "El rol " + rol + " viola la separación de funciones"));
    }

    @Test
    void noSePuedeCrearUnRolQueApruebeEnLosDosNiveles() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> servicio.crear(
                "SUPER_APROBADOR", "Súper aprobador", null,
                EnumSet.of(DOCUMENTOS_APROBAR_NIVEL_1, DOCUMENTOS_APROBAR_NIVEL_2), null, "admin"));
        assertTrue(ex.getMessage().contains("dos personas distintas"));
    }

    @Test
    void quienCreaProcesosNoPuedeAdjudicar() {
        assertFalse(SeparacionFunciones.validarRol(EnumSet.of(PROCESOS_CREAR, PROCESOS_ADJUDICAR)).isEmpty());
    }

    @Test
    void unRolDeProveedorNoPuedeTenerPermisosInternos() {
        assertFalse(SeparacionFunciones.validarRol(EnumSet.of(PORTAL_PROVEEDOR, PROVEEDORES_VER)).isEmpty());
    }

    @Test
    void creaUnRolPersonalizadoValidoConLimiteDeAprobacion() {
        when(repo.existsByCodigo("JEFE_COMPRAS_REGIONAL")).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        RolSistema rol = servicio.crear("jefe_compras_regional", "Jefe de compras regional", "Sede Eje Cafetero",
                EnumSet.of(PROVEEDORES_VER, DOCUMENTOS_APROBAR_NIVEL_1), BigDecimal.valueOf(200_000_000L),
                "admin@epcore.co");

        assertEquals("JEFE_COMPRAS_REGIONAL", rol.getCodigo());
        assertFalse(rol.getPredefinido());
        assertTrue(rol.puedeAprobarMonto(BigDecimal.valueOf(150_000_000L)));
        assertFalse(rol.puedeAprobarMonto(BigDecimal.valueOf(250_000_000L)));
        assertEquals("admin@epcore.co", rol.getModificadoPor());
    }

    @Test
    void noSePuedeUsarElCodigoDeUnRolDelSistema() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(
                "GERENTE", "Gerente falso", null, EnumSet.of(PROVEEDORES_VER), null, "admin"));
    }

    @Test
    void elAdministradorNoPuedeQuitarseLaGestionDeRoles() {
        RolSistema admin = RolSistema.builder().idRol(1L).codigo(Rol.ADMIN.name()).predefinido(true).activo(true)
                .permisos(PerfilesPredeterminados.permisosDe(Rol.ADMIN)).build();
        when(repo.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class, () -> servicio.actualizar(
                1L, "Administrador", null, EnumSet.of(PROVEEDORES_VER), null, "admin"));
    }

    @Test
    void losPredefinidosNoSeDesactivan() {
        RolSistema gerente = RolSistema.builder().idRol(2L).codigo("GERENTE").predefinido(true).activo(true)
                .permisos(Set.of(PROVEEDORES_VER)).build();
        when(repo.findById(2L)).thenReturn(Optional.of(gerente));

        assertThrows(IllegalArgumentException.class, () -> servicio.desactivar(2L, "admin"));
    }

    @Test
    void reglasPorAccionImpidenAutoaprobacion() {
        assertThrows(IllegalStateException.class,
                () -> SeparacionFunciones.validarAprobacion("ana@epcore.co", "ANA@epcore.co", null));
        assertThrows(IllegalStateException.class,
                () -> SeparacionFunciones.validarAprobacion("jorge@epcore.co", "ana@epcore.co", "jorge@epcore.co"));
        assertDoesNotThrow(
                () -> SeparacionFunciones.validarAprobacion("laura@epcore.co", "ana@epcore.co", "jorge@epcore.co"));
    }

    @Test
    void cadaPermisoTieneGrupoYDescripcion() {
        for (Permiso p : Permiso.values()) {
            assertFalse(p.getGrupo().isBlank());
            assertFalse(p.getDescripcion().isBlank());
        }
    }
}
