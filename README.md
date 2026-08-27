# EPCORE

Sistema web para la gestión y evaluación de proveedores, desarrollado con Spring Boot, Spring Security, Thymeleaf, JPA/Hibernate y MySQL.

## Arquitectura

El flujo principal es:

```text
Interfaz Thymeleaf
      ↓
Web Controller
      ↓
Service
      ↓
Repository (JPA)
      ↓
MySQL
```

La autenticación web usa Spring Security y, adicionalmente, la API expone autenticación JWT para demostrar y probar consumo protegido con Postman.

## Módulos

- Usuarios y roles.
- Proveedores.
- Criterios de evaluación.
- Evaluaciones y detalle de evaluación.
- Autenticación y autorización.

## Requisitos

- Java 17 o superior.
- Maven Wrapper incluido (`mvnw` / `mvnw.cmd`).
- MySQL 8.x recomendado.
- Git.
- Postman para pruebas de API.

## Configuración local

1. Copiar `.env.example` como `.env`.
2. Completar `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`.
3. Crear la base de datos `epcore_db` en MySQL si todavía no existe.
4. Si es una instalación nueva, puedes definir `INITIAL_ADMIN_EMAIL` e `INITIAL_ADMIN_PASSWORD` en `.env` para crear automáticamente un administrador.
5. Ejecutar la aplicación:

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux/macOS

```bash
./mvnw spring-boot:run
```

La aplicación queda disponible en:

```text
http://localhost:8080
```

## Seguridad

Las contraseñas se almacenan utilizando BCrypt. Las contraseñas antiguas almacenadas en texto plano se migran a BCrypt al iniciar la aplicación. Si se configura `INITIAL_ADMIN_*`, el usuario inicial también se guarda con BCrypt.

> La migración es automática y solo afecta contraseñas que todavía no tengan formato BCrypt.

La API JWT utiliza:

```text
POST /api/auth/login
```

Ejemplo de body:

```json
{
  "correo": "usuario@correo.com",
  "password": "su-clave"
}
```

La respuesta contiene un token Bearer. Para probar una ruta protegida:

```text
GET /api/proveedores
Authorization: Bearer <TOKEN>
```

También puede consultarse el usuario autenticado:

```text
GET /api/auth/me
Authorization: Bearer <TOKEN>
```

Sin token válido, las rutas `/api/**` protegidas responden con `401 Unauthorized`.

## Control de acceso

- `ADMIN`: administración completa.
- `ANALISTA_COMPRAS`: operaciones de evaluación y gestión permitidas.
- `AUDITOR`: consulta.
- `GERENTE`: consulta.

## Control de versiones

No subir:

- `.env`
- `target/`
- credenciales o secretos

El archivo `.env.example` sí se versiona.

## Relación con la evidencia GA8-220501096-AA1-EV02

El proyecto permite documentar:

1. Módulos codificados y documentados.
2. Documento técnico del sistema.
3. Ambiente de desarrollo y pruebas.
4. Código mediante Git.
5. Acta de pruebas y aceptación.

Las capturas de pruebas deben demostrar el recorrido de extremo a extremo y las pruebas de autenticación, CRUD y permisos.
