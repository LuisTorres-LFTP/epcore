# Acta de pruebas y aceptación - EPCORE

> Esta plantilla debe diligenciarse con los resultados reales de ejecución. No marcar una prueba como exitosa sin haberla ejecutado.

| ID | Prueba | Resultado esperado | Resultado obtenido | Estado | Evidencia |
|---|---|---|---|---|---|
| P01 | Login con credenciales válidas | 200 + JWT | Pendiente | ⏳ | Captura Postman |
| P02 | Login con credenciales inválidas | 401 | Pendiente | ⏳ | Captura Postman |
| P03 | `/api/proveedores` sin token | 401 | Pendiente | ⏳ | Captura Postman |
| P04 | `/api/proveedores` con JWT válido | 200 | Pendiente | ⏳ | Captura Postman |
| P05 | Token inválido | 401 | Pendiente | ⏳ | Captura Postman |
| P06 | Consultar usuario autenticado | 200 | Pendiente | ⏳ | Captura Postman |
| P07 | Crear proveedor | Registro guardado | Pendiente | ⏳ | Captura / BD |
| P08 | Editar proveedor | Registro actualizado | Pendiente | ⏳ | Captura / BD |
| P09 | Eliminar proveedor con rol permitido | Operación realizada | Pendiente | ⏳ | Captura |
| P10 | Usuario sin permiso intenta operación restringida | 403 | Pendiente | ⏳ | Captura |
| P11 | Crear evaluación | Evaluación guardada y calculada | Pendiente | ⏳ | Captura |
| P12 | Consultar ranking | Ranking correcto | Pendiente | ⏳ | Captura |

## Aceptación

Responsable de pruebas: ______________________________

Fecha: ______________________________

Observaciones: ______________________________________

Aceptación:  Sí [ ]   No [ ]
