-- MoronCorreo - Morón Diseño y Decoraciones de Interiores
-- Ejecutar: psql -U grupo14sa -d db_grupo14sa -f schema.sql

-- Limpiar en orden inverso a dependencias
DROP TABLE IF EXISTS pago_cuota CASCADE;
DROP TABLE IF EXISTS pago CASCADE;
DROP TABLE IF EXISTS venta_detalle CASCADE;
DROP TABLE IF EXISTS venta CASCADE;
DROP TABLE IF EXISTS promocion CASCADE;
DROP TABLE IF EXISTS inventario_movimiento CASCADE;
DROP TABLE IF EXISTS pedido_detalle CASCADE;
DROP TABLE IF EXISTS pedido CASCADE;
DROP TABLE IF EXISTS producto CASCADE;
DROP TABLE IF EXISTS usuario CASCADE;

-- CU1 - Usuarios
CREATE TABLE usuario (
  id SERIAL PRIMARY KEY,
  ci VARCHAR(20) UNIQUE NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  apellido VARCHAR(100) NOT NULL,
  rol VARCHAR(20) NOT NULL CHECK (rol IN ('PROPIETARIO','VENDEDOR','CLIENTE')),
  ci_nit VARCHAR(20),
  telefono VARCHAR(20),
  email VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(100) NOT NULL,
  activo BOOLEAN DEFAULT true,
  fecha_registro TIMESTAMP DEFAULT NOW()
);

-- CU2 - Productos
CREATE TABLE producto (
  id SERIAL PRIMARY KEY,
  nombre VARCHAR(150) NOT NULL,
  descripcion TEXT,
  precio NUMERIC(10,2) NOT NULL,
  stock INTEGER DEFAULT 0,
  categoria VARCHAR(100),
  activo BOOLEAN DEFAULT true,
  fecha_registro TIMESTAMP DEFAULT NOW()
);

-- CU3 - Pedidos
CREATE TABLE pedido (
  id SERIAL PRIMARY KEY,
  id_usuario INTEGER REFERENCES usuario(id),
  fecha TIMESTAMP DEFAULT NOW(),
  estado VARCHAR(20) DEFAULT 'PENDIENTE'
    CHECK (estado IN ('PENDIENTE','CONFIRMADO','ENTREGADO','CANCELADO')),
  total NUMERIC(10,2) DEFAULT 0,
  observaciones TEXT
);

CREATE TABLE pedido_detalle (
  id SERIAL PRIMARY KEY,
  id_pedido INTEGER REFERENCES pedido(id),
  id_producto INTEGER REFERENCES producto(id),
  cantidad INTEGER NOT NULL,
  precio_unitario NUMERIC(10,2) NOT NULL,
  subtotal NUMERIC(10,2) NOT NULL
);

-- CU4 - Inventario
CREATE TABLE inventario_movimiento (
  id SERIAL PRIMARY KEY,
  id_producto INTEGER REFERENCES producto(id),
  tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('INGRESO','SALIDA')),
  cantidad INTEGER NOT NULL,
  costo_unitario NUMERIC(10,2),
  tecnica VARCHAR(10) DEFAULT 'PEPS' CHECK (tecnica IN ('PEPS','UEPS','PROMEDIO')),
  fecha TIMESTAMP DEFAULT NOW(),
  observaciones TEXT
);

-- CU5 - Promociones
CREATE TABLE promocion (
  id SERIAL PRIMARY KEY,
  id_producto INTEGER REFERENCES producto(id),
  porcentaje NUMERIC(5,2) NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin DATE NOT NULL,
  activo BOOLEAN DEFAULT true,
  descripcion TEXT
);

-- CU6 - Ventas
CREATE TABLE venta (
  id SERIAL PRIMARY KEY,
  id_usuario INTEGER REFERENCES usuario(id),
  id_pedido INTEGER REFERENCES pedido(id),
  fecha TIMESTAMP DEFAULT NOW(),
  total NUMERIC(10,2) NOT NULL,
  estado VARCHAR(20) DEFAULT 'COMPLETADA'
);

CREATE TABLE venta_detalle (
  id SERIAL PRIMARY KEY,
  id_venta INTEGER REFERENCES venta(id),
  id_producto INTEGER REFERENCES producto(id),
  cantidad INTEGER NOT NULL,
  precio_unitario NUMERIC(10,2) NOT NULL,
  subtotal NUMERIC(10,2) NOT NULL
);

-- CU7 - Pagos
CREATE TABLE pago (
  id SERIAL PRIMARY KEY,
  id_venta INTEGER REFERENCES venta(id),
  tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('CONTADO','CUOTAS','QR')),
  total NUMERIC(10,2) NOT NULL,
  cuotas INTEGER DEFAULT 1,
  tasa_interes NUMERIC(5,4) DEFAULT 0,
  cuota_inicial NUMERIC(10,2) DEFAULT 0,
  fecha TIMESTAMP DEFAULT NOW()
);

CREATE TABLE pago_cuota (
  id SERIAL PRIMARY KEY,
  id_pago INTEGER REFERENCES pago(id),
  numero_cuota INTEGER NOT NULL,
  monto NUMERIC(10,2) NOT NULL,
  fecha_vencimiento DATE NOT NULL,
  pagado BOOLEAN DEFAULT false,
  fecha_pago TIMESTAMP
);

-- PagoFácil QR
CREATE TABLE pago_qr (
  id SERIAL PRIMARY KEY,
  id_pago INTEGER REFERENCES pago(id),
  pagofacil_transaction_id VARCHAR(50),
  payment_number VARCHAR(50) UNIQUE NOT NULL,
  checkout_url TEXT,
  estado VARCHAR(20) DEFAULT 'PENDIENTE'
    CHECK (estado IN ('PENDIENTE','PAGADO','EXPIRADO','ANULADO')),
  fecha_pago TIMESTAMP
);
