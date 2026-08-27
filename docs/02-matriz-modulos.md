# Matriz de módulos - EPCORE

| Módulo | Responsabilidad | Entrada | Salida | Se conecta con |
|---|---|---|---|---|
| Autenticación | Validar credenciales y generar JWT | correo + contraseña | token JWT | Spring Security, UsuarioRepository |
| Usuarios | Crear, consultar y administrar usuarios | datos de usuario | Usuario persistido / lista | UsuarioRepository, BCrypt |
| Proveedores | Gestionar proveedores | datos de proveedor | Proveedor persistido / lista | ProveedorRepository |
| Criterios | Gestionar criterios de evaluación | nombre, descripción, peso | Criterio persistido / lista | CriterioRepository |
| Evaluaciones | Crear y consultar evaluaciones | proveedor, fecha, calificaciones | Evaluación y resultado | EvaluacionRepository, CriterioService |
| Detalle evaluación | Persistir la calificación de cada criterio | evaluación, criterio, calificación, peso | detalle persistido | EvaluacionDetalleRepository |
| Persistencia | Guardar y recuperar información | entidades / consultas JPA | registros MySQL | MySQL |

## Flujo de integración principal

```text
Interfaz
   ↓
Web Controller
   ↓
Service
   ↓
Repository
   ↓
MySQL
   ↑
Respuesta
```
