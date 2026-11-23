# PagoFlex - Microservicios

Backend de PagoFlex (DSY1105, Evaluación Sumativa 3, DUOC UC) con Spring Boot, Spring Cloud y MySQL. Sin Docker.

## Integrantes

- Maicol Rivas (GitHub: Mairivas)
- Integrante 2: _completar nombre y usuario de GitHub_

Repositorio de la app: https://github.com/Mairivas/App-PagoFlex

## Arquitectura

| Servicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| eureka-server | 8761 | - | Registro y descubrimiento de servicios |
| api-gateway | 8080 | - | Punto de entrada único, valida el JWT y reenvía `X-User-Id`, `X-User-Role` y `X-Empresa-Id` |
| auth-service | 8081 | pagoflex_auth | Login, registro, perfil, cambio y recuperación de clave, comunas, búsqueda de persona por RUT, emisión del JWT |
| compromisos-service | 8082 | pagoflex_compromisos | Compromisos de pago, empresas, resumen, registro, anulación y cobranza |
| pagos-service | 8083 | pagoflex_pagos | Pagos simulados, comprobantes, historial e indicador de cumplimiento |
| soporte-service | 8084 | pagoflex_soporte | Reportes de problemas y su gestión por los agentes |

- Java 17, Spring Boot 3.3.5, Spring Cloud 2023.0.3, Gradle (wrapper), MySQL 8.4.
- Comunicación entre servicios con OpenFeign (pagos y soporte llaman a compromisos; compromisos llama a auth). Los endpoints `/api/internal/**` no se enrutan por el Gateway.
- Cada servicio tiene su base de datos, sin claves foráneas entre bases. Las tablas se crean al iniciar y se cargan datos de ejemplo con fechas relativas a la fecha de hoy.
- Estructura de cada servicio: `controller`, `service`, `repository`, `entity`, `dto`, `exception` con `@RestControllerAdvice`, validación con Bean Validation y documentación Swagger/OpenAPI (`/swagger-ui.html` en cada servicio).
- Reglas de visibilidad por rol aplicadas en el servidor: USUARIO, AGENTE y EJECUTIVO.

## Endpoints (vía Gateway, `http://localhost:8080`)

auth-service
- `POST /api/auth/login`, `POST /api/auth/registro` (públicos)
- `POST /api/auth/recuperar-clave/solicitud`, `POST /api/auth/recuperar-clave/confirmacion`
- `GET /api/comunas` (público)
- `GET`, `PUT /api/usuarios/perfil`, `POST /api/usuarios/cambiar-clave`, `PUT /api/usuarios/preferencias-avisos`
- `GET /api/usuarios/buscar?rut=`

compromisos-service
- `GET /api/compromisos?estado=` (usuario: los suyos; ejecutivo: los de su empresa)
- `GET /api/compromisos/{id}`, `GET /api/compromisos/resumen`, `GET /api/compromisos/cobranza?mes=AAAA-MM`
- `POST /api/compromisos` (ejecutivo), `POST /api/compromisos/{id}/anular` (ejecutivo)

pagos-service
- `POST /api/pagos`, `GET /api/pagos?empresaId=&desde=&hasta=`, `GET /api/pagos/{id}`, `GET /api/pagos/cumplimiento`

soporte-service
- `POST /api/reportes` (usuario), `GET /api/reportes?estado=`, `GET /api/reportes/{id}`
- `PUT /api/reportes/{id}/tomar`, `PUT /api/reportes/{id}/resolver` (agente)

API externa usada por la app: `https://mindicador.cl/api`.

## Cómo ejecutar

Requisitos: JDK 17 y MySQL (Laragon) en el puerto 3306 con usuario `root` sin clave. Las bases se crean solas (`createDatabaseIfNotExist`).

1. `gradlew build` en esta carpeta (compila y corre las pruebas de todos los servicios).
2. Iniciar en este orden, cada uno en su terminal:
   - `java -jar eureka-server/build/libs/eureka-server.jar`
   - `java -jar auth-service/build/libs/auth-service.jar`
   - `java -jar api-gateway/build/libs/api-gateway.jar`
   - `java -jar compromisos-service/build/libs/compromisos-service.jar`
   - `java -jar pagos-service/build/libs/pagos-service.jar`
   - `java -jar soporte-service/build/libs/soporte-service.jar`
3. Panel de Eureka: http://localhost:8761

## Pruebas y cobertura

Pruebas con JUnit 5, Mockito y MockMvc. JaCoCo exige un mínimo de 80 % de cobertura de instrucciones por servicio (`gradlew jacocoTestCoverageVerification`) y genera el reporte HTML en `build/reports/jacoco/test/html`. Se excluyen de la medición `*Application`, `entity`, `dto`, `config` y `client` (clases sin lógica).

## Usuarios de prueba

Clave de todos: `PagoFlex2026!`

| Rol | Correo |
|---|---|
| Usuario final | `crojas@correo.ejemplo`, `lmunoz@correo.ejemplo` |
| Agente | `alagos@pagoflex.ejemplo` |
| Ejecutivo | `psaavedra@altosmaipo.ejemplo`, `gcarrasco@loshalcones.ejemplo`, `mleiva@plazaoriente.ejemplo`, `chenriquez@creditoandino.ejemplo` |
