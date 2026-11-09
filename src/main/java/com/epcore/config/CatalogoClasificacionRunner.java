package com.epcore.config;

import com.epcore.entity.ActividadCiiu;
import com.epcore.entity.Nicho;
import com.epcore.repository.ActividadCiiuRepository;
import com.epcore.repository.NichoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Carga un catálogo inicial de ejemplo SOLO si las tablas están vacías.
 * Incluye los casos especiales (actividades genéricas, reguladas y con
 * restricción legal). El catálogo completo de la CIIU Rev. 4 A.C. del DANE
 * debe cargarse aparte; los nichos los ajusta cada cliente.
 */
@Component
@RequiredArgsConstructor
public class CatalogoClasificacionRunner implements CommandLineRunner {

    private final ActividadCiiuRepository actividadCiiuRepository;
    private final NichoRepository nichoRepository;
    private final com.epcore.multicliente.ClientesConfigurados clientes;

    @Override
    public void run(String... args) {
        // En la base de cada cliente (y en la principal)
        clientes.paraCadaCliente(id -> cargarSiVacio());
    }

    private void cargarSiVacio() {
        if (actividadCiiuRepository.count() == 0) {
            actividadCiiuRepository.saveAll(List.of(
                    ciiu("8121", "Limpieza general interior de edificios"),
                    ciiu("8129", "Otras actividades de limpieza de edificios e instalaciones industriales"),
                    ciiu("8130", "Actividades de paisajismo y servicios de mantenimiento conexos"),
                    ciiu("6201", "Actividades de desarrollo de sistemas informáticos"),
                    ciiu("6202", "Actividades de consultoría informática y administración de instalaciones informáticas"),
                    ciiu("6209", "Otras actividades de tecnologías de información y servicios informáticos"),
                    ciiu("6311", "Procesamiento de datos, alojamiento (hosting) y actividades relacionadas"),
                    regulada("4923", "Transporte de carga por carretera",
                            "Habilitación del Ministerio de Transporte para transporte de carga"),
                    ciiu("5210", "Almacenamiento y depósito"),
                    regulada("5621", "Catering para eventos",
                            "Concepto sanitario favorable de la autoridad de salud"),
                    ciiu("4111", "Construcción de edificios residenciales"),
                    ciiu("4112", "Construcción de edificios no residenciales"),
                    ciiu("4330", "Terminación y acabado de edificios y obras de ingeniería civil"),
                    regulada("8010", "Actividades de seguridad privada",
                            "Licencia de funcionamiento de la Superintendencia de Vigilancia y Seguridad Privada"),
                    regulada("3822", "Tratamiento y disposición de desechos peligrosos",
                            "Licencia ambiental vigente"),
                    generica("4690", "Comercio al por mayor no especializado"),
                    generica("8299", "Otras actividades de servicio de apoyo a las empresas n.c.p."),
                    ActividadCiiu.builder().codigo("7820")
                            .descripcion("Actividades de empresas de servicios temporales")
                            .requiereRevisionLegal(true)
                            .requisitoRegulatorio("Autorización del Ministerio del Trabajo")
                            .build()
            ));
        }

        if (nichoRepository.count() == 0) {
            nichoRepository.saveAll(List.of(
                    nicho("Aseo y limpieza", "8121", "8129", "8130"),
                    nicho("Tecnología", "62", "6311"),
                    nicho("Transporte y logística", "4923", "5210"),
                    nicho("Alimentos", "56"),
                    nicho("Construcción", "41", "4330"),
                    nicho("Seguridad privada", "8010"),
                    nicho("Gestión de residuos", "3822")
            ));
        }
    }

    private static ActividadCiiu ciiu(String codigo, String descripcion) {
        return ActividadCiiu.builder().codigo(codigo).descripcion(descripcion).build();
    }

    private static ActividadCiiu regulada(String codigo, String descripcion, String requisito) {
        return ActividadCiiu.builder().codigo(codigo).descripcion(descripcion)
                .requisitoRegulatorio(requisito).build();
    }

    private static ActividadCiiu generica(String codigo, String descripcion) {
        return ActividadCiiu.builder().codigo(codigo).descripcion(descripcion).generica(true).build();
    }

    private static Nicho nicho(String nombre, String... prefijos) {
        return Nicho.builder().nombre(nombre).prefijosCiiu(Set.of(prefijos)).build();
    }
}
