# Recomendaciones: procesos de contratación privados y públicos

## Decisión recomendada

Un **único módulo "Procesos de contratación"** en el que la **modalidad** (Entidad financiera, Privada o Pública) es una plantilla configurable, en lugar de dos secciones separadas.

**Por qué:** el ciclo de vida es el mismo en todas las modalidades: requisitos, aprobación, publicación, ofertas, adendas, evaluación y adjudicación. Lo que cambia son las **reglas**. Dos módulos separados duplicarían código, pruebas y mantenimiento.

| Aspecto | Privada / Entidad financiera | Pública |
|---|---|---|
| Requisitos | Los define la empresa | Los fija la ley y el pliego (Ley 80/1993, Ley 1150/2007, Decreto 1082/2015) |
| SARLAFT / ciberseguridad (C.E. 007/2018 SFC) | Sí, sobre todo en entidades financieras | No es el foco |
| Puntaje por desempeño histórico | Permitido | No aplica |
| Documentos | Los de la empresa | RUP, antecedentes, parafiscales, pólizas |
| Publicación | Interna | SECOP II |
| Factor adicional | — | Apoyo a la industria nacional (Ley 816/2003) |

## Interfaz

- En el menú, una sola opción: **Procesos de contratación**.
- Pestañas o filtros visibles: *Privados*, *Entidades financieras* y *Licitaciones públicas*.
- Al crear un proceso, lo primero que se elige es la **modalidad**. De ella salen la plantilla de requisitos, los factores de puntaje y los avisos legales.
- Cada proceso muestra una etiqueta con su modalidad.
- La pantalla **Análisis por nicho** sigue siendo para proveedores, no para crear procesos. Se enlaza con los procesos, por ejemplo: "2 procesos abiertos en este nicho".
- **Escalabilidad:** cada empresa cliente activa o desactiva las modalidades en su configuración. Un banco que no licita con el Estado no ve "Licitación pública".

## Diseño en Java (Spring Boot)

- `Proceso` (entidad), con `Modalidad modalidad` (enum `PRIVADA`, `FINANCIERA`, `PUBLICA`).
- `ReglasModalidad` (interfaz, **patrón Strategy**), con las implementaciones `ReglasPrivada`, `ReglasFinanciera` y `ReglasPublica`. Por ejemplo, `ReglasPublica` desactiva el puntaje por desempeño y exige el RUP.
- Plantillas de requisitos **en base de datos** (`PlantillaRequisito`), editables por el administrador.
- `ConfiguracionEmpresa` guarda las modalidades habilitadas por cliente.
- Clases nuevas previstas, ligadas a `Proveedor`:
  - `InformacionFinanciera`: liquidez = AC/PC, endeudamiento = PT/AT, cobertura de intereses = UO/GI, patrimonio.
  - `ContratoExperiencia`: valor, códigos UNSPSC, fechas, certificación.
  - `ConsultaSarlaft`: listas OFAC, ONU, Procuraduría, Contraloría y Policía; resultado, fecha y responsable.
  - Además: `ProcesoRequisito`, `Adenda` y `Oferta`.

## Prototipo

El prototipo funcional (MVP) que valida estas reglas está en [`docs/mvp/index.html`](mvp/index.html). Es un archivo HTML autónomo con datos de ejemplo ficticios. Para abrirlo, sírvelo con un servidor local, por ejemplo:

```
python -m http.server 8765 --directory docs/mvp
```

Pendiente: la aprobación del diseño por parte de Luis Torres antes de implementarlo en el código Java.

## Modelo mixto: portal de proveedores

Ejemplo: un banco contrata EpCore para seleccionar a sus proveedores de aseo.

**Lo que hace el proveedor en el portal web:**
- Se registra una sola vez. Se valida el dígito de verificación del NIT, que el NIT y el correo no estén repetidos y que acepte el tratamiento de datos (Ley 1581).
- Sube sus documentos. EpCore valida NIT y fechas al instante; si no puede, pasan a la aprobación dual interna.
- Ve las convocatorias abiertas y las invitaciones, revisa si cumple los requisitos y presenta una oferta confidencial, que puede reemplazar hasta el cierre. Al ofertar firma una declaración de que no tiene conflicto de interés.
- Recibe avisos de invitaciones, adendas y resultados. Solo conoce su propio resultado.

**Lo que se queda interno:** la evaluación, la aprobación dual, la consulta SARLAFT, las ofertas de los demás proponentes y la adjudicación.

**Visibilidad del proceso:** *por invitación* (solo los proveedores invitados lo ven) o *convocatoria abierta*. Los procesos de entidades públicas se presentan oficialmente en SECOP II.

**Implicaciones en el código Java:**
- Rol `PROVEEDOR` vinculado a un `Proveedor`, con autorización a nivel de registro: un proveedor nunca puede ver los datos de otro.
- Rutas separadas: `/portal/**` para proveedores y la parte interna para el comprador.
- `Proceso.visibilidad` (`INVITACION`, `ABIERTO` o `SECOP`) y una relación `ProcesoInvitado`.
- `Oferta.canal` (`PORTAL` o `INTERNO`) y la declaración de conflicto de interés.
- Registro de proveedores con validación del NIT y verificación del correo.
