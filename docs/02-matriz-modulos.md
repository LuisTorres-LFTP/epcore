# Matriz de módulos - EPCORE

| Módulo | Responsabilidad | Entrada | Salida | Se conecta con |
|---|---|---|---|---|
| Autenticación | Validar credenciales, bloquear tras intentos fallidos y generar JWT para la API | Correo y contraseña | Sesión web o token JWT | Spring Security, UsuarioRepository, LoginAttemptService |
| Usuarios | Crear, editar, activar e inactivar usuarios | Nombre, correo, contraseña, rol | Usuario guardado / lista | UsuarioRepository, BCrypt, RolSistemaService |
| Roles y permisos | Ajustar permisos de los roles y crear roles personalizados | Código, nombre, permisos | Rol guardado | RolSistemaRepository, SeparacionFunciones |
| Proveedores | Registrar y consultar proveedores | NIT, razón social, correo, sector, etc. | Proveedor guardado / lista | ProveedorRepository |
| Expediente | Información financiera, experiencia, clasificación y SARLAFT | Datos financieros, contratos, consultas | Expediente del proveedor | Servicios del expediente, catálogos |
| Documentos | Cargar documentos y aprobarlos en dos niveles | Archivo (máx. 10 MB), tipo, vigencia | Documento aprobado o rechazado | ProveedorDocumentoService, almacenamiento en `uploads/` |
| Criterios | Gestionar criterios de evaluación | Nombre, descripción, peso (1–100) | Criterio guardado; suma de pesos activos | CriterioRepository |
| Evaluaciones | Calificar proveedores y calcular el resultado | Proveedor, fecha, calificación (0–100) por criterio activo | Evaluación, detalle, ranking e indicadores | EvaluacionService, CriterioService |
| Procesos de contratación | Estructurar, aprobar, publicar, recibir ofertas, evaluar y adjudicar | Modalidad, presupuesto, cierre, invitados, ofertas | Proceso en su estado; resultado de la evaluación | ProcesoService, ReglasModalidad, NotificacionService |
| Portal de proveedores | Registro público, documentos y ofertas del proveedor | Datos de la empresa, archivos, oferta | Cuenta de portal, oferta registrada | PortalProveedorService, ProcesoService |
| Notificaciones | Avisar a roles y proveedores, y enviar correo | Eventos de los procesos y documentos | Notificación en bandeja y correo | NotificacionService, ProcesadorCorreos |
| Catálogos | Cargar CIIU y listas restrictivas | Archivo CSV / lista oficial | Catálogo actualizado | Servicios de catálogo |
| Multicliente | Dirigir cada petición a la base de su cliente | Subdominio | Conexión a la base del cliente | ClienteFilter, ClienteRoutingDataSource, Flyway |

## Flujo de integración principal

```text
Interfaz (Thymeleaf) o API REST
        ↓
Controlador ── muestra mensajeError si el servicio rechaza los datos
        ↓
Servicio ── valida reglas de negocio y separación de funciones
        ↓
Repositorio (JPA)
        ↓
MySQL de la empresa cliente
```

## Flujo de un proceso de contratación

```text
Analista crea borrador → envía a aprobación
  → Director de compras aprueba (1/2) → Gerente aprueba (2/2) y publica
  → Proveedores invitados ofertan desde el portal → se cierra la recepción
  → Evaluadores califican → Gerente adjudica o declara desierto
```
