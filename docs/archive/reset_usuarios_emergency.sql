-- Script de emergencia para resetear usuarios y crear admin funcional
-- ADVERTENCIA: Esto BORRARÁ todos los usuarios existentes

-- Paso 1: Eliminar todas las suscripciones (por foreign key)
DELETE FROM suscripciones;

-- Paso 2: Eliminar todos los perfiles (por foreign key)
DELETE FROM perfiles;

-- Paso 3: Eliminar todas las facturas (por foreign key)
DELETE FROM facturas;

-- Paso 4: Eliminar todos los usuarios
DELETE FROM usuarios;

-- Paso 5: Crear usuario ADMIN desde cero
INSERT INTO usuarios (email, pais, password, rol, fecha_alta)
VALUES ('admin@saas.com', 'ES', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', NOW());

-- Paso 6: Crear usuario DEMO desde cero
INSERT INTO usuarios (email, pais, password, rol, fecha_alta)
VALUES ('demo@saas.local', 'ES', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', NOW());

-- Verificar
SELECT email, rol, 
       substring(password, 1, 20) as password_inicio,
       fecha_alta
FROM usuarios
ORDER BY rol DESC;

-- IMPORTANTE: Después de ejecutar esto, reinicia la aplicación Spring Boot
-- El DataInitializer creará las suscripciones y perfiles necesarios
