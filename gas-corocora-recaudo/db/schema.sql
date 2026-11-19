-- =====================================================================
-- GAS COROCORA - Servicio de Recaudo / Tesoreria (SQLite)
-- Se ejecuta al iniciar el servicio; es idempotente (IF NOT EXISTS).
-- =====================================================================

-- Obligacion de pago: una por factura emitida por el proceso P1 (Medicion y Facturacion)
CREATE TABLE IF NOT EXISTS obligacion (
  referencia_pago     TEXT PRIMARY KEY,                 -- RP-<digitos de la factura>
  numero_factura      TEXT NOT NULL UNIQUE,             -- llave de correlacion con el motor
  numero_cuenta       TEXT NOT NULL,
  concepto            TEXT NOT NULL DEFAULT 'FACTURA_GAS',
  valor               REAL NOT NULL CHECK (valor > 0),
  saldo               REAL NOT NULL CHECK (saldo >= 0),
  fecha_limite        TEXT,
  process_instance_id TEXT,                             -- instancia de Camunda que la origino
  estado              TEXT NOT NULL DEFAULT 'PENDIENTE' -- PENDIENTE | PAGO_PARCIAL | PAGADA | AJUSTADA
                      CHECK (estado IN ('PENDIENTE', 'PAGO_PARCIAL', 'PAGADA', 'AJUSTADA')),
  creada_en           TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
  actualizada_en      TEXT
);

-- Pago recibido (portal del usuario, corresponsal, PSE...). Cada pago se notifica al motor.
CREATE TABLE IF NOT EXISTS pago (
  id                 INTEGER PRIMARY KEY AUTOINCREMENT,
  numero_comprobante TEXT NOT NULL UNIQUE,
  referencia_pago    TEXT NOT NULL REFERENCES obligacion(referencia_pago),
  valor              REAL NOT NULL CHECK (valor > 0),
  medio_pago         TEXT NOT NULL,
  notificado_motor   INTEGER NOT NULL DEFAULT 0,          -- 1 si el motor correlaciono el mensaje
  respuesta_motor    TEXT,
  registrado_en      TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- Nota credito aplicada por el subproceso "Ajuste de factura" (P2 PQRS)
CREATE TABLE IF NOT EXISTS nota_credito (
  id                  INTEGER PRIMARY KEY AUTOINCREMENT,
  numero_nota_credito TEXT NOT NULL UNIQUE,
  numero_cuenta       TEXT NOT NULL,
  numero_radicado     TEXT,
  referencia_pago     TEXT REFERENCES obligacion(referencia_pago),
  valor               REAL NOT NULL CHECK (valor >= 0),
  valor_aplicado      REAL NOT NULL DEFAULT 0,
  saldo_anterior      REAL,
  nuevo_saldo         REAL,
  registrada_en       TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- Saldo a favor de una cuenta (nota credito mayor que la deuda); se descuenta de la siguiente factura
CREATE TABLE IF NOT EXISTS saldo_a_favor (
  numero_cuenta TEXT PRIMARY KEY,
  valor         REAL NOT NULL DEFAULT 0 CHECK (valor >= 0)
);

CREATE INDEX IF NOT EXISTS idx_obligacion_cuenta ON obligacion(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_pago_referencia   ON pago(referencia_pago);
CREATE INDEX IF NOT EXISTS idx_nota_cuenta       ON nota_credito(numero_cuenta);
