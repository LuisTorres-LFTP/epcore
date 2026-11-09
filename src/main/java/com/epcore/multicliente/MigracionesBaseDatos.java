package com.epcore.multicliente;

import jakarta.persistence.Entity;
import org.flywaydb.core.Flyway;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aplica las migraciones versionadas (src/main/resources/db/migration) a la base
 * principal y a la de cada cliente, antes de que arranque JPA.
 *
 * Transición desde ddl-auto=update: una base que ya tenía tablas pero no tiene
 * historial de Flyway se pone al día UNA sola vez con Hibernate (solo agrega
 * tablas y columnas, nunca borra) y se marca como versión 1. Desde ahí, todo
 * cambio de esquema va en un archivo V2__, V3__... revisable en Git.
 *
 * La base principal la valida JPA (ddl-auto=validate); las de clientes se
 * validan aquí con las mismas reglas, para no arrancar con un esquema distinto.
 */
@Component
public class MigracionesBaseDatos implements FlywayMigrationStrategy {

    private static final Logger log = LoggerFactory.getLogger(MigracionesBaseDatos.class);
    private static final String PAQUETE_ENTIDADES = "com.epcore.entity";

    private final DataSource dataSource;
    private final MultiClienteProperties props;

    public MigracionesBaseDatos(DataSource dataSource, MultiClienteProperties props) {
        this.dataSource = dataSource;
        this.props = props;
    }

    @Override
    public void migrate(Flyway flyway) {
        List<String> ids = new ArrayList<>();
        ids.add(ClienteContexto.PRINCIPAL);
        if (props.isHabilitado()) ids.addAll(props.getClientes().keySet());

        for (String id : ids) {
            ClienteContexto.ejecutarComo(id, () -> {
                // El DataSource es enrutado: dentro de ejecutarComo apunta a la base de este cliente.
                Flyway f = Flyway.configure().configuration(flyway.getConfiguration()).dataSource(dataSource).load();
                if (esBaseAnteriorAFlyway()) {
                    log.warn("Base '{}' creada antes de las migraciones: se pone al día y se marca como versión 1.", id);
                    hibernate("update");
                    f.baseline();
                }
                int aplicadas = f.migrate().migrationsExecuted;
                if (!ClienteContexto.PRINCIPAL.equals(id)) hibernate("validate");
                log.info("Base '{}': {} migraciones aplicadas, versión {}", id, aplicadas, f.info().current() == null
                        ? "-" : f.info().current().getVersion());
            });
        }
    }

    /** Tiene tablas de EpCore pero aún no tiene el historial de Flyway. */
    private boolean esBaseAnteriorAFlyway() {
        try (Connection c = dataSource.getConnection()) {
            return existeTabla(c, "usuarios") && !existeTabla(c, "flyway_schema_history");
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo revisar la base de datos: " + e.getMessage(), e);
        }
    }

    private static boolean existeTabla(Connection c, String tabla) throws SQLException {
        try (ResultSet rs = c.getMetaData().getTables(c.getCatalog(), null, tabla, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    /** Ejecuta hbm2ddl (update o validate) con las mismas entidades y reglas de nombres que JPA. */
    private void hibernate(String modo) {
        StandardServiceRegistry registro = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DATASOURCE, dataSource)
                .applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.MySQLDialect")
                .applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName())
                .applySetting(AvailableSettings.IMPLICIT_NAMING_STRATEGY, SpringImplicitNamingStrategy.class.getName())
                .applySetting(AvailableSettings.HBM2DDL_AUTO, modo)
                .applySetting(AvailableSettings.HBM2DDL_HALT_ON_ERROR, true)
                .build();
        try {
            MetadataSources fuentes = new MetadataSources(registro);
            entidades().forEach(fuentes::addAnnotatedClass);
            fuentes.buildMetadata().buildSessionFactory().close();
        } finally {
            StandardServiceRegistryBuilder.destroy(registro);
        }
    }

    static List<Class<?>> entidades() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        List<Class<?>> clases = new ArrayList<>();
        scanner.findCandidateComponents(PAQUETE_ENTIDADES).forEach(bd ->
                clases.add(ClassUtils.resolveClassName(bd.getBeanClassName(), MigracionesBaseDatos.class.getClassLoader())));
        return clases;
    }
}
