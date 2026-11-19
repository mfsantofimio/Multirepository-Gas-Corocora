-- =====================================================================
-- GAS COROCORA - Datos iniciales de demostracion (idempotente: MERGE)
-- Clave de los tres suscriptores en el portal de usuarios: demo123
-- (se guarda como SHA-256 de "documento:clave").
-- =====================================================================

MERGE INTO gc_suscriptor (documento, nombre, email, telefono, direccion, municipio, clave_hash, origen)
KEY (documento) VALUES
('1122334455', 'Ana Pérez',     'ana.perez@correo.com',   '3101234567', 'Cra 33 # 15-20, La Esperanza',  'VILLAVICENCIO',
 '6163c0951c0fa94b9e592cb1af3582161909decedd0f87f4852f4eca943eb594', 'SEMILLA'),
('2233445566', 'Carlos Gómez',  'carlos.gomez@correo.com', '3209876543', 'Calle 38 # 30-12, Centro',      'VILLAVICENCIO',
 '9f0c17436d15656f3ea7b2369810d3626d6852fe88c674f0b413b84dc18b6eba', 'SEMILLA'),
('3344556677', 'Lucía Rincón',  'lucia.rincon@correo.com', '3115551234', 'Vereda Apiay, finca El Recreo', 'VILLAVICENCIO',
 '0a05791c7a37f38850ea6191488bfd97755e6a14580e5de75e04874cbea47bbc', 'SEMILLA');

MERGE INTO gc_cuenta (numero_cuenta, documento, tipo_usuario, estrato, direccion, consumo_promedio_m3, ultima_lectura, estado)
KEY (numero_cuenta) VALUES
('CTA-1001', '1122334455', 'RESIDENCIAL', 2, 'Cra 33 # 15-20, La Esperanza', 20, 1000, 'ACTIVA'),
('CTA-1002', '2233445566', 'COMERCIAL',   4, 'Calle 38 # 30-12, Centro',      85, 5400, 'ACTIVA'),
('CTA-1003', '1122334455', 'RESIDENCIAL', 2, 'Cra 33 # 15-22, La Esperanza', 15, 640,  'ACTIVA');
