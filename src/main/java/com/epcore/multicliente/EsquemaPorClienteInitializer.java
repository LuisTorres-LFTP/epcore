package com.epcore.multicliente;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * Hibernate crea o actualiza las tablas solo en la base principal al arrancar.
 * Este paso hace lo mismo en la base de cada cliente configurado, con las
 * mismas entidades y las mismas reglas de nombres. Se ejecuta antes que los
 * demás procesos de arranque (roles, catálogos, administrador inicial).
 */
@Component
@Order(-100)
public class EsquemaPorClienteInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EsquemaPorClienteInitializer.class);

    private final ClientesConfigurados clientes;
    private final EntityManagerFactory emf;
    private final DataSource dataSource;
    private final String ddlAuto;

    public EsquemaPorClienteInitializer(ClientesConfigurados clientes, EntityManagerFactory emf, DataSource dataSource,
                                        @Value("${spring.jpa.hibernate.ddl-auto:none}") String ddlAuto) {
        this.clientes = clientes;
        this.emf = emf;
        this.dataSource = dataSource;
        this.ddlAuto = ddlAuto;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"update".equalsIgnoreCase(ddlAuto)) {
            return; // con migraciones controladas, cada base se actualiza con su propio proceso
        }
        List<Class<?>> entidades = emf.getMetamodel().getEntities().stream()
                .map(EntityType::getJavaType).<Class<?>>map(c -> c).toList();

        for (String id : clientes.ids()) {
            if (ClienteContexto.PRINCIPAL.equals(id)) {
                continue; // Hibernate ya la actualizó
            }
            ClienteContexto.ejecutarComo(id, () -> actualizar(id, entidades));
        }
    }

    private void actualizar(String id, List<Class<?>> entidades) {
        StandardServiceRegistry registro = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DATASOURCE, dataSource) // enrutado: apunta a la base del cliente
                .applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.MySQLDialect")
                .applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName())
                .applySetting(AvailableSettings.IMPLICIT_NAMING_STRATEGY, SpringImplicitNamingStrategy.class.getName())
                .applySetting(AvailableSettings.HBM2DDL_AUTO, "update")
                .applySetting(AvailableSettings.HBM2DDL_HALT_ON_ERROR, true)
                .build();
        try {
            MetadataSources fuentes = new MetadataSources(registro);
            entidades.forEach(fuentes::addAnnotatedClass);
            Metadata metadata = fuentes.buildMetadata();
            // Al construir la fábrica, Hibernate aplica hbm2ddl=update sobre la base del cliente.
            metadata.buildSessionFactory().close();
            log.info("Esquema actualizado para el cliente {}", id);
        } finally {
            StandardServiceRegistryBuilder.destroy(registro);
        }
    }
}
