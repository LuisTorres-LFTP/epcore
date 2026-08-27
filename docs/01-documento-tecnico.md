# Documento técnico - EPCORE

## 1. Identificación

- Proyecto: EPCORE
- Tecnología principal: Java / Spring Boot
- Persistencia: Spring Data JPA / Hibernate
- Base de datos: MySQL
- Interfaz: Thymeleaf
- Seguridad: Spring Security + BCrypt + JWT para API
- Puerto: 8080

## 2. Propósito

Gestionar proveedores, criterios y evaluaciones de proveedores, incluyendo usuarios, roles y control de acceso.

## 3. Arquitectura

```text
Usuario
  ↓
Thymeleaf / REST API
  ↓
Controllers
  ↓
Services
  ↓
Repositories (JPA)
  ↓
MySQL
```

La autenticación web se realiza mediante Spring Security. La API ofrece `/api/auth/login` para generar un JWT y proteger las rutas `/api/**`.

## 4. Módulos

- Autenticación y autorización.
- Usuarios.
- Proveedores.
- Criterios.
- Evaluaciones.
- Persistencia MySQL.

## 5. Seguridad

- Contraseñas almacenadas con BCrypt.
- Usuarios inactivos no pueden autenticarse.
- Autorización por roles.
- JWT firmado para consumo protegido de la API.
- Secretos y credenciales mediante variables de entorno.

## 6. Ejecución

1. Crear `.env` a partir de `.env.example`.
2. Configurar conexión MySQL.
3. Ejecutar `./mvnw spring-boot:run` o `mvnw.cmd spring-boot:run`.
4. Abrir `http://localhost:8080`.

## 7. Pruebas

Las pruebas manuales deben ejecutarse con la colección `postman/EPCORE-SENA.postman_collection.json` y documentarse en el acta de pruebas.
