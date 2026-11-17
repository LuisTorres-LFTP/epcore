# EPCORE

Sistema web para la gestión, evaluación y contratación de proveedores. Cubre el ciclo completo:
registro y verificación del proveedor, debida diligencia, evaluación de desempeño ponderada y
procesos de contratación con aprobación dual, ofertas desde un portal y adjudicación.

Desarrollado con Java 17+, Spring Boot 3.5, Spring Security, Thymeleaf, JPA/Hibernate, Flyway y MySQL 8.

## Módulos

| Módulo | Qué hace |
|---|---|
| Proveedores | Registro, edición y consulta. NIT único y campos obligatorios validados en el servidor. |
| Expediente del proveedor | Información financiera, contratos de experiencia, clasificación por CIIU, tipo de empresa y nicho, y consultas SARLAFT contra listas restrictivas. |
| Documentos | Carga de documentos del proveedor con **aprobación dual** (Director de compras y Gerente) y registro del motivo de rechazo. |
| Criterios de evaluación | Criterios con peso de 1 a 100. Los pesos de los criterios activos deben sumar **100 %** para poder evaluar. |
| Evaluaciones | Calificación de 0 a 100 por criterio, resultado ponderado, ranking de proveedores e indicadores por sector económico. |
| Procesos de contratación | Modalidades privada, financiera y pública; aprobación dual de términos; invitación o convocatoria abierta; ofertas; calificación técnica; adjudicación o declaratoria de desierto; adendas. |
| Portal de proveedores | Registro público del proveedor, carga de sus documentos, consulta de procesos a los que fue invitado y presentación de ofertas. |
| Notificaciones | Bandeja de notificaciones por rol y por proveedor, con envío de correo. |
| Catálogos | Carga del catálogo CIIU y de listas restrictivas oficiales. |
| Usuarios, roles y permisos | Roles predefinidos y roles personalizados por cliente combinando permisos, con reglas de separación de funciones. |
| API REST | Autenticación con JWT y consulta de proveedores, usuarios y roles. |

## Arquitectura

```text
Navegador (Thymeleaf)        Cliente REST (Postman)
        │                            │
  Cadena web: sesión + CSRF    Cadena API: JWT sin sesión
        └──────────┬─────────────────┘
             Controladores
                   │
               Servicios  ← reglas de negocio y validaciones
                   │
         Repositorios (Spring Data JPA)
                   │
    MySQL (una base por cliente, esquema con Flyway)
```

- **Una base de datos por cliente:** el cliente se identifica por el subdominio
  (`bancocafetero.epcore.co` → `bancocafetero`). En desarrollo se usa la base principal `epcore_db`.
- **Esquema con Flyway:** las tablas se crean y cambian con las migraciones de
  `src/main/resources/db/migration`; JPA solo valida el esquema.

## Roles y separación de funciones

El acceso se decide por **permisos**, no por nombre de rol. Cada cliente puede ajustar los permisos
de los roles o crear roles propios desde la pantalla *Roles y permisos*.

| Rol | Puede | No puede (separación de funciones) |
|---|---|---|
| Administrador | Gestionar usuarios, roles, catálogos, proveedores y evaluaciones. | Aprobar documentos, crear procesos ni adjudicar. |
| Analista de compras | Registrar proveedores, cargar documentos, evaluar y crear procesos. | Aprobar lo que él mismo crea. |
| Director de compras | Primera aprobación de documentos y procesos; calificación técnica. | Crear procesos. |
| Gerente (supervisor) | Segunda aprobación, publicar procesos y adjudicar. | Aprobar dos veces el mismo trámite. |
| Oficial de cumplimiento | Consultas SARLAFT. | Intervenir en compras. |
| Evaluador técnico | Calificar técnicamente las ofertas. | — |
| Área solicitante | Evaluar el desempeño del proveedor. | — |
| Auditor | Consultar y ver trazabilidad. | Modificar cualquier dato. |
| Proveedor (portal) | Ver y gestionar solo la información de su empresa y ofertar. | Entrar al área interna. |

## Seguridad

- Contraseñas con BCrypt; mínimo 8 caracteres con letras y números.
- Protección CSRF en la aplicación web y JWT sin sesión en la API (`/api/**`).
- Bloqueo temporal de la cuenta tras varios intentos fallidos de inicio de sesión.
- Cookies de sesión `HttpOnly` y `SameSite=Lax` (`Secure` en producción con `COOKIE_SEGURA=true`).
- Las páginas de error nunca muestran trazas ni mensajes técnicos.
- Claves y secretos fuera del código: se leen de variables de entorno o del archivo `.env`.

## Requisitos

- Java 17 o superior (probado con JDK 21).
- MySQL 8.
- Maven Wrapper incluido (`mvnw` / `mvnw.cmd`); no hace falta instalar Maven.
- Opcional: IntelliJ IDEA y Postman.

## Instalación local

1. Crear la base de datos y el usuario en MySQL:

   ```sql
   CREATE DATABASE epcore_db CHARACTER SET utf8mb4;
   CREATE USER 'epcore_user'@'localhost' IDENTIFIED BY 'su-clave';
   GRANT ALL PRIVILEGES ON epcore_db.* TO 'epcore_user'@'localhost';
   ```

2. Copiar `.env.example` como `.env` en la carpeta del proyecto y completar al menos `DB_PASSWORD`
   y `JWT_SECRET`. EpCore lee este archivo automáticamente; también acepta variables de entorno.
   El archivo `.env` no se sube a Git.

3. Ejecutar la aplicación desde la carpeta del proyecto:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   En Linux o macOS: `./mvnw spring-boot:run`. En IntelliJ IDEA basta con ejecutar `EpcoreApplication`
   (el directorio de trabajo debe ser la carpeta del proyecto para que encuentre el `.env`).

4. Abrir `http://localhost:8080`.

En una base nueva, Flyway crea el esquema al arrancar. Para tener un administrador inicial,
defina `INITIAL_ADMIN_EMAIL` e `INITIAL_ADMIN_PASSWORD` en el `.env`.

## Modo demostración

Para mostrar todos los módulos funcionando de punta a punta, active en el `.env`:

```properties
DEMO_HABILITADO=true
DEMO_CLAVE=una-clave-con-letras-y-numeros
```

Al arrancar, EpCore crea (solo una vez):

- Un usuario por rol, todos con la clave de `DEMO_CLAVE`: `analista@demo.epcore.test`,
  `director@demo.epcore.test`, `gerente@demo.epcore.test`, `cumplimiento@demo.epcore.test`,
  `evaluador@demo.epcore.test`, `auditor@demo.epcore.test` y `admin@demo.epcore.test`.
- Dos proveedores del portal: `proveedor1@demo.epcore.test` y `proveedor2@demo.epcore.test`.
- Tres procesos de contratación: uno en borrador, uno en aprobación y uno publicado con una oferta.

Los datos se crean con los mismos servicios de la aplicación, así que respetan sus validaciones y
la separación de funciones. **Nunca active el modo demostración en una instalación real.**

## API REST

```text
POST /api/auth/login        { "correo": "...", "password": "..." }  → token Bearer
GET  /api/auth/me           Authorization: Bearer <TOKEN>
GET  /api/proveedores       Authorization: Bearer <TOKEN>
```

Sin token válido, las rutas protegidas responden `401`. Los datos inválidos responden `400` con un
mensaje en el campo `error`. La colección de Postman está en `postman/`.

## Pruebas

```powershell
.\mvnw.cmd test
```

Incluye pruebas de seguridad (escalada de privilegios, CSRF, ataques comunes), de roles y separación
de funciones, de los servicios de negocio y de las validaciones de proveedores, criterios, usuarios y
evaluaciones. El flujo de integración continua de GitHub (`.github/workflows/ci.yml`) las ejecuta
contra un MySQL temporal en cada cambio.

## Operación

- HTTPS detrás de Apache o Nginx, alta de clientes, migraciones y respaldos:
  ver [docs/07-https-y-respaldos.md](docs/07-https-y-respaldos.md).
- Scripts en `scripts/`: `alta-cliente` (nueva base de cliente) y `respaldo` (copia de seguridad).

## Documentación

| Documento | Contenido |
|---|---|
| [docs/01-documento-tecnico.md](docs/01-documento-tecnico.md) | Arquitectura, módulos, modelo de datos y decisiones de seguridad. |
| [docs/02-matriz-modulos.md](docs/02-matriz-modulos.md) | Responsabilidad, entradas, salidas y dependencias de cada módulo. |
| [docs/03-ambiente.md](docs/03-ambiente.md) | Ambiente de desarrollo y pruebas. |
| [docs/06-recomendaciones-procesos-contratacion.md](docs/06-recomendaciones-procesos-contratacion.md) | Diseño de los procesos de contratación. |
| [docs/07-https-y-respaldos.md](docs/07-https-y-respaldos.md) | Puesta en producción. |

## Control de versiones

No subir a Git: `.env`, `config/clientes.properties`, `target/`, `uploads/` ni ninguna clave.
El archivo `.env.example` sí se versiona como plantilla.
