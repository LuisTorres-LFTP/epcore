package com.epcore.multicliente;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Configuración de clientes. Ejemplo (variables de entorno o archivo externo, nunca en Git):
 * <pre>
 * epcore.multicliente.habilitado=true
 * epcore.multicliente.dominio-base=epcore.co
 * epcore.multicliente.clientes.bancocafetero.nombre=Banco Cafetero de Colombia
 * epcore.multicliente.clientes.bancocafetero.url=jdbc:mysql://db-bc:3306/epcore_bancocafetero
 * epcore.multicliente.clientes.bancocafetero.usuario=${BC_DB_USER}
 * epcore.multicliente.clientes.bancocafetero.clave=${BC_DB_PASSWORD}
 * epcore.multicliente.clientes.bancocafetero.admin-correo=accesos@bancocafetero.co
 * epcore.multicliente.clientes.bancocafetero.admin-clave=${BC_ADMIN_INICIAL}
 * </pre>
 * Con habilitado=false (por defecto) EpCore usa una sola base, como siempre.
 */
@ConfigurationProperties(prefix = "epcore.multicliente")
public class MultiClienteProperties {

    /** Identificador de cliente válido: minúsculas, números y guion, como un subdominio. */
    public static final Pattern ID_VALIDO = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,38}[a-z0-9])?");

    private boolean habilitado = false;

    /** Dominio desde el que se resuelve el cliente por subdominio (bancocafetero.epcore.co). */
    private String dominioBase = "epcore.co";

    private Map<String, Cliente> clientes = new LinkedHashMap<>();

    public static class Cliente {
        private String nombre;
        private String url;
        private String usuario;
        private String clave;
        /** Primer administrador de accesos que crea EpCore al dar de alta al cliente. */
        private String adminCorreo;
        private String adminClave;

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getClave() { return clave; }
        public void setClave(String clave) { this.clave = clave; }
        public String getAdminCorreo() { return adminCorreo; }
        public void setAdminCorreo(String adminCorreo) { this.adminCorreo = adminCorreo; }
        public String getAdminClave() { return adminClave; }
        public void setAdminClave(String adminClave) { this.adminClave = adminClave; }
    }

    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }
    public String getDominioBase() { return dominioBase; }
    public void setDominioBase(String dominioBase) { this.dominioBase = dominioBase; }
    public Map<String, Cliente> getClientes() { return clientes; }
    public void setClientes(Map<String, Cliente> clientes) { this.clientes = clientes; }
}
