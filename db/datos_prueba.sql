-- MoronCorreo - Datos de prueba para defensa
-- Ejecutar DESPUÉS de schema.sql

-- Usuarios
INSERT INTO usuario (ci, nombre, apellido, rol, ci_nit, telefono, email, password) VALUES
  ('12345678', 'Carlos',  'Morón',    'PROPIETARIO', '12345678',  '77712345', 'carlos.moron@gmail.com',   'pass123'),
  ('23456789', 'Laura',   'Vásquez',  'VENDEDOR',    '23456789',  '77723456', 'laura.vasquez@gmail.com',  'pass123'),
  ('34567890', 'Diego',   'Quispe',   'VENDEDOR',    '34567890',  '77734567', 'diego.quispe@gmail.com',   'pass123'),
  ('45678901', 'Ana',     'Flores',   'CLIENTE',     '45678901',  '77745678', 'ana.flores@gmail.com',     'pass123'),
  ('56789012', 'Roberto', 'Mamani',   'CLIENTE',     '56789012',  '77756789', 'roberto.mamani@gmail.com', 'pass123'),
  ('67890123', 'Sofía',   'Herrera',  'CLIENTE',     '67890123',  '77767890', 'sofia.herrera@gmail.com',  'pass123');

-- Productos
INSERT INTO producto (nombre, descripcion, precio, stock, categoria) VALUES
  ('Sofá 3 cuerpos',      'Sofá moderno de tela gris, 3 plazas',              2500.00, 10, 'Sala'),
  ('Mesa de comedor',     'Mesa rectangular de madera nogal, 6 personas',     1800.00,  8, 'Comedor'),
  ('Silla ejecutiva',     'Silla ergonómica con reposabrazos regulable',        450.00, 20, 'Oficina'),
  ('Estante flotante',    'Estante de madera MDF blanco 120cm',                 280.00, 15, 'Decoracion'),
  ('Lámpara de pie',      'Lámpara pie de metal negro, foco LED incluido',      320.00, 12, 'Iluminacion'),
  ('Alfombra sala',       'Alfombra peluda 200x300cm color beige',              650.00,  6, 'Decoracion');

-- Inventario - ingresos iniciales (PEPS)
INSERT INTO inventario_movimiento (id_producto, tipo, cantidad, costo_unitario, tecnica, observaciones) VALUES
  (1, 'INGRESO', 10, 2000.00, 'PEPS', 'Stock inicial sofás'),
  (2, 'INGRESO',  8, 1400.00, 'PEPS', 'Stock inicial mesas'),
  (3, 'INGRESO', 20,  300.00, 'PEPS', 'Stock inicial sillas'),
  (4, 'INGRESO', 15,  180.00, 'PEPS', 'Stock inicial estantes'),
  (5, 'INGRESO', 12,  200.00, 'PEPS', 'Stock inicial lámparas'),
  (6, 'INGRESO',  6,  420.00, 'PEPS', 'Stock inicial alfombras');

-- Pedido 1 - cliente Ana Flores (id=4)
INSERT INTO pedido (id_usuario, estado, total, observaciones) VALUES
  (4, 'CONFIRMADO', 5300.00, 'Entrega a domicilio');

INSERT INTO pedido_detalle (id_pedido, id_producto, cantidad, precio_unitario, subtotal) VALUES
  (1, 1, 1, 2500.00, 2500.00),
  (1, 2, 1, 1800.00, 1800.00),
  (1, 6, 1,  650.00,  650.00),
  (1, 5, 1,  320.00,  320.00);

-- Pedido 2 - cliente Roberto Mamani (id=5)
INSERT INTO pedido (id_usuario, estado, total, observaciones) VALUES
  (5, 'ENTREGADO', 900.00, 'Retiro en tienda');

INSERT INTO pedido_detalle (id_pedido, id_producto, cantidad, precio_unitario, subtotal) VALUES
  (2, 3, 2, 450.00, 900.00);

-- Venta 1 - vendedor Laura (id=2), pedido 1
INSERT INTO venta (id_usuario, id_pedido, total, estado) VALUES
  (2, 1, 5300.00, 'COMPLETADA');

INSERT INTO venta_detalle (id_venta, id_producto, cantidad, precio_unitario, subtotal) VALUES
  (1, 1, 1, 2500.00, 2500.00),
  (1, 2, 1, 1800.00, 1800.00),
  (1, 6, 1,  650.00,  650.00),
  (1, 5, 1,  320.00,  320.00);

-- Salidas de inventario por venta 1
INSERT INTO inventario_movimiento (id_producto, tipo, cantidad, costo_unitario, tecnica, observaciones) VALUES
  (1, 'SALIDA', 1, 2000.00, 'PEPS', 'Venta #1'),
  (2, 'SALIDA', 1, 1400.00, 'PEPS', 'Venta #1'),
  (6, 'SALIDA', 1,  420.00, 'PEPS', 'Venta #1'),
  (5, 'SALIDA', 1,  200.00, 'PEPS', 'Venta #1');

-- Venta 2 - vendedor Diego (id=3), pedido 2
INSERT INTO venta (id_usuario, id_pedido, total, estado) VALUES
  (3, 2, 900.00, 'COMPLETADA');

INSERT INTO venta_detalle (id_venta, id_producto, cantidad, precio_unitario, subtotal) VALUES
  (2, 3, 2, 450.00, 900.00);

INSERT INTO inventario_movimiento (id_producto, tipo, cantidad, costo_unitario, tecnica, observaciones) VALUES
  (3, 'SALIDA', 2, 300.00, 'PEPS', 'Venta #2');

-- Pago contado - venta 2
INSERT INTO pago (id_venta, tipo, total, cuotas) VALUES
  (2, 'CONTADO', 900.00, 1);

INSERT INTO pago_cuota (id_pago, numero_cuota, monto, fecha_vencimiento, pagado, fecha_pago) VALUES
  (1, 1, 900.00, '2026-05-12', true, NOW());

-- Pago en cuotas - venta 1 (3 cuotas)
INSERT INTO pago (id_venta, tipo, total, cuotas) VALUES
  (1, 'CUOTAS', 5300.00, 3);

INSERT INTO pago_cuota (id_pago, numero_cuota, monto, fecha_vencimiento, pagado) VALUES
  (2, 1, 1766.67, '2026-05-31', false),
  (2, 2, 1766.67, '2026-06-30', false),
  (2, 3, 1766.66, '2026-07-31', false);

-- Promociones
INSERT INTO promocion (id_producto, porcentaje, fecha_inicio, fecha_fin, activo, descripcion) VALUES
  (4, 15.00, '2026-05-01', '2026-05-31', true,  'Promo mayo - estantes'),
  (5, 10.00, '2026-05-01', '2026-05-31', true,  'Promo mayo - iluminación'),
  (6, 20.00, '2026-04-01', '2026-04-30', false, 'Promo abril - alfombras (vencida)');
