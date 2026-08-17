
## Decisiones técnicas

- **PostgreSQL** sobre MySQL: soporte nativo de `CHECK` constraints para blindar reglas de negocio a nivel de esquema.
- **Snapshot de cliente en `ventas`** (no hay tabla `clientes`): intencional, para conservar los datos del cliente tal como eran al momento de la venta. El enunciado exime de crear esa tabla.
- **Arquitectura en capas**: `Controller → Service → Repository → Entity`, con DTOs (nunca se expone la entidad JPA). Reglas de transición de estado viven en el Service.
- **JWT stateless**, rol como `GrantedAuthority` (`@PreAuthorize` por endpoint). El filtro valida también que el usuario siga **activo**, no solo el token.
- **Errores uniformes**: `{ timestamp, path, error, message }` en toda la API vía `@RestControllerAdvice`.
- **Auditoría**: `validado_por`/`fecha_validacion` en BD + logs separados por dominio (`logs/audit/`).
- **Frontend** escapa todo dato del backend antes de insertarlo en el DOM (previene XSS).

---

## Ejecución local

```bash
docker compose up --build
```

Levanta BD + backend + frontend con un solo comando. Abrir: **http://localhost:5173**

Si editas `schema.sql`/`data.sql`, resetea con `docker compose down -v` antes de volver a levantar.

**Alternativa (desarrollo iterativo):**
```bash
docker compose up -d db
cd backend && mvn spring-boot:run
cd frontend && python -m http.server 5173
```

### Usuarios seed

| Username | Password | Rol |
|---|---|---|
| admin | Admin*123 | ADMIN |
| supervisor1 | Sup*123 | SUPERVISOR |
| agente1 | Agente*123 | AGENTE (bajo supervisor1) |
| agente2 | Agente*123 | AGENTE (bajo supervisor1) |
| back1 | Back*123 | BACKOFFICE |

### Puertos

| Servicio | Puerto |
|---|---|
| Frontend | 5173 |
| Backend (`/api/v1`, Swagger en `/swagger-ui.html`) | 8080 |
| PostgreSQL | 5433 |