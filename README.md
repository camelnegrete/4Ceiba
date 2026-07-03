# Medisalud API

API REST para un **sistema de agendamiento de citas médicas** (gestión de médicos y pacientes, reserva, consulta de disponibilidad, cancelación con penalización y reprogramación).

---

> #  IMPORTANTE — CÓMO PROBAR 
>
> ## ➡ USA LA COLECCIÓN DE POSTMAN INCLUIDA PARA HACER LAS PRUEBAS
>
> ###  `postman/Medisalud-API.postman_collection.json`
>
> **1.** Levanta el proyecto con **`docker compose up --build`**.
> **2.** En Postman: **Import** → selecciona ese archivo.
> **3.** Ejecuta la colección (o con el **Collection Runner**): carpetas **Médicos → Pacientes → Citas → Validar errores**.
>
> La colección ya trae los flujos felices **y** una carpeta **“Validar errores”** con casos que provocan `400 / 404 / 409` a propósito. **No es necesario armar peticiones a mano.**

---

## Prueba técnica — Ceiba

Este proyecto es una **prueba técnica para Ceiba**, desarrollada por el postulante:

- **Nombre:** Camel Negrete
- **Correo:** camelnegrete@gmail.com
- **Cargo al que aplica:** Desarrollador

## Stack

Java 21 · Spring Boot 3.5 · Gradle · PostgreSQL 16 · Flyway · Spring Data JPA ·
Lombok · MapStruct · Bean Validation · OpenAPI (Swagger) · JUnit 5 · Mockito ·
Testcontainers · Docker / Docker Compose.

## Arquitectura (resumen)

Clean Architecture por capas con dependencias hacia el dominio:

```
com.camel.medisalud
├── config/              Configuración (OpenAPI, JPA Auditing, propiedades de agenda)
├── controller/          Capa web (REST) + advice/ (GlobalExceptionHandler)
├── dto/ request|response DTOs (nunca se exponen entidades JPA)
├── service/ interfaces|impl   Capa de servicios (inyección por constructor)
│         └── scheduling/       Reglas de agenda (horarios, validaciones)
├── repository/          Repository Pattern (+ JpaSpecificationExecutor)
├── domain/ model|enums|exception|validation   Núcleo del dominio
├── mapper/              MapStruct
└── specification/       Specification Pattern (filtros dinámicos)
```

El esquema de base de datos lo gobierna **Flyway** (`src/main/resources/db/migration`)
y Hibernate corre en modo `validate`.

## Cómo ejecutar (recomendado: Docker Compose)

Todo el stack (PostgreSQL + aplicación) se levanta con un solo comando:

```bash
docker compose up --build
```

- Reconstruye la imagen la primera vez o al cambiar código; luego basta `docker compose up`.
- En segundo plano: `docker compose up -d --build`
- Ver logs: `docker compose logs -f app`
- Detener: `docker compose down` (o `docker compose down -v` para borrar también los datos).

Una vez arriba:

- API → http://localhost:8080
- Swagger UI → http://localhost:8080/swagger-ui.html
- OpenAPI (JSON) → http://localhost:8080/v3/api-docs
- Health → http://localhost:8080/actuator/health

> Nota: la ruta raíz `/` no tiene endpoint; los servicios viven bajo `/api/v1/...`.

### Puerto de la base de datos

Las variables están en `.env`. El PostgreSQL del contenedor se publica en el host
en el puerto **`55432`** (`DB_PORT=55432`) para no chocar con un PostgreSQL local
que ya use el `5432`. Internamente la app siempre habla con la BD por `postgres:5432`
dentro de la red de Docker, así que este valor solo afecta el acceso desde el host
(por ejemplo con DBeaver/psql en `localhost:55432`, usuario/clave `medisalud`).

Si tu host tiene libre el `5432` y prefieres el puerto estándar, puedes poner
`DB_PORT=5432` en `.env`.

## Cómo probar (colección de Postman)

El proyecto incluye una colección lista para importar:

```
postman/Medisalud-API.postman_collection.json
```

Pasos:

1. En Postman: **Import** → selecciona el archivo anterior.
2. Se creará la colección **“Medisalud API”** con las carpetas **Médicos**,
   **Pacientes**, **Citas**, **Validar errores** y **Documentación y Salud**.
3. Con la aplicación levantada (Docker Compose), ejecuta en orden — o usa el
   **Collection Runner** para correr todo de una:
   1. **Médicos → Crear médico**
   2. **Pacientes → Crear paciente**
   3. **Citas → Reservar cita**
   4. **Citas → Consultar disponibilidad / Listar citas**
   5. **Citas → Reprogramar cita / Cancelar cita**
   6. **Validar errores → ...**

La colección guarda automáticamente los `id` creados y calcula fechas de cita
válidas (próximo día hábil en horario laboral), de modo que el flujo completo es
ejecutable sin editar nada. La variable `baseUrl` apunta a `http://localhost:8080`.

### Carpeta “Validar errores”

Contiene peticiones que provocan errores **a propósito** para verificar el manejo
de excepciones y las reglas de negocio:

- Doble reserva para el mismo médico en la misma hora → **409**
- Reservar en domingo → **400**
- Reservar fuera del horario laboral → **400**
- Crear paciente con documento repetido → **409**
- Consultar un UUID inexistente → **404**
- Reservar con un paciente penalizado (RN-05) → **409**

> El caso del **paciente penalizado** está en el subgrupo *“Paciente penalizado
> (requiere horario laboral)”*: como una penalización solo se genera al cancelar
> de forma tardía (menos de 2 h antes), ese subgrupo reserva y cancela 3 citas de
> **hoy** para acumular 3 penalizaciones y luego intenta una cuarta reserva (que
> debe dar 409). Por eso **solo funciona si se ejecuta en horario laboral
> colombiano** (Lun–Vie 08:00–16:00 o Sáb 08:00–11:00). El resto de casos de error
> funcionan a cualquier hora.

## Reglas de negocio implementadas

- **Horario laboral:** Lunes a Viernes 08:00–18:00, Sábado 08:00–13:00, Domingo cerrado; citas de 30 minutos (zona horaria América/Bogotá).
- **No duplicidad del médico:** un médico no puede tener dos citas en la misma franja.
- **Fecha de nacimiento:** no se permite fecha futura (nula se asume válida).
- **Conflicto del paciente:** un paciente no puede tener otra cita con el mismo médico en la misma fecha y hora.
- **Cancelación tardía:** cancelar con menos de 2 horas genera una penalización asociada al paciente y a la cita.
- **Bloqueo por penalizaciones:** un paciente con 3 o más penalizaciones en los últimos 30 días no puede reservar.
- **Reprogramación:** cancela la cita anterior (penaliza si aplica) y crea una nueva revalidando todas las reglas, de forma transaccional.

## Pruebas y build

```bash
./gradlew clean build     # compila y ejecuta todas las pruebas (requiere Docker para Testcontainers)
./gradlew test            # solo pruebas
```

Las pruebas de integración usan **Testcontainers** (PostgreSQL efímero), por lo que
requieren un Docker en ejecución. El reporte de cobertura JaCoCo queda en
`build/reports/jacoco/test/html/index.html`.
