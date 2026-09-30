USE pixel_zone;

-- =====================================================================
-- A. INSERCIÓN DE DATOS COHERENTES EN TODAS LAS TABLAS (24 TABLAS)
-- Se utilizan UUIDs fijos de 36 caracteres para mantener la integridad
-- referencial entre llaves primarias y foráneas.
-- =====================================================================

-- 1. PERFILES
INSERT INTO perfiles (id_perfil, nombre, descripcion) VALUES
('10000000-0000-0000-0000-000000000001', 'Administrador', 'Control total del sistema Pixel Zone'),
('10000000-0000-0000-0000-000000000002', 'Vendedor POS', 'Atención en mostrador, ventas, rentas y apartados'),
('10000000-0000-0000-0000-000000000003', 'Almacenista', 'Control de inventario, compras y recepciones');

-- 2. PERMISOS
INSERT INTO permisos (id_permiso, nombre, descripcion) VALUES
('20000000-0000-0000-0000-000000000001', 'GESTION_VENTAS', 'Permite registrar y cancelar ventas'),
('20000000-0000-0000-0000-000000000002', 'GESTION_INVENTARIO', 'Permite dar de alta productos y ejemplares'),
('20000000-0000-0000-0000-000000000003', 'GESTION_RENTAS', 'Permite gestionar rentas y devoluciones');

-- 3. PERFIL_PERMISOS
INSERT INTO perfil_permisos (id_perfil, id_permiso) VALUES
('10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001'),
('10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002'),
('10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003'),
('10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001'),
('10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003'),
('10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002');

-- 4. USUARIOS
INSERT INTO usuarios (id_usuario, id_perfil, nombre_usuario, contrasena, nombre_completo, correo, activo) VALUES
('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'admin_pixel', 'hash_admin123', 'Roberto Gómez', 'roberto@pixelzone.com', TRUE),
('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'caja_ana', 'hash_ana456', 'Ana Martínez', 'ana@pixelzone.com', TRUE),
('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'almacen_luis', 'hash_luis789', 'Luis Hernández', 'luis@pixelzone.com', TRUE);

-- 5. CLIENTES
INSERT INTO clientes (id_cliente, nombre, telefono, correo, tipo_cliente, activo) VALUES
('40000000-0000-0000-0000-000000000001', 'Carlos Ramírez', '3312345678', 'carlos.ramirez@email.com', 'registrado', TRUE),
('40000000-0000-0000-0000-000000000002', 'Fernanda López', '3387654321', 'fer.lopez@email.com', 'registrado', TRUE),
('40000000-0000-0000-0000-000000000003', 'Miguel Ángel Torres', '3355554444', 'miguel.torres@email.com', 'mostrador', TRUE),
('40000000-0000-0000-0000-000000000004', 'Cliente Mostrador General', NULL, NULL, 'anonimo', TRUE);

-- 6. DIRECCIONES_CLIENTE
INSERT INTO direcciones_cliente (id_direccion, id_cliente, alias, calle, colonia, ciudad, estado, codigo_postal, es_principal) VALUES
('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'Casa', 'Av. Vallarta 1540', 'Americana', 'Guadalajara', 'Jalisco', '44160', TRUE),
('41000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'Departamento', 'Av. Patria 890', 'Jardines Universidad', 'Zapopan', 'Jalisco', '45110', TRUE);

-- 7. CATEGORIAS
INSERT INTO categorias (id_categoria, nombre, descripcion) VALUES
('50000000-0000-0000-0000-000000000001', 'Videojuegos', 'Juegos físicos para distintas consolas'),
('50000000-0000-0000-0000-000000000002', 'Consolas', 'Sistemas de entretenimiento nuevos y seminuevos'),
('50000000-0000-0000-0000-000000000003', 'Accesorios', 'Controles, audífonos, bases de carga y cables');

-- 8. PLATAFORMAS
INSERT INTO plataformas (id_plataforma, nombre, fabricante) VALUES
('60000000-0000-0000-0000-000000000001', 'PlayStation 5', 'Sony'),
('60000000-0000-0000-0000-000000000002', 'Nintendo Switch', 'Nintendo'),
('60000000-0000-0000-0000-000000000003', 'Xbox Series X', 'Microsoft');

-- 9. PRODUCTOS
INSERT INTO productos (id_producto, id_categoria, id_plataforma, codigo_interno, codigo_barras, nombre, descripcion, tipo, genero, clasificacion, edicion, fecha_lanzamiento, precio_nuevo, precio_usado, rentable, activo) VALUES
('70000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'VJ-001', '711719541028', 'Marvel Spider-Man 2', 'Juego de acción y aventura en Nueva York', 'videojuego', 'Acción', 'T', 'Estándar', '2023-10-20', 1399.00, 950.00, TRUE, TRUE),
('70000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000002', 'VJ-002', '045496599058', 'The Legend of Zelda: Tears of the Kingdom', 'Aventura épica en los cielos de Hyrule', 'videojuego', 'Aventura', 'E10+', 'Estándar', '2023-05-12', 1299.00, 899.00, TRUE, TRUE),
('70000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000003', 'VJ-003', '889842651324', 'Forza Motorsport', 'Simulador de carreras de nueva generación', 'videojuego', 'Carreras', 'E', 'Estándar', '2023-10-10', 1199.00, 799.00, TRUE, TRUE),
('70000000-0000-0000-0000-000000000004', '50000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000001', 'ACC-001', '711719541998', 'Control DualSense Blanco', 'Control inalámbrico oficial para PS5', 'accesorio', 'Hardware', 'E', 'Estándar', '2020-11-12', 1499.00, 999.00, FALSE, TRUE);

-- 10. EJEMPLARES
INSERT INTO ejemplares (id_ejemplar, id_producto, numero_serie, condicion, costo, precio_venta, estado, tiene_caja, tiene_manual, observaciones, id_cliente_origen) VALUES
('80000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'SN-SP2-001', 'nuevo', 900.00, 1399.00, 'vendido', TRUE, TRUE, 'Ejemplar sellado de fábrica', NULL),
('80000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000001', 'SN-SP2-002', 'usado', 500.00, 950.00, 'disponible', TRUE, FALSE, 'Comprado a cliente en buen estado', '40000000-0000-0000-0000-000000000001'),
('80000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000002', 'SN-ZLD-001', 'nuevo', 850.00, 1299.00, 'rentado', TRUE, TRUE, 'Destinado a catálogo de renta', NULL),
('80000000-0000-0000-0000-000000000004', '70000000-0000-0000-0000-000000000003', 'SN-FRZ-001', 'nuevo', 750.00, 1199.00, 'apartado', TRUE, TRUE, 'Apartado en mostrador', NULL),
('80000000-0000-0000-0000-000000000005', '70000000-0000-0000-0000-000000000004', 'SN-DLS-001', 'nuevo', 950.00, 1499.00, 'disponible', TRUE, TRUE, 'En vitrina principal', NULL);

-- 11. PROVEEDORES
INSERT INTO proveedores (id_proveedor, nombre, contacto, telefono, correo, direccion, activo) VALUES
('90000000-0000-0000-0000-000000000001', 'Distribuidora Gamer México', 'Ing. Ricardo Soto', '5511223344', 'ventas@gamermex.com', 'Av. Insurgentes Sur 400, CDMX', TRUE),
('90000000-0000-0000-0000-000000000002', 'Importaciones Latam Play', 'Lic. Sofía Vega', '8144556677', 'contacto@latamplay.mx', 'Parque Industrial, Monterrey, NL', TRUE);

-- 12. COMPRAS
INSERT INTO compras (id_compra, id_proveedor, id_usuario, estado) VALUES
('91000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000003', 'recibida');

-- 13. DETALLE_COMPRAS (subtotal se calcula automáticamente)
INSERT INTO detalle_compras (id_detalle_compra, id_compra, id_producto, cantidad, costo_unitario) VALUES
('92000000-0000-0000-0000-000000000001', '91000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 5, 900.00),
('92000000-0000-0000-0000-000000000002', '91000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000002', 3, 850.00);

-- 14. COMPRAS_USADO
INSERT INTO compras_usado (id_compra_usado, id_cliente, id_usuario, id_ejemplar, precio_compra, observaciones) VALUES
('93000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', '80000000-0000-0000-0000-000000000002', 500.00, 'Disco sin rayones, caja original');

-- 15. VENTAS
INSERT INTO ventas (id_venta, folio, id_cliente, id_usuario, descuento, impuestos, estado) VALUES
('a0000000-0000-0000-0000-000000000001', 'VTA-001', '40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', 0.00, 223.84, 'completada');

-- 16. DETALLE_VENTAS
INSERT INTO detalle_ventas (id_detalle_venta, id_venta, id_ejemplar, cantidad, precio_unitario) VALUES
('a1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', 1, 1399.00);

-- 17. RENTAS
INSERT INTO rentas (id_renta, id_cliente, id_usuario, id_ejemplar, fecha_limite, monto_renta, deposito, monto_extra, estado) VALUES
('b0000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', '80000000-0000-0000-0000-000000000003', DATE_ADD(CURDATE(), INTERVAL 7 DAY), 180.00, 300.00, 0.00, 'activa');

-- 18. APARTADOS
INSERT INTO apartados (id_apartado, folio, id_cliente, id_ejemplar, fecha_limite, importe_anticipo, estado) VALUES
('c0000000-0000-0000-0000-000000000001', 'APT-001', '40000000-0000-0000-0000-000000000003', '80000000-0000-0000-0000-000000000004', DATE_ADD(CURDATE(), INTERVAL 15 DAY), 300.00, 'activo');

-- 19. PEDIDOS
INSERT INTO pedidos (id_pedido, id_direccion, descuento, impuestos, costo_envio, paqueteria, numero_guia, estado) VALUES
('d0000000-0000-0000-0000-000000000001', '41000000-0000-0000-0000-000000000001', 50.00, 200.00, 120.00, 'DHL Express', 'GUIA-MX-998877', 'enviado');

-- 20. DETALLE_PEDIDOS
INSERT INTO detalle_pedidos (id_detalle_pedido, id_pedido, id_producto, cantidad, precio_unitario) VALUES
('d1000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000004', 1, 1499.00);

-- 21. PAGOS (Cumpliendo el CHECK de una sola operación por registro)
INSERT INTO pagos (id_pago, id_venta, id_pedido, id_renta, id_apartado, monto, metodo_pago) VALUES
('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', NULL, NULL, NULL, 1622.84, 'tarjeta'),
('e0000000-0000-0000-0000-000000000002', NULL, 'd0000000-0000-0000-0000-000000000001', NULL, NULL, 1769.00, 'transferencia'),
('e0000000-0000-0000-0000-000000000003', NULL, NULL, 'b0000000-0000-0000-0000-000000000001', NULL, 480.00, 'efectivo'),
('e0000000-0000-0000-0000-000000000004', NULL, NULL, NULL, 'c0000000-0000-0000-0000-000000000001', 300.00, 'efectivo');

-- 22. MOVIMIENTOS_INVENTARIO
INSERT INTO movimientos_inventario (id_movimiento, id_ejemplar, id_usuario, tipo_movimiento, motivo, cantidad, observaciones) VALUES
('f0000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', 'salida', 'venta', 1, 'Venta folio VTA-001'),
('f0000000-0000-0000-0000-000000000002', '80000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 'entrada', 'compra_usado', 1, 'Ingreso seminuevo cliente Carlos'),
('f0000000-0000-0000-0000-000000000003', '80000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000002', 'salida', 'renta', 1, 'Renta a Fernanda López');

-- 23. PROMOCIONES
INSERT INTO promociones (id_promocion, nombre, descripcion, tipo_descuento, valor_descuento, fecha_inicio, fecha_fin, id_producto, id_categoria, id_plataforma, id_usuario, id_usuario_autoriza, activo) VALUES
('f1000000-0000-0000-0000-000000000001', 'Semana Gamer PS5', '15% de descuento en catálogo seleccionado de PlayStation 5', 'porcentaje', 15.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), NULL, NULL, '60000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', TRUE);

-- 24. DEVOLUCIONES_GARANTIA
INSERT INTO devoluciones_garantia (id_devolucion, id_detalle_venta, motivo, condicion_recibida, estado, monto_reembolso, id_usuario) VALUES
('f2000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'Revisión de lectura de disco en consola', 'buena', 'solicitada', 0.00, '30000000-0000-0000-0000-000000000002');


