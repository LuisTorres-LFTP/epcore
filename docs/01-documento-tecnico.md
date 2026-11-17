# Documento técnico - EPCORE

## 1. Identificación

| Aspecto | Detalle |
|---|---|
| Proyecto | EPCORE – Sistema de gestión, evaluación y contratación de proveedores |
| Lenguaje | Java 17+ |
| Framework | Spring Boot 3.5 (Web, Security, Data JPA, Validation, Mail, Thymeleaf) |
| Persistencia | Spring Data JPA / Hibernate sobre MySQL 8, esquema versionado con Flyway |
| Interfaz | Plantillas Thymeleaf renderizadas en el servidor |
| API | REST con autenticación JWT |
| Pruebas | JUnit 5 y Mockito; integración continua en GitHub Actions |

## 2. Propósito

Apoyar al área de compras en todo el ciclo de relación con proveedores: registro y verificación,
debida diligencia (SARLAFT y listas restrictivas), evaluación de desempeño ponderada y procesos de
contratación con aprobaciones, ofertas y adjudicación, con trazabilidad y separación de funciones.

## 3. Arquitectura

Arquitectura por capas con dos cadenas de seguridad independientes:

```text
Navegador (Thymeleaf)              Cliente REST
        │                               │
Cadena web: sesión + CSRF      Cadena API (/api/**): JWT, sin sesión
        └───────────────┬───────────────┘
                 Controladores        ← reciben la petición y muestran mensajes
                        │
                    Servicios         ← reglas de negocio y validaciones
                        │
             Repositorios (JPA)
                        │
       ClienteRoutingDataSource       ← elige la base del cliente
                        │
            MySQL (una base por cliente)
```

### 3.1 Multicliente

Cada empresa cliente tiene su propia base de datos. `ClienteFilter` identifica el cliente por el
subdominio y `ClienteRoutingDataSource` dirige las consultas a su base. Las migraciones de Flyway se
aplican a todas las bases al arrancar (`MigracionesBaseDatos`). Las conexiones de cada cliente se
configuran en `config/clientes.properties`, que se genera con `scripts/alta-cliente` y no está en Git.

### 3.2 Validaciones

Las reglas de negocio se validan en los **servicios**, no solo en el navegador. Los servicios lanzan
`IllegalArgumentException` o `IllegalStateException` con un mensaje para el usuario, y el controlador
lo muestra en el formulario (`mensajeError`). La API devuelve el mismo mensaje con estado `400`.

Ejemplos: NIT único y campos obligatorios del proveedor; peso del criterio entre 1 y 100; calificación
entre 0 y 100; suma de pesos de los criterios activos igual a 100 % para poder evaluar; correo único y
contraseña de mínimo 8 caracteres con letras y números; reglas de cada modalidad de contratación.

## 4. Módulos

| Módulo | Paquetes principales |
|---|---|
| Autenticación y seguridad | `config.SecurityConfig`, `security`, `auth` |
| Usuarios, roles y permisos | `UsuarioService`, `RolSistemaService`, `PerfilesPredeterminados`, `SeparacionFunciones` |
| Proveedores y expediente | `ProveedorService`, `InformacionFinancieraService`, `ContratoExperienciaService`, `ClasificadorProveedor` |
| Debida diligencia | `ConsultaSarlaftService`, catálogos de listas restrictivas |
| Documentos | `ProveedorDocumentoService`, `DocumentoAprobacionService` |
| Criterios y evaluaciones | `CriterioService`, `EvaluacionService`, `EvaluacionDetalleService` |
| Procesos de contratación | `ProcesoService`, `ReglasModalidad` (patrón Strategy), `EvaluadorOfertas` |
| Portal de proveedores | `PortalProveedorService`, `RegistroProveedorController` |
| Notificaciones | `NotificacionService`, `ProcesadorCorreos` |
| Multicliente | paquete `multicliente` |

El detalle de entradas, salidas y dependencias está en `02-matriz-modulos.md`.

## 5. Modelo de datos (principal)

```text
Proveedor 1───* ProveedorDocumento
    │  1───* ContratoExperiencia
    │  1───1 InformacionFinanciera
    │  1───* ConsultaSarlaft
    │  1───* Evaluacion 1───* EvaluacionDetalle *───1 Criterio
    │  *───* Proceso (invitados)
    │  1───* Oferta *───1 Proceso 1───* Adenda
Usuario *───0..1 Proveedor (usuarios del portal)
Usuario *───0..1 RolSistema (rol personalizado)
```

El resultado de una evaluación es la suma de `calificación × peso / 100` de cada criterio activo.

## 6. Seguridad

- **Permisos, no roles:** cada ruta exige un permiso (`Permiso`). Los roles son conjuntos de permisos
  ajustables por cliente.
- **Separación de funciones:** quien crea un proceso no lo aprueba ni lo adjudica; las dos aprobaciones
  las hacen personas distintas; el administrador no aprueba documentos ni adjudica.
- **Aprobación dual** de documentos y de términos de procesos.
- **Contraseñas** con BCrypt; las antiguas en texto plano se migran al arrancar.
- **CSRF** activo en la aplicación web; la API usa JWT y no usa cookies.
- **Bloqueo** temporal tras intentos fallidos de inicio de sesión.
- **Errores sin información interna:** páginas propias 403, 404 y 500; la traza queda solo en el registro.
- **Secretos fuera del código:** se leen de variables de entorno o del archivo `.env`.

## 7. Ejecución

Ver la sección *Instalación local* del `README.md`. Para mostrar todos los módulos con datos de
ejemplo, ver *Modo demostración* en el mismo archivo.

## 8. Pruebas

- Pruebas automáticas con `mvnw test`: seguridad, roles y separación de funciones, servicios de negocio,
  validaciones y multicliente. Se ejecutan en GitHub Actions en cada cambio.
- Pruebas manuales de la API con la colección `postman/EPCORE-SENA.postman_collection.json`.
