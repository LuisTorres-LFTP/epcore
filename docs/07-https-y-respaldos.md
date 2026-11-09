# Operación: HTTPS, clientes, migraciones y respaldos

Esta guía es para poner EpCore en un servidor real. En el computador de desarrollo
nada de esto es obligatorio: EpCore sigue funcionando en `http://localhost:8080`.

## 1. HTTPS

EpCore **no** maneja el certificado: lo hace un servidor web delante (Apache o Nginx),
que recibe en el puerto 443 y le pasa las peticiones a EpCore en `127.0.0.1:8080`.
Así el certificado se renueva sin reiniciar la aplicación.

### Apache (Ubuntu)

```bash
sudo apt install apache2 certbot python3-certbot-apache
sudo a2enmod proxy proxy_http headers ssl rewrite
```

`/etc/apache2/sites-available/epcore.conf`:

```apache
<VirtualHost *:80>
    ServerName epcore.co
    ServerAlias *.epcore.co
    RewriteEngine On
    RewriteRule ^ https://%{HTTP_HOST}%{REQUEST_URI} [R=301,L]
</VirtualHost>

<VirtualHost *:443>
    ServerName epcore.co
    ServerAlias *.epcore.co

    SSLEngine on
    SSLCertificateFile    /etc/letsencrypt/live/epcore.co/fullchain.pem
    SSLCertificateKeyFile /etc/letsencrypt/live/epcore.co/privkey.pem
    SSLProtocol -all +TLSv1.2 +TLSv1.3

    # EpCore necesita el subdominio original para saber qué cliente es
    ProxyPreserveHost On
    RequestHeader set X-Forwarded-Proto "https"
    ProxyPass        / http://127.0.0.1:8080/
    ProxyPassReverse / http://127.0.0.1:8080/

    # 11 MB: un poco más que el máximo de archivo de EpCore (10 MB)
    LimitRequestBody 11534336
</VirtualHost>
```

Certificado (el comodín `*.epcore.co` cubre a todos los clientes; Let's Encrypt lo
emite con validación por DNS):

```bash
sudo certbot certonly --manual --preferred-challenges dns -d epcore.co -d "*.epcore.co"
sudo a2ensite epcore && sudo systemctl reload apache2
```

### Variables de EpCore en producción

| Variable | Valor | Para qué |
|---|---|---|
| `COOKIE_SEGURA` | `true` | La cookie de sesión solo viaja por HTTPS |
| `SERVER_ADDRESS` | `127.0.0.1` | EpCore solo escucha al proxy, no a Internet |
| `JWT_SECRET` | 32+ caracteres aleatorios | Firma de los tokens de la API |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | base principal | |
| `EPCORE_MULTICLIENTE_DOMINIO_BASE` | `epcore.co` | Para reconocer `cliente.epcore.co` |

EpCore ya trae `server.forward-headers-strategy=native`: con el proxy en la misma
máquina o red interna, sabe que la petición original fue HTTPS. Spring Security
agrega entonces HSTS (el navegador recuerda usar siempre HTTPS) y las demás
cabeceras de seguridad.

> Sin proxy (no recomendado): Spring Boot puede servir HTTPS directo con
> `SERVER_SSL_ENABLED=true`, `SERVER_SSL_KEY_STORE=/ruta/epcore.p12`,
> `SERVER_SSL_KEY_STORE_PASSWORD=...`, `SERVER_SSL_KEY_STORE_TYPE=PKCS12` y `SERVER_PORT=8443`.

## 2. Dar de alta un cliente nuevo

Un solo comando crea la base del cliente, su usuario de MySQL (con permisos solo
sobre esa base) y lo agrega a `config/clientes.properties` (archivo local, fuera de Git):

```powershell
.\scripts\alta-cliente.ps1 -Id bancocafetero -Nombre "Banco Cafetero" -AdminCorreo accesos@bancocafetero.co
```

```bash
scripts/alta-cliente.sh bancocafetero "Banco Cafetero" accesos@bancocafetero.co
```

El script pide la contraseña de root de MySQL (no la guarda) y muestra la clave
inicial del administrador del cliente. Al reiniciar EpCore se crean solos las tablas,
los roles, los catálogos y ese administrador. El cliente entra por
`https://bancocafetero.epcore.co`.

## 3. Cambios en la base de datos (migraciones)

El esquema ya no lo cambia Hibernate al arrancar: lo cambian archivos versionados en
`src/main/resources/db/migration` (Flyway), y JPA solo **valida** que coincida.

- Para cambiar una tabla se crea un archivo nuevo `V3__descripcion.sql`, `V4__...`.
  Nunca se edita uno que ya se aplicó.
- Al arrancar, EpCore aplica las migraciones pendientes en la base principal y en la
  de cada cliente, en orden, y registra cada una en `flyway_schema_history`.
- Si una entidad y la base no coinciden, EpCore no arranca y dice qué falta
  (mejor fallar al inicio que guardar datos mal).
- Las bases creadas antes de Flyway se ponen al día una sola vez y se marcan como
  versión 1, sin borrar nada.

## 4. Respaldos

```bash
# Linux, cada noche a las 2:30 (crontab -e)
30 2 * * * cd /opt/epcore && scripts/respaldo.sh >> /var/log/epcore-respaldo.log 2>&1
```

```powershell
# Windows (se puede programar en el Programador de tareas)
.\scripts\respaldo.ps1 -Destino D:\Respaldos\EpCore -RetencionDias 14
```

Cada respaldo es una carpeta con fecha que contiene la base principal, la de cada
cliente, los documentos cargados y un archivo `SHA256SUMS` para verificar que nada
se dañó. Se conservan 14 días (configurable). Recomendación: copiar además la carpeta
de respaldos a otro lugar (otro disco o la nube), porque un respaldo en el mismo
servidor no sirve si el servidor se pierde.

### Restaurar

```bash
mysql -u root -p -e "CREATE DATABASE epcore_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
gunzip -c 20261109-0230/principal.sql.gz | mysql -u root -p epcore_db
tar -xzf 20261109-0230/documentos.tar.gz -C /opt/epcore
```

Probar la restauración de vez en cuando en una base aparte: un respaldo que nunca se
ha restaurado no se sabe si sirve.

## 5. Revisión automática (CI)

En cada `push` a GitHub, el flujo `.github/workflows/ci.yml` compila EpCore, corre
todas las pruebas y arranca la aplicación contra una base MySQL vacía, lo que prueba
también las migraciones. El resultado aparece en la pestaña **Actions** del repositorio.
