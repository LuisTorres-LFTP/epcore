-- EpCore - Esquema inicial (versión 1).
-- Generado a partir de las entidades JPA. Las bases que ya existían antes de Flyway
-- se ponen al día una sola vez y se marcan en esta versión (ver MigracionesBaseDatos).
-- Los cambios siguientes van en V2__..., V3__..., nunca editando este archivo.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `adendas` (
  `numero` int NOT NULL,
  `fecha_emision` datetime(6) NOT NULL,
  `id_adenda` bigint NOT NULL AUTO_INCREMENT,
  `id_proceso` bigint NOT NULL,
  `emitida_por` varchar(120) NOT NULL,
  `justificacion` varchar(1000) NOT NULL,
  `cambios` varchar(3000) NOT NULL,
  PRIMARY KEY (`id_adenda`),
  UNIQUE KEY `UKjir6u1wm6xwkn0d2ehx0lvgrx` (`id_proceso`,`numero`),
  CONSTRAINT `FK10ldk222j4ey9xssrdxs69hn8` FOREIGN KEY (`id_proceso`) REFERENCES `procesos` (`id_proceso`)
) ENGINE=InnoDB;
CREATE TABLE `catalogo_ciiu` (
  `activo` bit(1) NOT NULL,
  `codigo` varchar(4) NOT NULL,
  `generica` bit(1) NOT NULL,
  `requiere_revision_legal` bit(1) NOT NULL,
  `version` varchar(30) NOT NULL,
  `descripcion` varchar(300) NOT NULL,
  `requisito_regulatorio` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`codigo`)
) ENGINE=InnoDB;
CREATE TABLE `correos_salientes` (
  `intentos` int NOT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `fecha_envio` datetime(6) DEFAULT NULL,
  `id_correo` bigint NOT NULL AUTO_INCREMENT,
  `proximo_intento` datetime(6) DEFAULT NULL,
  `destinatario` varchar(120) NOT NULL,
  `asunto` varchar(200) NOT NULL,
  `clave_idempotencia` varchar(200) NOT NULL,
  `ultimo_error` varchar(500) DEFAULT NULL,
  `cuerpo_html` text NOT NULL,
  `estado` enum('ENVIADO','FALLIDO','PENDIENTE') NOT NULL,
  PRIMARY KEY (`id_correo`),
  UNIQUE KEY `uk_correo_idempotencia` (`clave_idempotencia`),
  KEY `ix_correo_pendientes` (`estado`,`proximo_intento`)
) ENGINE=InnoDB;
CREATE TABLE `criterios` (
  `activo` bit(1) NOT NULL,
  `peso` int NOT NULL,
  `id_criterio` bigint NOT NULL AUTO_INCREMENT,
  `nombre_criterio` varchar(120) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_criterio`)
) ENGINE=InnoDB;
CREATE TABLE `evaluacion_detalles` (
  `calificacion` double NOT NULL,
  `peso` int NOT NULL,
  `resultado_ponderado` double NOT NULL,
  `id_criterio` bigint NOT NULL,
  `id_detalle` bigint NOT NULL AUTO_INCREMENT,
  `id_evaluacion` bigint NOT NULL,
  PRIMARY KEY (`id_detalle`),
  KEY `FKaevp2nfi4axbtab0qb5h5a9ko` (`id_criterio`),
  KEY `FK744o1cya34d9u8nhj97jcggp7` (`id_evaluacion`),
  CONSTRAINT `FK744o1cya34d9u8nhj97jcggp7` FOREIGN KEY (`id_evaluacion`) REFERENCES `evaluaciones` (`id_evaluacion`),
  CONSTRAINT `FKaevp2nfi4axbtab0qb5h5a9ko` FOREIGN KEY (`id_criterio`) REFERENCES `criterios` (`id_criterio`)
) ENGINE=InnoDB;
CREATE TABLE `evaluaciones` (
  `activo` bit(1) NOT NULL,
  `fecha_evaluacion` date NOT NULL,
  `resultado_final` double NOT NULL,
  `id_evaluacion` bigint NOT NULL AUTO_INCREMENT,
  `id_proveedor` bigint NOT NULL,
  `observaciones` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id_evaluacion`),
  KEY `FK97hepcpwh5rxpwlkehj632dv7` (`id_proveedor`),
  CONSTRAINT `FK97hepcpwh5rxpwlkehj632dv7` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`)
) ENGINE=InnoDB;
CREATE TABLE `listas_restrictivas_entradas` (
  `fecha_carga` datetime(6) NOT NULL,
  `id_entrada` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(300) NOT NULL,
  `nombre_normalizado` varchar(300) NOT NULL,
  `lista` enum('CONTRALORIA','OFAC','ONU','PEP','POLICIA','PROCURADURIA') NOT NULL,
  PRIMARY KEY (`id_entrada`),
  KEY `ix_lista_nombre` (`nombre_normalizado`),
  KEY `ix_lista_tipo` (`lista`)
) ENGINE=InnoDB;
CREATE TABLE `nicho_prefijos_ciiu` (
  `prefijo_ciiu` varchar(4) DEFAULT NULL,
  `id_nicho` bigint NOT NULL,
  KEY `FKt2reg9ygeikdr6dv02g8jhrsl` (`id_nicho`),
  CONSTRAINT `FKt2reg9ygeikdr6dv02g8jhrsl` FOREIGN KEY (`id_nicho`) REFERENCES `nichos` (`id_nicho`)
) ENGINE=InnoDB;
CREATE TABLE `nichos` (
  `activo` bit(1) NOT NULL,
  `id_nicho` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `descripcion` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`id_nicho`),
  UNIQUE KEY `UK1hc8n7e21ur3s98r248opckvw` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=8;
CREATE TABLE `notificaciones` (
  `leida` bit(1) NOT NULL,
  `fecha` datetime(6) NOT NULL,
  `id_notificacion` bigint NOT NULL AUTO_INCREMENT,
  `destinatario` varchar(120) NOT NULL,
  `titulo` varchar(150) NOT NULL,
  `enlace` varchar(300) DEFAULT NULL,
  `mensaje` varchar(1000) NOT NULL,
  PRIMARY KEY (`id_notificacion`),
  KEY `ix_notif_destinatario` (`destinatario`,`leida`)
) ENGINE=InnoDB;
CREATE TABLE `ofertas` (
  `calificacion_tecnica` int DEFAULT NULL,
  `declaracion_conflicto` bit(1) NOT NULL,
  `industria_nacional` bit(1) NOT NULL,
  `plazo_dias` int NOT NULL,
  `valor` decimal(19,2) NOT NULL,
  `fecha_presentacion` datetime(6) NOT NULL,
  `id_oferta` bigint NOT NULL AUTO_INCREMENT,
  `id_proceso` bigint NOT NULL,
  `id_proveedor` bigint NOT NULL,
  `calificado_por` varchar(120) DEFAULT NULL,
  `registrada_por` varchar(120) NOT NULL,
  `canal` enum('INTERNO','PORTAL') NOT NULL,
  PRIMARY KEY (`id_oferta`),
  UNIQUE KEY `UKix9hxo8dr328magsvm3mob8we` (`id_proceso`,`id_proveedor`),
  KEY `FKdu0qe3o0yy56gjsy7h9ly5ag7` (`id_proveedor`),
  CONSTRAINT `FKdu0qe3o0yy56gjsy7h9ly5ag7` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`),
  CONSTRAINT `FKrv633hcaxk2rcubh33kpnd6x` FOREIGN KEY (`id_proceso`) REFERENCES `procesos` (`id_proceso`)
) ENGINE=InnoDB;
CREATE TABLE `pluralidad_integrantes` (
  `porcentaje_participacion` decimal(5,2) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `id_integrante` bigint NOT NULL,
  `id_pluralidad` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKityljt6xmkl60aqdx2ayrt03j` (`id_pluralidad`,`id_integrante`),
  KEY `FKjl4sdfqfkrm798cfed9o57hpr` (`id_integrante`),
  CONSTRAINT `FKjl4sdfqfkrm798cfed9o57hpr` FOREIGN KEY (`id_integrante`) REFERENCES `proveedores` (`id_proveedor`),
  CONSTRAINT `FKlse74fy6vysjxs9ib9j25pxfg` FOREIGN KEY (`id_pluralidad`) REFERENCES `proveedores` (`id_proveedor`)
) ENGINE=InnoDB;
CREATE TABLE `proceso_invitados` (
  `id_proceso` bigint NOT NULL,
  `id_proveedor` bigint NOT NULL,
  PRIMARY KEY (`id_proceso`,`id_proveedor`),
  KEY `FK4maglfc5nrftpo5glbpok2rwt` (`id_proveedor`),
  CONSTRAINT `FK4maglfc5nrftpo5glbpok2rwt` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`),
  CONSTRAINT `FK5bdinudyd9wqcxit9ul8fg481` FOREIGN KEY (`id_proceso`) REFERENCES `procesos` (`id_proceso`)
) ENGINE=InnoDB;
CREATE TABLE `procesos` (
  `presupuesto` decimal(19,2) NOT NULL,
  `pts_desempeno` int DEFAULT NULL,
  `pts_experiencia` int DEFAULT NULL,
  `pts_industria_nacional` int DEFAULT NULL,
  `pts_plazo` int DEFAULT NULL,
  `pts_precio` int DEFAULT NULL,
  `pts_tecnica` int DEFAULT NULL,
  `req_cobertura_minima` decimal(8,2) DEFAULT NULL,
  `req_contratos_minimos` int DEFAULT NULL,
  `req_endeudamiento_maximo` decimal(5,4) DEFAULT NULL,
  `req_exigir_documentos` bit(1) DEFAULT NULL,
  `req_exigir_sarlaft` bit(1) DEFAULT NULL,
  `req_experiencia_pct` int DEFAULT NULL,
  `req_liquidez_minima` decimal(8,2) DEFAULT NULL,
  `req_patrimonio_pct` int DEFAULT NULL,
  `fecha_adjudicacion` datetime(6) DEFAULT NULL,
  `fecha_aprobacion_nivel1` datetime(6) DEFAULT NULL,
  `fecha_cierre` datetime(6) NOT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `fecha_publicacion` datetime(6) DEFAULT NULL,
  `id_nicho` bigint DEFAULT NULL,
  `id_proceso` bigint NOT NULL AUTO_INCREMENT,
  `id_proveedor_adjudicado` bigint DEFAULT NULL,
  `codigo` varchar(40) NOT NULL,
  `adjudicado_por` varchar(120) DEFAULT NULL,
  `aprobado_nivel1_por` varchar(120) DEFAULT NULL,
  `aprobado_nivel2_por` varchar(120) DEFAULT NULL,
  `creado_por` varchar(120) NOT NULL,
  `entidad_contratante` varchar(150) NOT NULL,
  `nombre` varchar(300) NOT NULL,
  `req_codigos_unspsc` varchar(300) DEFAULT NULL,
  `observacion_cierre` varchar(1000) DEFAULT NULL,
  `estado` enum('ADJUDICADO','BORRADOR','DESIERTO','EN_APROBACION','EN_EVALUACION','PUBLICADO') NOT NULL,
  `modalidad` enum('FINANCIERA','PRIVADA','PUBLICA') NOT NULL,
  `visibilidad` enum('ABIERTO','INVITACION','SECOP') NOT NULL,
  PRIMARY KEY (`id_proceso`),
  UNIQUE KEY `UKov164o0he458nvmivbc4yml9k` (`codigo`),
  KEY `FKnjhpg6qjgakorswm07vg0rixd` (`id_proveedor_adjudicado`),
  KEY `FK7ddfo6qpk7rcgpy6prdyiyusb` (`id_nicho`),
  CONSTRAINT `FK7ddfo6qpk7rcgpy6prdyiyusb` FOREIGN KEY (`id_nicho`) REFERENCES `nichos` (`id_nicho`),
  CONSTRAINT `FKnjhpg6qjgakorswm07vg0rixd` FOREIGN KEY (`id_proveedor_adjudicado`) REFERENCES `proveedores` (`id_proveedor`)
) ENGINE=InnoDB;
CREATE TABLE `proveedor_actividades` (
  `codigo_ciiu` varchar(4) NOT NULL,
  `verificada` bit(1) NOT NULL,
  `id_actividad` bigint NOT NULL AUTO_INCREMENT,
  `id_proveedor` bigint NOT NULL,
  `tipo` enum('OTRA','PRINCIPAL','SECUNDARIA') NOT NULL,
  PRIMARY KEY (`id_actividad`),
  KEY `FKi7p38h5kwa0uym7v99qkap9g5` (`id_proveedor`),
  CONSTRAINT `FKi7p38h5kwa0uym7v99qkap9g5` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`)
) ENGINE=InnoDB;
CREATE TABLE `proveedor_clasificacion` (
  `fecha_clasificacion` datetime(6) NOT NULL,
  `fecha_resolucion` datetime(6) DEFAULT NULL,
  `id_clasificacion` bigint NOT NULL AUTO_INCREMENT,
  `id_proveedor` bigint NOT NULL,
  `version_ciiu` varchar(30) NOT NULL,
  `resuelto_por` varchar(120) DEFAULT NULL,
  `justificacion` varchar(1000) DEFAULT NULL,
  `requisitos_adicionales` varchar(1000) DEFAULT NULL,
  `motivos_revision` varchar(2000) DEFAULT NULL,
  `estado` enum('AUTOMATICA','EN_REVISION','MANUAL','RECHAZADA') NOT NULL,
  `tipo_proveedor` enum('CONSORCIO','COOPERATIVA','ENTIDAD_SIN_ANIMO_DE_LUCRO','EXTRANJERA_SIN_SUCURSAL','PERSONA_JURIDICA','PERSONA_NATURAL_COMERCIANTE','PERSONA_NATURAL_NO_COMERCIANTE','UNION_TEMPORAL') NOT NULL,
  PRIMARY KEY (`id_clasificacion`),
  UNIQUE KEY `UK7ag4jl13sipdy5now81x2o253` (`id_proveedor`),
  CONSTRAINT `FKp2y0bao4f9vl98h4wg8d5qo3s` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`)
) ENGINE=InnoDB;
CREATE TABLE `proveedor_clasificacion_nichos` (
  `id_clasificacion` bigint NOT NULL,
  `id_nicho` bigint NOT NULL,
  PRIMARY KEY (`id_clasificacion`,`id_nicho`),
  KEY `FKpysxu921aqte3yrxxc7t316s3` (`id_nicho`),
  CONSTRAINT `FKpysxu921aqte3yrxxc7t316s3` FOREIGN KEY (`id_nicho`) REFERENCES `nichos` (`id_nicho`),
  CONSTRAINT `FKt2iat6netgl1esj440l1dq17j` FOREIGN KEY (`id_clasificacion`) REFERENCES `proveedor_clasificacion` (`id_clasificacion`)
) ENGINE=InnoDB;
CREATE TABLE `proveedor_consultas_sarlaft` (
  `fecha_consulta` datetime(6) NOT NULL,
  `id_consulta` bigint NOT NULL AUTO_INCREMENT,
  `id_proveedor` bigint NOT NULL,
  `documento_consultado` varchar(30) NOT NULL,
  `referencia` varchar(100) DEFAULT NULL,
  `usuario_responsable` varchar(120) NOT NULL,
  `nombre_consultado` varchar(150) NOT NULL,
  `observacion` varchar(1000) DEFAULT NULL,
  `lista` enum('CONTRALORIA','OFAC','ONU','PEP','POLICIA','PROCURADURIA') NOT NULL,
  `resultado` enum('COINCIDENCIA','POSIBLE_COINCIDENCIA','SIN_COINCIDENCIA') NOT NULL,
  PRIMARY KEY (`id_consulta`),
  KEY `FKtomy1a61utud2ikafxxvtn0x7` (`id_proveedor`),
  CONSTRAINT `FKtomy1a61utud2ikafxxvtn0x7` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`)
) ENGINE=InnoDB;
CREATE TABLE `proveedor_contratos_experiencia` (
  `contratante_publico` bit(1) NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_terminacion` date DEFAULT NULL,
  `porcentaje_participacion` decimal(5,2) NOT NULL,
  `tiene_certificacion` bit(1) NOT NULL,
  `valor` decimal(19,2) NOT NULL,
  `fecha_registro` datetime(6) NOT NULL,
  `id_contrato` bigint NOT NULL AUTO_INCREMENT,
  `id_proveedor` bigint NOT NULL,
  `estado` varchar(30) NOT NULL,
  `nit_contratante` varchar(30) DEFAULT NULL,
  `numero_contrato` varchar(60) DEFAULT NULL,
  `contratante` varchar(150) NOT NULL,
  `codigos_unspsc` varchar(500) DEFAULT NULL,
  `ruta_soporte` varchar(500) DEFAULT NULL,
  `objeto` varchar(1000) NOT NULL,
  PRIMARY KEY (`id_contrato`),
  KEY `FK7ymmjxf5ngut7k8aktf5wam6g` (`id_proveedor`),
  CONSTRAINT `FK7ymmjxf5ngut7k8aktf5wam6g` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`)
) ENGINE=InnoDB;
CREATE TABLE `proveedor_documentos` (
  `activo` bit(1) NOT NULL,
  `fecha_expedicion` date DEFAULT NULL,
  `fecha_vencimiento` date DEFAULT NULL,
  `obligatorio` bit(1) NOT NULL,
  `tiene_vencimiento` bit(1) NOT NULL,
  `validacion_automatica` bit(1) DEFAULT NULL,
  `fecha_aprobacion_nivel1` datetime(6) DEFAULT NULL,
  `fecha_aprobacion_nivel2` datetime(6) DEFAULT NULL,
  `fecha_carga` datetime(6) NOT NULL,
  `fecha_rechazo` datetime(6) DEFAULT NULL,
  `id_documento` bigint NOT NULL AUTO_INCREMENT,
  `id_proveedor` bigint NOT NULL,
  `estado_validacion` varchar(30) NOT NULL,
  `tipo_documento` varchar(100) NOT NULL,
  `aprobado_nivel1_por` varchar(120) DEFAULT NULL,
  `aprobado_nivel2_por` varchar(120) DEFAULT NULL,
  `cargado_por` varchar(120) DEFAULT NULL,
  `rechazado_por` varchar(120) DEFAULT NULL,
  `ruta_archivo` varchar(500) NOT NULL,
  `detalle_validacion` varchar(1000) DEFAULT NULL,
  `motivo_rechazo` varchar(1000) DEFAULT NULL,
  `observacion` varchar(1000) DEFAULT NULL,
  `nombre_archivo` varchar(255) NOT NULL,
  PRIMARY KEY (`id_documento`),
  KEY `FKtl2dg0dwijpwja5lias6ncfhe` (`id_proveedor`),
  CONSTRAINT `FKtl2dg0dwijpwja5lias6ncfhe` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`)
) ENGINE=InnoDB;
CREATE TABLE `proveedor_informacion_financiera` (
  `activo_corriente` decimal(19,2) NOT NULL,
  `activo_total` decimal(19,2) NOT NULL,
  `anio_corte` int NOT NULL,
  `fecha_corte` date NOT NULL,
  `gastos_intereses` decimal(19,2) DEFAULT NULL,
  `ingresos_operacionales` decimal(19,2) DEFAULT NULL,
  `pasivo_corriente` decimal(19,2) NOT NULL,
  `pasivo_total` decimal(19,2) NOT NULL,
  `patrimonio` decimal(19,2) NOT NULL,
  `utilidad_neta` decimal(19,2) DEFAULT NULL,
  `utilidad_operacional` decimal(19,2) DEFAULT NULL,
  `fecha_registro` datetime(6) NOT NULL,
  `id_info_financiera` bigint NOT NULL AUTO_INCREMENT,
  `id_proveedor` bigint NOT NULL,
  `fuente` varchar(40) NOT NULL,
  `observacion` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id_info_financiera`),
  UNIQUE KEY `uk_info_financiera_proveedor_anio` (`id_proveedor`,`anio_corte`),
  CONSTRAINT `FK3nxdowkhlu4x12b57sas3iyq5` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`)
) ENGINE=InnoDB;
CREATE TABLE `proveedores` (
  `autorizacion_datos` bit(1) NOT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `id_proveedor` bigint NOT NULL AUTO_INCREMENT,
  `estado` varchar(30) NOT NULL,
  `nit` varchar(30) NOT NULL,
  `telefono` varchar(30) DEFAULT NULL,
  `tipo_persona` varchar(30) NOT NULL,
  `tamano_empresa` varchar(50) DEFAULT NULL,
  `ciudad` varchar(80) DEFAULT NULL,
  `departamento` varchar(80) DEFAULT NULL,
  `pais` varchar(80) DEFAULT NULL,
  `correo` varchar(120) NOT NULL,
  `representante_legal` varchar(120) DEFAULT NULL,
  `sector_economico` varchar(120) DEFAULT NULL,
  `actividad_economica` varchar(150) DEFAULT NULL,
  `nombre_comercial` varchar(150) DEFAULT NULL,
  `razon_social` varchar(150) NOT NULL,
  `direccion` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id_proveedor`),
  UNIQUE KEY `UK4viyt15dd71w7i0wu9684deq7` (`nit`)
) ENGINE=InnoDB;
CREATE TABLE `rol_permisos` (
  `id_rol` bigint NOT NULL,
  `permiso` enum('AUDITORIA_VER','CATALOGOS_GESTIONAR','CLASIFICACION_RESOLVER','DOCUMENTOS_APROBAR_NIVEL_1','DOCUMENTOS_APROBAR_NIVEL_2','DOCUMENTOS_CARGAR','EVALUACIONES_ELIMINAR','EVALUACIONES_REGISTRAR','EVALUACIONES_VER','PORTAL_PROVEEDOR','PROCESOS_ADJUDICAR','PROCESOS_APROBAR_NIVEL_1','PROCESOS_APROBAR_NIVEL_2','PROCESOS_CREAR','PROCESOS_EVALUAR_TECNICO','PROVEEDORES_ELIMINAR','PROVEEDORES_GESTIONAR','PROVEEDORES_VER','ROLES_GESTIONAR','SARLAFT_CONSULTAR','USUARIOS_GESTIONAR') DEFAULT NULL,
  KEY `FKuk781na64ek3t6t4kdwdj3nk` (`id_rol`),
  CONSTRAINT `FKuk781na64ek3t6t4kdwdj3nk` FOREIGN KEY (`id_rol`) REFERENCES `roles_sistema` (`id_rol`)
) ENGINE=InnoDB;
CREATE TABLE `roles_sistema` (
  `activo` bit(1) NOT NULL,
  `limite_aprobacion` decimal(19,2) DEFAULT NULL,
  `predefinido` bit(1) NOT NULL,
  `fecha_modificacion` datetime(6) DEFAULT NULL,
  `id_rol` bigint NOT NULL AUTO_INCREMENT,
  `codigo` varchar(40) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `modificado_por` varchar(120) DEFAULT NULL,
  `descripcion` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`id_rol`),
  UNIQUE KEY `UKr8xc9ovwyv8vakcsbhuc1kt2q` (`codigo`)
) ENGINE=InnoDB AUTO_INCREMENT=10;
CREATE TABLE `usuarios` (
  `activo` bit(1) NOT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `id_proveedor` bigint DEFAULT NULL,
  `id_rol_personalizado` bigint DEFAULT NULL,
  `id_usuario` bigint NOT NULL AUTO_INCREMENT,
  `nombre_completo` varchar(100) NOT NULL,
  `correo` varchar(120) NOT NULL,
  `password` varchar(255) NOT NULL,
  `rol` varchar(30) NOT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `UKcdmw5hxlfj78uf4997i3qyyw5` (`correo`),
  KEY `FKcbia04wn1g31dr5w7rvo6jqty` (`id_rol_personalizado`),
  KEY `FK2633x1jewj6jj95ha27vhkqbh` (`id_proveedor`),
  CONSTRAINT `FK2633x1jewj6jj95ha27vhkqbh` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`),
  CONSTRAINT `FKcbia04wn1g31dr5w7rvo6jqty` FOREIGN KEY (`id_rol_personalizado`) REFERENCES `roles_sistema` (`id_rol`)
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;
