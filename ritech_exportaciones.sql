-- ============================================================
--  BASE DE DATOS: ritech_exportaciones
--  Sistema de gestion de exportaciones de cafe y cacao
--  RiTech S.A.S  -  Universidad Cooperativa de Colombia
--  Motor: MySQL 8.x
-- ============================================================

DROP DATABASE IF EXISTS ritech_exportaciones;

CREATE DATABASE ritech_exportaciones
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_spanish_ci;

USE ritech_exportaciones;

-- ------------------------------------------------------------
-- 1. ROL
-- ------------------------------------------------------------
CREATE TABLE rol (
  id_rol      INT AUTO_INCREMENT PRIMARY KEY,
  nombre      VARCHAR(50)  NOT NULL UNIQUE,
  descripcion VARCHAR(150)
);

-- ------------------------------------------------------------
-- 2. USUARIO
-- ------------------------------------------------------------
CREATE TABLE usuario (
  id_usuario      INT AUTO_INCREMENT PRIMARY KEY,
  nombre          VARCHAR(80)  NOT NULL,
  correo          VARCHAR(120) NOT NULL UNIQUE,
  contrasena_hash VARCHAR(255) NOT NULL,
  id_rol          INT          NOT NULL,
  activo          BOOLEAN      NOT NULL DEFAULT TRUE,
  fecha_creacion  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
);

-- ------------------------------------------------------------
-- 3. PROVEEDOR
-- ------------------------------------------------------------
CREATE TABLE proveedor (
  id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
  documento    VARCHAR(30)  NOT NULL UNIQUE,
  nombre       VARCHAR(100) NOT NULL,
  telefono     VARCHAR(30),
  correo       VARCHAR(120)
);

-- ------------------------------------------------------------
-- 4. FINCA
-- ------------------------------------------------------------
CREATE TABLE finca (
  id_finca     INT AUTO_INCREMENT PRIMARY KEY,
  id_proveedor INT NOT NULL,
  nombre       VARCHAR(100)  NOT NULL,
  lugar        VARCHAR(120),
  latitud      DECIMAL(10,6) NOT NULL,
  longitud     DECIMAL(10,6) NOT NULL,
  CONSTRAINT fk_finca_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor)
);

-- ------------------------------------------------------------
-- 5. PRODUCTO
-- ------------------------------------------------------------
CREATE TABLE producto (
  id_producto INT AUTO_INCREMENT PRIMARY KEY,
  nombre      VARCHAR(50) NOT NULL UNIQUE
);

-- ------------------------------------------------------------
-- 6. VARIEDAD
-- ------------------------------------------------------------
CREATE TABLE variedad (
  id_variedad INT AUTO_INCREMENT PRIMARY KEY,
  id_producto INT NOT NULL,
  nombre      VARCHAR(60) NOT NULL,
  categorias  VARCHAR(100),
  CONSTRAINT fk_variedad_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);

-- ------------------------------------------------------------
-- 7. LOTE
-- ------------------------------------------------------------
CREATE TABLE lote (
  id_lote        INT AUTO_INCREMENT PRIMARY KEY,
  codigo         VARCHAR(30)   NOT NULL UNIQUE,
  id_variedad    INT           NOT NULL,
  id_finca       INT           NOT NULL,
  peso_kg        DECIMAL(10,2) NOT NULL,
  fecha_cosecha  DATE          NOT NULL,
  estado         VARCHAR(30)   NOT NULL DEFAULT 'DISPONIBLE',
  fecha_registro DATETIME      DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_lote_variedad FOREIGN KEY (id_variedad) REFERENCES variedad(id_variedad),
  CONSTRAINT fk_lote_finca    FOREIGN KEY (id_finca)    REFERENCES finca(id_finca),
  CONSTRAINT ck_lote_peso     CHECK (peso_kg > 0)
);

-- ------------------------------------------------------------
-- 8. PAIS
-- ------------------------------------------------------------
CREATE TABLE pais (
  id_pais        INT AUTO_INCREMENT PRIMARY KEY,
  nombre         VARCHAR(80) NOT NULL UNIQUE,
  codigo_iso     CHAR(2)     NOT NULL UNIQUE,
  es_union_europea BOOLEAN   NOT NULL DEFAULT TRUE
);

-- ------------------------------------------------------------
-- 9. MONEDA
-- ------------------------------------------------------------
CREATE TABLE moneda (
  id_moneda  INT AUTO_INCREMENT PRIMARY KEY,
  codigo_iso CHAR(3)     NOT NULL UNIQUE,
  nombre     VARCHAR(50) NOT NULL
);

-- ------------------------------------------------------------
-- 10. CLIENTE
-- ------------------------------------------------------------
CREATE TABLE cliente (
  id_cliente         INT AUTO_INCREMENT PRIMARY KEY,
  nombre_empresa     VARCHAR(120) NOT NULL,
  id_pais            INT          NOT NULL,
  ciudad             VARCHAR(80),
  persona_contacto   VARCHAR(100),
  correo             VARCHAR(120),
  id_moneda_habitual INT,
  CONSTRAINT fk_cliente_pais   FOREIGN KEY (id_pais)            REFERENCES pais(id_pais),
  CONSTRAINT fk_cliente_moneda FOREIGN KEY (id_moneda_habitual) REFERENCES moneda(id_moneda)
);

-- ------------------------------------------------------------
-- 11. PEDIDO
-- ------------------------------------------------------------
CREATE TABLE pedido (
  id_pedido      INT AUTO_INCREMENT PRIMARY KEY,
  numero         VARCHAR(30)   NOT NULL UNIQUE,
  id_cliente     INT           NOT NULL,
  id_pais_destino INT          NOT NULL,
  id_moneda      INT           NOT NULL,
  tasa_cambio    DECIMAL(14,4) NOT NULL,
  fecha_pedido   DATE          NOT NULL,
  estado         VARCHAR(30)   NOT NULL DEFAULT 'CREADO',
  total          DECIMAL(14,2) NOT NULL DEFAULT 0,
  id_usuario     INT           NOT NULL,
  CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente)      REFERENCES cliente(id_cliente),
  CONSTRAINT fk_pedido_pais    FOREIGN KEY (id_pais_destino) REFERENCES pais(id_pais),
  CONSTRAINT fk_pedido_moneda  FOREIGN KEY (id_moneda)       REFERENCES moneda(id_moneda),
  CONSTRAINT fk_pedido_usuario FOREIGN KEY (id_usuario)      REFERENCES usuario(id_usuario)
);

-- ------------------------------------------------------------
-- 12. PEDIDO_LOTE  (tabla intermedia)
-- ------------------------------------------------------------
CREATE TABLE pedido_lote (
  id_pedido_lote  INT AUTO_INCREMENT PRIMARY KEY,
  id_pedido       INT           NOT NULL,
  id_lote         INT           NOT NULL,
  precio_unitario DECIMAL(14,2) NOT NULL,
  total_parcial   DECIMAL(14,2) NOT NULL,
  CONSTRAINT fk_pedlote_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido),
  CONSTRAINT fk_pedlote_lote   FOREIGN KEY (id_lote)   REFERENCES lote(id_lote),
  CONSTRAINT uq_pedido_lote    UNIQUE (id_pedido, id_lote)
);

-- ------------------------------------------------------------
-- 13. DOCUMENTO_EXPORTACION
-- ------------------------------------------------------------
CREATE TABLE documento_exportacion (
  id_documento   INT AUTO_INCREMENT PRIMARY KEY,
  id_pedido      INT          NOT NULL,
  tipo           VARCHAR(80)  NOT NULL,
  nombre_archivo VARCHAR(200),
  fecha_carga    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_documento_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido)
);

-- ============================================================
--  DATOS DE PRUEBA
-- ============================================================

INSERT INTO rol (id_rol, nombre, descripcion) VALUES
  (1, 'ADMINISTRADOR', 'Administra usuarios y genera reportes'),
  (2, 'AUXILIAR',      'Opera proveedores, lotes y pedidos');

-- Contrasenas cifradas con SHA-256:
--   admin@ritech.com     ->  admin123
--   auxiliar@ritech.com  ->  1234
INSERT INTO usuario (nombre, correo, contrasena_hash, id_rol, activo) VALUES
  ('Administrador RiTech', 'admin@ritech.com',
   '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 1, TRUE),
  ('Auxiliar de exportaciones', 'auxiliar@ritech.com',
   '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 2, TRUE);

INSERT INTO producto (id_producto, nombre) VALUES (1, 'Cafe'), (2, 'Cacao');

INSERT INTO variedad (id_variedad, id_producto, nombre, categorias) VALUES
  (1, 1, 'Caturra',    'Excelso'),
  (2, 1, 'Castillo',   'Excelso'),
  (3, 1, 'Typica',     'Supremo'),
  (4, 1, 'Borbon',     'Supremo'),
  (5, 2, 'Criollo',    'Fino de aroma'),
  (6, 2, 'Forastero',  'Corriente'),
  (7, 2, 'Trinitario', 'Fino de aroma');

INSERT INTO moneda (id_moneda, codigo_iso, nombre) VALUES
  (1, 'COP', 'Peso colombiano'),
  (2, 'USD', 'Dolar estadounidense'),
  (3, 'EUR', 'Euro');

-- Los 27 estados miembros de la Union Europea
INSERT INTO pais (nombre, codigo_iso, es_union_europea) VALUES
  ('Alemania','DE',TRUE), ('Austria','AT',TRUE), ('Belgica','BE',TRUE),
  ('Bulgaria','BG',TRUE), ('Chipre','CY',TRUE), ('Croacia','HR',TRUE),
  ('Dinamarca','DK',TRUE), ('Eslovaquia','SK',TRUE), ('Eslovenia','SI',TRUE),
  ('Espana','ES',TRUE), ('Estonia','EE',TRUE), ('Finlandia','FI',TRUE),
  ('Francia','FR',TRUE), ('Grecia','GR',TRUE), ('Hungria','HU',TRUE),
  ('Irlanda','IE',TRUE), ('Italia','IT',TRUE), ('Letonia','LV',TRUE),
  ('Lituania','LT',TRUE), ('Luxemburgo','LU',TRUE), ('Malta','MT',TRUE),
  ('Paises Bajos','NL',TRUE), ('Polonia','PL',TRUE), ('Portugal','PT',TRUE),
  ('Republica Checa','CZ',TRUE), ('Rumania','RO',TRUE), ('Suecia','SE',TRUE);

-- Proveedores de ejemplo con sus fincas (coordenadas reales de Narino)
INSERT INTO proveedor (id_proveedor, documento, nombre, telefono, correo) VALUES
  (1, '1085123456', 'Jose Manuel Burbano', '3214567890', 'jburbano@correo.com'),
  (2, '1085987654', 'Maria Elena Chaves',  '3109876543', 'mchaves@correo.com');

INSERT INTO finca (id_proveedor, nombre, lugar, latitud, longitud) VALUES
  (1, 'La Esperanza', 'Consaca, Narino',   1.208900, -77.468300),
  (2, 'El Mirador',   'Sandona, Narino',   1.284700, -77.471500);

INSERT INTO lote (codigo, id_variedad, id_finca, peso_kg, fecha_cosecha, estado) VALUES
  ('LOTE-0001', 1, 1, 1250.50, '2026-08-15', 'DISPONIBLE'),
  ('LOTE-0002', 5, 2,  980.00, '2026-08-22', 'DISPONIBLE');

INSERT INTO cliente (nombre_empresa, id_pais, ciudad, persona_contacto, correo, id_moneda_habitual)
VALUES ('Kaffee Hamburg GmbH',
        (SELECT id_pais FROM pais WHERE codigo_iso = 'DE'),
        'Hamburgo', 'Klaus Weber', 'kweber@kaffee-hh.de', 3);
