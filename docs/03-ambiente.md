# Ambiente de desarrollo y pruebas

## Requisitos

| Herramienta | Versión | Rol |
|---|---|---|
| Java | 17 o superior (probado con JDK 21) | Ejecución y compilación |
| Spring Boot | 3.5.14 | Framework backend |
| Maven | Wrapper incluido | Construcción y pruebas |
| MySQL | 8.0 | Persistencia (una base por cliente) |
| Flyway | Incluido en el proyecto | Migraciones del esquema |
| Git y GitHub Actions | — | Control de versiones e integración continua |
| Postman | Versión disponible en el equipo | Pruebas de la API REST |
| IntelliJ IDEA | Versión disponible en el equipo | Desarrollo |
| Navegador | Google Chrome u otro moderno | Pruebas de la interfaz |

## Configuración

Las claves y secretos nunca van en el código. EpCore los lee, en este orden:

1. Variables de entorno del sistema (así lo hace el CI de GitHub).
2. El archivo `.env` de la carpeta del proyecto, que se copia de `.env.example` y no se sube a Git.

Variables principales:

| Variable | Obligatoria | Uso |
|---|---|---|
| `DB_PASSWORD` | Sí | Clave de MySQL. Sin ella la aplicación no arranca. |
| `DB_URL`, `DB_USERNAME` | No | Conexión a MySQL (por defecto `epcore_db` local y `epcore_user`). |
| `JWT_SECRET` | Recomendada | Firma de los tokens de la API (mínimo 32 caracteres). Sin ella se genera una clave temporal. |
| `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` | No | Crea un administrador en una base nueva. |
| `DEMO_HABILITADO`, `DEMO_CLAVE` | No | Modo demostración (ver `README.md`). Solo para pruebas y presentaciones. |
| `COOKIE_SEGURA` | En producción | `true` cuando EpCore está detrás de HTTPS. |

## Inicio

Desde la carpeta del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux o macOS: `./mvnw spring-boot:run`. En IntelliJ IDEA se ejecuta la clase `EpcoreApplication`
con la carpeta del proyecto como directorio de trabajo.

```text
http://localhost:8080
```

## Pruebas

```powershell
.\mvnw.cmd test
```

Las pruebas usan la misma configuración (`.env` o variables de entorno) y necesitan MySQL disponible
para la prueba de arranque de la aplicación.
