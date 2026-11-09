package com.epcore.multicliente;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Una base de datos por cliente. La base "principal" es la de siempre
 * (spring.datasource.*); cada cliente configurado agrega la suya.
 */
@Configuration
@EnableConfigurationProperties(MultiClienteProperties.class)
public class MultiClienteConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties principal, MultiClienteProperties props) {
        Map<Object, Object> destinos = new LinkedHashMap<>();
        destinos.put(ClienteContexto.PRINCIPAL,
                principal.initializeDataSourceBuilder().type(HikariDataSource.class).build());

        if (props.isHabilitado()) {
            props.getClientes().forEach((id, c) -> {
                if (!MultiClienteProperties.ID_VALIDO.matcher(id).matches() || ClienteContexto.PRINCIPAL.equals(id)) {
                    throw new IllegalStateException("Identificador de cliente no válido: " + id);
                }
                if (c.getUrl() == null || c.getUsuario() == null) {
                    throw new IllegalStateException("Falta la url o el usuario de la base del cliente " + id);
                }
                HikariDataSource ds = new HikariDataSource();
                ds.setPoolName("epcore-" + id);
                ds.setJdbcUrl(c.getUrl());
                ds.setUsername(c.getUsuario());
                ds.setPassword(c.getClave());
                ds.setMaximumPoolSize(5);
                destinos.put(id, ds);
            });
        }

        ClienteRoutingDataSource routing = new ClienteRoutingDataSource();
        routing.setTargetDataSources(destinos);
        routing.setDefaultTargetDataSource(destinos.get(ClienteContexto.PRINCIPAL));
        routing.afterPropertiesSet();
        return routing;
    }

    /** El filtro de cliente va antes que la seguridad: así el login consulta la base correcta. */
    @Bean
    public FilterRegistrationBean<ClienteFilter> clienteFilter(MultiClienteProperties props) {
        FilterRegistrationBean<ClienteFilter> reg = new FilterRegistrationBean<>(new ClienteFilter(props));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        reg.addUrlPatterns("/*");
        return reg;
    }
}
