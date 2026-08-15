-- =====================================================================
-- schema.sql — Prueba Tecnica Backend Telco Fija Hogar
-- Motor: PostgreSQL 14+
-- =====================================================================

-- Limpieza (util para re-ejecutar en local durante desarrollo)
DROP TABLE IF EXISTS ventas CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;

-- =====================================================================
-- Tabla: usuarios
-- =====================================================================
CREATE TABLE usuarios (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    supervisor_id   BIGINT       NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMIN', 'AGENTE', 'BACKOFFICE', 'SUPERVISOR')),
    CONSTRAINT fk_usuarios_supervisor
        FOREIGN KEY (supervisor_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_usuarios_supervisor_id ON usuarios (supervisor_id);
CREATE INDEX idx_usuarios_rol ON usuarios (rol);

-- =====================================================================
-- Tabla: ventas
-- Guarda snapshot de los datos del cliente al momento de la venta
-- (no se exige tabla de clientes separada — ver docs/decisiones-tecnicas.md)
-- =====================================================================
CREATE TABLE ventas (
    id                  BIGSERIAL PRIMARY KEY,
    agente_id           BIGINT        NOT NULL,
    dni_cliente         VARCHAR(11)   NOT NULL,
    nombre_cliente      VARCHAR(150)  NOT NULL,
    telefono_cliente    VARCHAR(9)    NOT NULL,
    direccion_cliente   VARCHAR(255)  NOT NULL,
    plan_actual         VARCHAR(100)  NULL,
    plan_nuevo          VARCHAR(100)  NOT NULL,
    codigo_llamada      VARCHAR(50)   NOT NULL,
    producto            VARCHAR(50)   NOT NULL DEFAULT 'FIJA_HOGAR',
    monto               NUMERIC(10,2) NOT NULL,
    estado              VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo      VARCHAR(255)  NULL,
    validado_por        BIGINT        NULL,
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT now(),
    fecha_validacion    TIMESTAMP     NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT now(),

    CONSTRAINT uq_ventas_codigo_llamada UNIQUE (codigo_llamada),
    CONSTRAINT fk_ventas_agente
        FOREIGN KEY (agente_id) REFERENCES usuarios (id),
    CONSTRAINT fk_ventas_validado_por
        FOREIGN KEY (validado_por) REFERENCES usuarios (id),
    CONSTRAINT ck_ventas_estado
        CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT ck_ventas_dni
        CHECK (dni_cliente ~ '^[0-9]{8}$' OR dni_cliente ~ '^[0-9]{11}$'),
    CONSTRAINT ck_ventas_telefono
        CHECK (telefono_cliente ~ '^[0-9]{9}$'),
    CONSTRAINT ck_ventas_monto
        CHECK (monto >= 0),
    -- Si la venta esta RECHAZADA, el motivo es obligatorio
    CONSTRAINT ck_ventas_motivo_rechazo
        CHECK (estado <> 'RECHAZADA' OR motivo_rechazo IS NOT NULL)
);

CREATE INDEX idx_ventas_agente_id ON ventas (agente_id);
CREATE INDEX idx_ventas_estado ON ventas (estado);
CREATE INDEX idx_ventas_fecha_registro ON ventas (fecha_registro);
CREATE INDEX idx_ventas_agente_estado ON ventas (agente_id, estado);

-- =====================================================================
-- Trigger generico: mantiene updated_at al dia en cada UPDATE
-- =====================================================================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_ventas_updated_at
    BEFORE UPDATE ON ventas
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();