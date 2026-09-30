-- =====================================================================
-- A. CREACIÓN DE LAS 5 VISTAS (VIEWS)
-- =====================================================================

USE pixel_zone;

-- 1. Vista de inventario detallado de ejemplares con producto, categoría y plataforma
CREATE OR REPLACE VIEW vw_inventario_ejemplares AS
SELECT
    e.numero_serie AS Serie,
    p.codigo_interno AS Codigo,
    p.nombre AS Producto,
    c.nombre AS Categoria,
    COALESCE(pl.nombre, 'Multiconsola / N/A') AS Plataforma,
    e.condicion AS Condicion,
    e.costo AS Costo,
    e.precio_venta AS Precio_Venta,
    e.estado AS Estado,
    IF(e.tiene_caja, 'Sí', 'No') AS Caja,
    IF(e.tiene_manual, 'Sí', 'No') AS Manual,
    e.fecha_ingreso AS Fecha_Ingreso
FROM ejemplares e
INNER JOIN productos p ON e.id_producto = p.id_producto
INNER JOIN categorias c ON p.id_categoria = c.id_categoria
LEFT JOIN plataformas pl ON p.id_plataforma = pl.id_plataforma;

-- 2. Vista de historial detallado de ventas (POS) con cliente, vendedor y producto
CREATE OR REPLACE VIEW vw_historial_ventas AS
SELECT
    v.folio AS Folio,
    v.fecha_venta AS Fecha,
    cl.nombre AS Cliente,
    u.nombre_completo AS Vendedor,
    e.numero_serie AS Serie_Ejemplar,
    p.nombre AS Producto,
    dv.cantidad AS Cantidad,
    dv.precio_unitario AS Precio_Unitario,
    dv.subtotal AS Subtotal,
    v.descuento AS Descuento_Venta,
    v.impuestos AS Impuestos_Venta,
    v.estado AS Estado_Venta
FROM ventas v
         INNER JOIN clientes cl ON v.id_cliente = cl.id_cliente
         INNER JOIN usuarios u ON v.id_usuario = u.id_usuario
         INNER JOIN detalle_ventas dv ON v.id_venta = dv.id_venta
         INNER JOIN ejemplares e ON dv.id_ejemplar = e.id_ejemplar
         INNER JOIN productos p ON e.id_producto = p.id_producto;

-- 3. Vista de rentas activas y vencidas con cálculo de días restantes
CREATE OR REPLACE VIEW vw_rentas_activas AS
SELECT
    cl.nombre AS Cliente,
    cl.telefono AS Telefono,
    p.nombre AS Videojuego,
    e.numero_serie AS Serie,
    u.nombre_completo AS Atendio,
    r.fecha_renta AS Fecha_Renta,
    r.fecha_limite AS Fecha_Limite,
    DATEDIFF(r.fecha_limite, CURDATE()) AS Dias_Restantes,
    r.monto_renta AS Monto_Renta,
    r.deposito AS Deposito,
    r.estado AS Estado
FROM rentas r
         INNER JOIN clientes cl ON r.id_cliente = cl.id_cliente
         INNER JOIN usuarios u ON r.id_usuario = u.id_usuario
         INNER JOIN ejemplares e ON r.id_ejemplar = e.id_ejemplar
         INNER JOIN productos p ON e.id_producto = p.id_producto
WHERE r.estado IN ('activa', 'vencida');

-- 4. Vista de pedidos de E-Commerce con dirección completa y total calculado
CREATE OR REPLACE VIEW vw_pedidos_ecommerce AS
SELECT
    pe.id_pedido AS ID_Pedido,
    pe.fecha_pedido AS Fecha,
    cl.nombre AS Cliente,
    CONCAT(dc.calle, ', Col. ', COALESCE(dc.colonia, 'S/N'), ', ', dc.ciudad, ', ', dc.estado, ' CP ', dc.codigo_postal) AS Direccion_Envio,
    COALESCE(pe.paqueteria, 'Por asignar') AS Paqueteria,
    COALESCE(pe.numero_guia, 'Sin guía') AS Guia,
    (COALESCE(SUM(dp.subtotal), 0) - pe.descuento + pe.impuestos + pe.costo_envio) AS Total_Pedido,
    pe.estado AS Estado
FROM pedidos pe
         INNER JOIN direcciones_cliente dc ON pe.id_direccion = dc.id_direccion
         INNER JOIN clientes cl ON dc.id_cliente = cl.id_cliente
         LEFT JOIN detalle_pedidos dp ON pe.id_pedido = dp.id_pedido
GROUP BY pe.id_pedido, pe.fecha_pedido, cl.nombre, dc.calle, dc.colonia, dc.ciudad, dc.estado, dc.codigo_postal, pe.paqueteria, pe.numero_guia, pe.descuento, pe.impuestos, pe.costo_envio, pe.estado;

-- 5. Vista de Kardex de movimientos de inventario (auditoría de entradas/salidas)
CREATE OR REPLACE VIEW vw_kardex_movimientos AS
SELECT
    m.fecha_movimiento AS Fecha,
    e.numero_serie AS Serie,
    p.codigo_interno AS Codigo,
    p.nombre AS Producto,
    m.tipo_movimiento AS Tipo_Movimiento,
    m.motivo AS Motivo,
    m.cantidad AS Cantidad,
    u.nombre_completo AS Usuario_Responsable,
    COALESCE(m.observaciones, '') AS Observaciones
FROM movimientos_inventario m
         INNER JOIN ejemplares e ON m.id_ejemplar = e.id_ejemplar
         INNER JOIN productos p ON e.id_producto = p.id_producto
         INNER JOIN usuarios u ON m.id_usuario = u.id_usuario
ORDER BY m.fecha_movimiento DESC;


-- =====================================================================
-- B. CREACIÓN DE LOS 5 PROCEDIMIENTOS ALMACENADOS (STORED PROCEDURES)
-- =====================================================================

DELIMITER $$

-- 1. Filtrar productos por nombre de plataforma (ej. 'PlayStation', 'Xbox', 'Switch')
DROP PROCEDURE IF EXISTS sp_productos_por_plataforma$$
CREATE PROCEDURE sp_productos_por_plataforma(IN p_plat VARCHAR(100))
BEGIN
    SELECT
        p.codigo_interno AS Codigo,
        p.nombre AS Producto,
        p.tipo AS Tipo,
        c.nombre AS Categoria,
        COALESCE(pl.nombre, 'Sin Plataforma') AS Plataforma,
        p.precio_nuevo AS Precio_Nuevo,
        p.precio_usado AS Precio_Usado,
        IF(p.rentable, 'Sí', 'No') AS Rentable
    FROM productos p
             INNER JOIN categorias c ON p.id_categoria = c.id_categoria
             LEFT JOIN plataformas pl ON p.id_plataforma = pl.id_plataforma
    WHERE pl.nombre LIKE CONCAT('%', p_plat, '%')
       OR p.id_plataforma = p_plat;
END$$

-- 2. Filtrar ejemplares físicos según su estado ('disponible','rentado','vendido','apartado','reservado','baja')
DROP PROCEDURE IF EXISTS sp_ejemplares_por_estado$$
CREATE PROCEDURE sp_ejemplares_por_estado(IN p_est VARCHAR(30))
BEGIN
    SELECT
        e.numero_serie AS Serie,
        p.codigo_interno AS Codigo,
        p.nombre AS Producto,
        e.condicion AS Condicion,
        e.costo AS Costo,
        e.precio_venta AS Precio_Venta,
        e.estado AS Estado,
        e.fecha_ingreso AS Fecha_Ingreso
    FROM ejemplares e
             INNER JOIN productos p ON e.id_producto = p.id_producto
    WHERE e.estado = p_est OR p_est = '';
END$$

-- 3. Consultar pagos recibidos filtrados por método ('efectivo','tarjeta','transferencia','otro')
DROP PROCEDURE IF EXISTS sp_pagos_por_metodo$$
CREATE PROCEDURE sp_pagos_por_metodo(IN p_met VARCHAR(30))
BEGIN
    SELECT
        pa.id_pago AS ID_Pago,
        pa.fecha_pago AS Fecha,
        pa.metodo_pago AS Metodo,
        pa.monto AS Monto,
        CASE
            WHEN pa.id_venta IS NOT NULL THEN CONCAT('Venta POS (Folio: ', COALESCE(v.folio, ''), ')')
            WHEN pa.id_pedido IS NOT NULL THEN 'Pedido E-Commerce'
            WHEN pa.id_renta IS NOT NULL THEN 'Renta de Videojuego'
            WHEN pa.id_apartado IS NOT NULL THEN CONCAT('Apartado (Folio: ', COALESCE(a.folio, ''), ')')
            ELSE 'Otro'
            END AS Origen_Operacion
    FROM pagos pa
             LEFT JOIN ventas v ON pa.id_venta = v.id_venta
             LEFT JOIN apartados a ON pa.id_apartado = a.id_apartado
    WHERE pa.metodo_pago = p_met OR p_met = ''
    ORDER BY pa.fecha_pago DESC;
END$$

-- 4. Consultar historial de órdenes de compra a proveedores por nombre de proveedor
DROP PROCEDURE IF EXISTS sp_compras_proveedor$$
CREATE PROCEDURE sp_compras_proveedor(IN p_prov VARCHAR(150))
BEGIN
    SELECT
        co.id_compra AS ID_Compra,
        pr.nombre AS Proveedor,
        u.nombre_completo AS Comprador,
        co.fecha_compra AS Fecha,
        co.estado AS Estado,
        COALESCE(SUM(dc.cantidad), 0) AS Total_Articulos,
        COALESCE(SUM(dc.subtotal), 0.00) AS Costo_Total_Compra
    FROM compras co
             INNER JOIN proveedores pr ON co.id_proveedor = pr.id_proveedor
             INNER JOIN usuarios u ON co.id_usuario = u.id_usuario
             LEFT JOIN detalle_compras dc ON co.id_compra = dc.id_compra
    WHERE pr.nombre LIKE CONCAT('%', p_prov, '%')
       OR pr.id_proveedor = p_prov
    GROUP BY co.id_compra, pr.nombre, u.nombre_completo, co.fecha_compra, co.estado
    ORDER BY co.fecha_compra DESC;
END$$

-- 5. Consultar estado y saldo pendiente de apartados por folio o estado
DROP PROCEDURE IF EXISTS sp_estado_apartados$$
CREATE PROCEDURE sp_estado_apartados(IN p_fol VARCHAR(30))
BEGIN
    SELECT
        a.folio AS Folio,
        c.nombre AS Cliente,
        p.nombre AS Producto,
        e.numero_serie AS Serie,
        e.precio_venta AS Precio_Total,
        a.importe_anticipo AS Anticipo_Pagado,
        (e.precio_venta - a.importe_anticipo) AS Saldo_Pendiente,
        a.fecha_limite AS Fecha_Limite,
        a.estado AS Estado
    FROM apartados a
             INNER JOIN clientes c ON a.id_cliente = c.id_cliente
             INNER JOIN ejemplares e ON a.id_ejemplar = e.id_ejemplar
             INNER JOIN productos p ON e.id_producto = p.id_producto
    WHERE a.folio LIKE CONCAT('%', p_fol, '%')
       OR a.estado = p_fol;
END$$


-- =====================================================================
-- C. CREACIÓN DE LAS 5 FUNCIONES (FUNCTIONS)
-- =====================================================================

-- 1. Obtener la cantidad de ejemplares disponibles en stock por código interno o nombre de producto
DROP FUNCTION IF EXISTS fn_stock_disponible$$
CREATE FUNCTION fn_stock_disponible(p_cod VARCHAR(30))
    RETURNS INT
    DETERMINISTIC
    READS SQL DATA
BEGIN
    DECLARE v_stock INT DEFAULT 0;
    SELECT COUNT(*) INTO v_stock
    FROM ejemplares e
             INNER JOIN productos p ON e.id_producto = p.id_producto
    WHERE (p.codigo_interno = p_cod OR p.nombre LIKE CONCAT('%', p_cod, '%'))
      AND e.estado = 'disponible';
    RETURN COALESCE(v_stock, 0);
END$$

-- 2. Calcular el monto total neto de una venta (Subtotal - Descuento + Impuestos) dado su folio
DROP FUNCTION IF EXISTS fn_total_venta_folio$$
CREATE FUNCTION fn_total_venta_folio(p_fol VARCHAR(30))
    RETURNS DECIMAL(10,2)
    DETERMINISTIC
    READS SQL DATA
BEGIN
    DECLARE v_total DECIMAL(10,2) DEFAULT 0.00;
    SELECT (COALESCE(SUM(dv.subtotal), 0.00) - v.descuento + v.impuestos) INTO v_total
    FROM ventas v
             LEFT JOIN detalle_ventas dv ON v.id_venta = dv.id_venta
    WHERE v.folio = p_fol AND v.estado = 'completada'
    GROUP BY v.id_venta, v.descuento, v.impuestos;
    RETURN COALESCE(v_total, 0.00);
END$$

-- 3. Calcular la ganancia neta (precio_venta - costo) de un ejemplar por su número de serie
DROP FUNCTION IF EXISTS fn_ganancia_ejemplar$$
CREATE FUNCTION fn_ganancia_ejemplar(p_ser VARCHAR(60))
    RETURNS DECIMAL(10,2)
    DETERMINISTIC
    READS SQL DATA
BEGIN
    DECLARE v_ganancia DECIMAL(10,2) DEFAULT 0.00;
    SELECT (e.precio_venta - e.costo) INTO v_ganancia
    FROM ejemplares e
    WHERE e.numero_serie = p_ser
    LIMIT 1;
    RETURN COALESCE(v_ganancia, 0.00);
END$$

-- 4. Calcular el total de ingresos acumulados por método de pago ('efectivo','tarjeta','transferencia','otro')
DROP FUNCTION IF EXISTS fn_ingresos_por_metodo$$
CREATE FUNCTION fn_ingresos_por_metodo(p_met VARCHAR(30))
    RETURNS DECIMAL(10,2)
    DETERMINISTIC
    READS SQL DATA
BEGIN
    DECLARE v_ingresos DECIMAL(10,2) DEFAULT 0.00;
    SELECT COALESCE(SUM(monto), 0.00) INTO v_ingresos
    FROM pagos
    WHERE metodo_pago = p_met;
    RETURN COALESCE(v_ingresos, 0.00);
END$$

-- 5. Contar el número de rentas activas o vencidas que tiene un cliente por su nombre
DROP FUNCTION IF EXISTS fn_rentas_activas_cliente$$
CREATE FUNCTION fn_rentas_activas_cliente(p_nom VARCHAR(150))
    RETURNS INT
    DETERMINISTIC
    READS SQL DATA
BEGIN
    DECLARE v_rentas INT DEFAULT 0;
    SELECT COUNT(*) INTO v_rentas
    FROM rentas r
             INNER JOIN clientes c ON r.id_cliente = c.id_cliente
    WHERE (c.nombre LIKE CONCAT('%', p_nom, '%') OR c.id_cliente = p_nom)
      AND r.estado IN ('activa', 'vencida');
    RETURN COALESCE(v_rentas, 0);
END$$

DELIMITER ;