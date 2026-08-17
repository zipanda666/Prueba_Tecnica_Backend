# Decisiones técnicas y guía de despliegue local

## 1. Motor de base de datos: PostgreSQL

Se eligió PostgreSQL sobre MySQL por tres razones concretas para este proyecto:

- Soporte nativo y confiable de `CHECK` constraints, usados extensivamente para blindar reglas de
  negocio a nivel de esquema (formato de DNI/teléfono, transiciones de estado, motivo de rechazo
  obligatorio), no solo en el código Java.
- Con Spring Data JPA / Hibernate el cambio de motor es transparente (driver + dialect), así que
  no hay costo real de usar Postgres en vez de MySQL.
- Se integra de forma directa con Docker Compose, lo que simplifica el entregable de "correr en
  Windows/Linux con un solo comando".

## 2. Modelo de datos: normalización y una desnormalización intencional

Las tablas `usuarios` y `ventas` cumplen 1FN/2FN/3FN de forma directa (PK simple, sin campos
repetidos ni dependencias parciales). La única excepción intencional: `ventas` guarda un
**snapshot** de los datos del cliente (`nombre_cliente`, `telefono_cliente`, `direccion_cliente`)
en vez de referenciar una tabla `clientes` normalizada. Esto es deliberado, no un descuido: si el
cliente cambia de dirección después, la venta histórica debe conservar los datos que tenía *al
momento de la venta*, por motivos de auditoría. El enunciado además exime explícitamente de crear
tabla de clientes.

`usuarios.supervisor_id` es una auto-referencia (FK de `usuarios` a sí misma), lo que modela la
jerarquía supervisor→agentes sin necesitar una tabla de relación aparte.

## 3. Arquitectura del backend: capas MVC + Service

```
Controller (HTTP)  →  Service (reglas de negocio)  →  Repository (JPA)  →  Entity
        ↑
      DTO (request/response) - nunca se expone la entidad JPA directo
```

- Los DTOs evitan filtrar campos sensibles (`password_hash`) o problemas de serialización con
  relaciones lazy de Hibernate.
- Los filtros dinámicos de `/ventas/mis-ventas`, `/ventas/equipo` y compañía usan
  **Spring Data Specifications** en vez de un método de repositorio por combinación de filtros
  (evita explosión combinatoria de queries).
- Las reglas de transición de estado (solo se puede aprobar/rechazar una venta PENDIENTE, rechazar
  exige motivo) viven en el `Service`, no en anotaciones de validación de la entidad, porque son
  lógica de negocio dependiente de estado, no validación de formato de un campo aislado.

## 4. Seguridad: JWT stateless

Autenticación con JWT (HMAC-SHA, `jjwt`), sesiones **stateless** (`SessionCreationPolicy.STATELESS`).
El rol del usuario se codifica como claim y se traduce a `ROLE_<ROL>` como `GrantedAuthority` de
Spring Security, así los endpoints se protegen declarativamente con
`@PreAuthorize("hasRole('BACKOFFICE')")` en vez de chequear el rol a mano en cada método.

El secreto de firma (`app.jwt.secret`) vive en `application.yml` **solo para desarrollo local**;
en un entorno real debe inyectarse por variable de entorno y nunca commitearse al repositorio.

## 5. Manejo de errores

Un `@RestControllerAdvice` centraliza todas las excepciones (`ResourceNotFoundException` → 404,
`BusinessException` → 409, `BadCredentialsException` → 401, `AccessDeniedException` → 403, errores
de `@Valid` → 400 con detalle por campo) y las traduce siempre al mismo formato JSON:
`{ timestamp, path, error, message, details? }`. Ninguna excepción no controlada llega al cliente
como stack trace crudo.

## 6. Auditoría

Se distingue entre dos tipos de trazabilidad:

- **Estructurada (BD)**: `ventas.validado_por` + `fecha_validacion` registran qué usuario de
  BACKOFFICE resolvió cada venta y cuándo — consultable con SQL normal.
- **De eventos (logs)**: un logger `AUDIT.VENTAS` y otro `AUDIT.AUTH` (Logback, configurados en
  `logback-spring.xml`) escriben en archivos separados (`logs/audit/ventas.log`,
  `logs/audit/auth.log`) cada acción sensible — crear/aprobar/rechazar venta, consultas del
  supervisor, intentos de login — con usuario, rol y detalle de la acción. Se separan de
  `logs/app/app.log` (arranque, errores de framework) para que revisar auditoría no implique
  filtrar ruido técnico.

## 7. Guía de despliegue local (resumen)

Ver `README.md` para el detalle completo. En síntesis:

1. `docker compose up -d` (raíz del repo) → levanta Postgres con schema y seed ya cargados.
2. `cd backend && mvn spring-boot:run` → API en `http://localhost:8080/api/v1`.
3. `cd frontend && npm install && npm run dev` → UI en `http://localhost:5173`.
4. Login con cualquiera de los 5 usuarios seed (ver tabla en `README.md`) para obtener el JWT.

## 8. Limitaciones conocidas / fuera de alcance

- No hay refresh tokens: el JWT expira a las 24h y el usuario debe volver a loguearse.
- No hay rate limiting ni protección contra fuerza bruta en `/auth/login` (quedaría en un filtro
  adicional o un API Gateway en un entorno real).
- El rol `ADMIN` está definido en el modelo pero sin endpoints propios en este alcance — se
  incluyó porque el enunciado lo pide en el enum de roles, pero no hay CRUD de usuarios expuesto.
