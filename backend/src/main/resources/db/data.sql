-- =====================================================================
-- data.sql — Datos seed
-- Contrasenas en texto plano (solo referencia, NO se guardan asi):
--   admin        / Admin*123
--   agente1      / Agente*123   (bajo supervisor1)
--   back1        / Back*123
--   supervisor1  / Sup*123
-- Hashes generados con bcrypt (BCryptPasswordEncoder, strength 10)
-- =====================================================================

-- Orden: primero supervisor1 (sin jefe), luego el resto referenciandolo
INSERT INTO usuarios (username, password_hash, rol, supervisor_id, activo) VALUES
('supervisor1', '$2b$10$CFU9JoHCJO2omQ7lUVQy3OH4oEfusg6IYEiKW67oQHM8dIbxAZXPq', 'SUPERVISOR', NULL, TRUE),
('admin',       '$2b$10$F6ypoQorVZVnrXa/VWKrROYe7QEVinh8TYNV9Flv3vrVB0uSuuDDa', 'ADMIN',      NULL, TRUE),
('back1',       '$2b$10$WFS/8UC1sZ2n8RrWQOF1iejm.X3Pg7z1EVKRzci5svJ4GpIQdicGu', 'BACKOFFICE', NULL, TRUE);

INSERT INTO usuarios (username, password_hash, rol, supervisor_id, activo)
SELECT 'agente1', '$2b$10$sAA/IzifgtVPqL6h/RYC1.4L0i/WImx9.tetcdop3a2HfTDdpInf6', 'AGENTE', id, TRUE
FROM usuarios WHERE username = 'supervisor1';

-- Un segundo agente bajo el mismo supervisor, util para probar filtros/paginacion
INSERT INTO usuarios (username, password_hash, rol, supervisor_id, activo)
SELECT 'agente2', '$2b$10$sAA/IzifgtVPqL6h/RYC1.4L0i/WImx9.tetcdop3a2HfTDdpInf6', 'AGENTE', id, TRUE
FROM usuarios WHERE username = 'supervisor1';

-- =====================================================================
-- Ventas variadas (PENDIENTE / APROBADA / RECHAZADA), distintas fechas
-- =====================================================================

INSERT INTO ventas (
    agente_id, dni_cliente, nombre_cliente, telefono_cliente, direccion_cliente,
    plan_actual, plan_nuevo, codigo_llamada, producto, monto,
    estado, motivo_rechazo, validado_por, fecha_registro, fecha_validacion
)
SELECT
    a.id, '45678912', 'Maria Fernandez Rojas', '987654321', 'Av. Los Olivos 123, Lima',
    'Internet 100MB', 'Internet 200MB + TV', 'CALL-0001', 'FIJA_HOGAR', 129.90,
    'PENDIENTE', NULL, NULL, now() - INTERVAL '1 day', NULL
FROM usuarios a WHERE a.username = 'agente1';

INSERT INTO ventas (
    agente_id, dni_cliente, nombre_cliente, telefono_cliente, direccion_cliente,
    plan_actual, plan_nuevo, codigo_llamada, producto, monto,
    estado, motivo_rechazo, validado_por, fecha_registro, fecha_validacion
)
SELECT
    a.id, '78912345', 'Jorge Luis Castillo Vega', '956123456', 'Jr. Las Palmeras 456, San Martin de Porres',
    'Sin plan previo', 'Internet 100MB + Telefonia', 'CALL-0002', 'FIJA_HOGAR', 99.90,
    'APROBADA', NULL, b.id, now() - INTERVAL '6 day', now() - INTERVAL '5 day'
FROM usuarios a, usuarios b
WHERE a.username = 'agente1' AND b.username = 'back1';

INSERT INTO ventas (
    agente_id, dni_cliente, nombre_cliente, telefono_cliente, direccion_cliente,
    plan_actual, plan_nuevo, codigo_llamada, producto, monto,
    estado, motivo_rechazo, validado_por, fecha_registro, fecha_validacion
)
SELECT
    a.id, '32165498', 'Rosa Elvira Quispe Mamani', '945678123', 'Calle Union 789, Comas',
    'Internet 50MB', 'Internet 150MB', 'CALL-0003', 'FIJA_HOGAR', 89.90,
    'RECHAZADA', 'Cliente reporta domicilio fuera de zona de cobertura', b.id, now() - INTERVAL '4 day', now() - INTERVAL '3 day'
FROM usuarios a, usuarios b
WHERE a.username = 'agente1' AND b.username = 'back1';

INSERT INTO ventas (
    agente_id, dni_cliente, nombre_cliente, telefono_cliente, direccion_cliente,
    plan_actual, plan_nuevo, codigo_llamada, producto, monto,
    estado, motivo_rechazo, validado_por, fecha_registro, fecha_validacion
)
SELECT
    a.id, '65498732', 'Carlos Alberto Sanchez Diaz', '978912345', 'Av. Universitaria 1010, Los Olivos',
    'Sin plan previo', 'Internet 300MB + TV Premium', 'CALL-0004', 'FIJA_HOGAR', 159.90,
    'PENDIENTE', NULL, NULL, now() - INTERVAL '2 day', NULL
FROM usuarios a WHERE a.username = 'agente2';

INSERT INTO ventas (
    agente_id, dni_cliente, nombre_cliente, telefono_cliente, direccion_cliente,
    plan_actual, plan_nuevo, codigo_llamada, producto, monto,
    estado, motivo_rechazo, validado_por, fecha_registro, fecha_validacion
)
SELECT
    a.id, '15975348', 'Ana Lucia Torres Huaman', '923456789', 'Jr. Amazonas 234, Independencia',
    'Internet 100MB', 'Internet 200MB', 'CALL-0005', 'FIJA_HOGAR', 119.90,
    'APROBADA', NULL, b.id, now() - INTERVAL '10 day', now() - INTERVAL '9 day'
FROM usuarios a, usuarios b
WHERE a.username = 'agente2' AND b.username = 'back1';

INSERT INTO ventas (
    agente_id, dni_cliente, nombre_cliente, telefono_cliente, direccion_cliente,
    plan_actual, plan_nuevo, codigo_llamada, producto, monto,
    estado, motivo_rechazo, validado_por, fecha_registro, fecha_validacion
)
SELECT
    a.id, '75318642', 'Miguel Angel Ramos Flores', '934567891', 'Av. Naranjal 555, San Martin de Porres',
    'Internet 150MB', 'Internet 300MB + Telefonia', 'CALL-0006', 'FIJA_HOGAR', 149.90,
    'RECHAZADA', 'DNI del titular no coincide con la direccion registrada', b.id, now() - INTERVAL '8 day', now() - INTERVAL '7 day'
FROM usuarios a, usuarios b
WHERE a.username = 'agente1' AND b.username = 'back1';

INSERT INTO ventas (
    agente_id, dni_cliente, nombre_cliente, telefono_cliente, direccion_cliente,
    plan_actual, plan_nuevo, codigo_llamada, producto, monto,
    estado, motivo_rechazo, validado_por, fecha_registro, fecha_validacion
)
SELECT
    a.id, '85274196', 'Patricia Isabel Leon Garcia', '912345678', 'Calle Las Begonias 321, Comas',
    'Sin plan previo', 'Internet 100MB + TV', 'CALL-0007', 'FIJA_HOGAR', 109.90,
    'PENDIENTE', NULL, NULL, now(), NULL
FROM usuarios a WHERE a.username = 'agente1';

INSERT INTO ventas (
    agente_id, dni_cliente, nombre_cliente, telefono_cliente, direccion_cliente,
    plan_actual, plan_nuevo, codigo_llamada, producto, monto,
    estado, motivo_rechazo, validado_por, fecha_registro, fecha_validacion
)
SELECT
    a.id, '96385274', 'Luis Fernando Vargas Cruz', '967891234', 'Av. Tupac Amaru 890, Independencia',
    'Internet 200MB', 'Internet 400MB + TV Premium', 'CALL-0008', 'FIJA_HOGAR', 189.90,
    'APROBADA', NULL, b.id, now() - INTERVAL '15 day', now() - INTERVAL '14 day'
FROM usuarios a, usuarios b
WHERE a.username = 'agente2' AND b.username = 'back1';