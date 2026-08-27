# Ambiente de desarrollo y pruebas

## Requisitos

| Herramienta | Versión | Rol |
|---|---|---|
| Java | 17 o superior | Ejecución y compilación |
| Spring Boot | 3.5.14 | Framework backend |
| Maven | Wrapper incluido | Construcción |
| MySQL | 8.x recomendado | Persistencia |
| Git | Versión disponible en el equipo | Control de versiones |
| Postman | Versión disponible en el equipo | Pruebas REST |
| VS Code / IntelliJ IDEA | Versión disponible en el equipo | Desarrollo |

## Configuración

Las credenciales y secretos se configuran mediante `.env`, que no debe subirse al repositorio.

## Inicio

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

## URL

```text
http://localhost:8080
```
