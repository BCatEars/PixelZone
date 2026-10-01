-- =====================================================================
-- BASE DE DATOS: PIXEL ZONE
-- Proyecto escolar - Esquema simplificado y revisado (3FN / BCNF)
-- Reglas: SIN AUTO_INCREMENT, SIN ALTER TABLE
--         PK CHAR(36) DEFAULT (UUID())
--
-- Notas de la revision:
--   * productos.tipo se movio a categorias.tipo: id_categoria -> tipo era una
--     dependencia transitiva (id_producto -> id_categoria -> tipo) que violaba 3FN.
--   * pedidos guarda id_cliente; la direccion de envio vive en pedido_envio
--     (id_direccion -> id_cliente romperia BCNF si ambos vivieran en pedidos).
--   * detalle_pedidos referencia el ejemplar fisico (id_ejemplar), no el producto.
--   * El alcance de promociones se extrajo a promocion_alcance.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS pixel_zone
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pixel_zone;

-- =====================================================================
-- 1. SEGURIDAD Y USUARIOS
-- =====================================================================

CREATE TABLE perfiles (
                          id_perfil   CHAR(36)     NOT NULL DEFAULT (UUID()),
                          nombre      VARCHAR(80)  NOT NULL,
                          descripcion VARCHAR(255) NULL,
                          CONSTRAINT pk_perfiles PRIMARY KEY (id_perfil),
                          CONSTRAINT uq_perfiles_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE permisos (
                          id_permiso  CHAR(36)     NOT NULL DEFAULT (UUID()),
                          nombre      VARCHAR(80)  NOT NULL,
                          descripcion VARCHAR(255) NULL,
                          CONSTRAINT pk_permisos PRIMARY KEY (id_permiso),
                          CONSTRAINT uq_permisos_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE perfil_permisos (
                                 id_perfil  CHAR(36) NOT NULL,
                                 id_permiso CHAR(36) NOT NULL,
                                 CONSTRAINT pk_perfil_permisos PRIMARY KEY (id_perfil, id_permiso),
                                 CONSTRAINT fk_pp_perfil  FOREIGN KEY (id_perfil)  REFERENCES perfiles(id_perfil)  ON UPDATE CASCADE ON DELETE CASCADE,
                                 CONSTRAINT fk_pp_permiso FOREIGN KEY (id_permiso) REFERENCES permisos(id_permiso) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE usuarios (
                          id_usuario      CHAR(36)     NOT NULL DEFAULT (UUID()),
                          id_perfil       CHAR(36)     NOT NULL,
                          nombre_usuario  VARCHAR(50)  NOT NULL,
                          contrasena      VARCHAR(255) NOT NULL,
                          nombre_completo VARCHAR(150) NOT NULL,
                          correo          VARCHAR(150) NULL,
                          activo          BOOLEAN      NOT NULL DEFAULT TRUE,
                          fecha_creacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT pk_usuarios PRIMARY KEY (id_usuario),
                          CONSTRAINT uq_usuarios_nombre UNIQUE (nombre_usuario),
                          CONSTRAINT fk_usuarios_perfil FOREIGN KEY (id_perfil) REFERENCES perfiles(id_perfil) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =====================================================================
-- 2. CLIENTES
-- =====================================================================

CREATE TABLE clientes (
                          id_cliente     CHAR(36)     NOT NULL DEFAULT (UUID()),
                          nombre         VARCHAR(150) NOT NULL,
                          telefono       VARCHAR(20)  NULL,
                          correo         VARCHAR(150) NULL,
                          tipo_cliente   ENUM('registrado','mostrador','anonimo') NOT NULL DEFAULT 'registrado',
                          fecha_registro DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          activo         BOOLEAN      NOT NULL DEFAULT TRUE,
                          CONSTRAINT pk_clientes PRIMARY KEY (id_cliente)
) ENGINE=InnoDB;

CREATE TABLE direcciones_cliente (
                                     id_direccion  CHAR(36)     NOT NULL DEFAULT (UUID()),
                                     id_cliente    CHAR(36)     NOT NULL,
                                     alias         VARCHAR(50)  NULL,
                                     calle         VARCHAR(150) NOT NULL,
                                     colonia       VARCHAR(100) NULL,
                                     ciudad        VARCHAR(100) NOT NULL,
                                     estado        VARCHAR(100) NOT NULL,
                                     codigo_postal VARCHAR(10)  NOT NULL,
                                     es_principal  BOOLEAN      NOT NULL DEFAULT FALSE,
                                     CONSTRAINT pk_direcciones_cliente PRIMARY KEY (id_direccion),
                                     CONSTRAINT fk_direccion_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- 3. CATÁLOGO E INVENTARIO
-- =====================================================================

CREATE TABLE categorias (
                            id_categoria CHAR(36)     NOT NULL DEFAULT (UUID()),
                            nombre       VARCHAR(100) NOT NULL,
                            tipo         ENUM('videojuego','consola','accesorio','otro') NOT NULL DEFAULT 'otro',
                            descripcion  VARCHAR(255) NULL,
                            CONSTRAINT pk_categorias PRIMARY KEY (id_categoria),
                            CONSTRAINT uq_categorias_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE plataformas (
                             id_plataforma CHAR(36)     NOT NULL DEFAULT (UUID()),
                             nombre        VARCHAR(100) NOT NULL,
                             fabricante    VARCHAR(100) NULL,
                             CONSTRAINT pk_plataformas PRIMARY KEY (id_plataforma),
                             CONSTRAINT uq_plataformas_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- Catalogo de generos (crece con el tiempo; por eso tabla, no ENUM).
CREATE TABLE generos (
                         id_genero   CHAR(36)     NOT NULL DEFAULT (UUID()),
                         nombre      VARCHAR(80)  NOT NULL,
                         descripcion VARCHAR(255) NULL,
                         CONSTRAINT pk_generos PRIMARY KEY (id_genero),
                         CONSTRAINT uq_generos_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE productos (
                           id_producto       CHAR(36)      NOT NULL DEFAULT (UUID()),
                           id_categoria      CHAR(36)      NOT NULL,
                           id_plataforma     CHAR(36)      NULL,
                           id_genero         CHAR(36)      NULL,
                           codigo_interno    VARCHAR(30)   NOT NULL,
                           codigo_barras     VARCHAR(60)   NULL,
                           nombre            VARCHAR(150)  NOT NULL,
                           descripcion       TEXT          NULL,
                           clasificacion     ENUM('E','E10+','T','M','AO','RP') NULL,
                           edicion           VARCHAR(100)  NULL,
                           fecha_lanzamiento DATE          NULL,
                           precio_nuevo      DECIMAL(10,2) NULL,
                           precio_usado      DECIMAL(10,2) NULL,
                           rentable          BOOLEAN       NOT NULL DEFAULT FALSE,
                           activo            BOOLEAN       NOT NULL DEFAULT TRUE,
                           CONSTRAINT pk_productos PRIMARY KEY (id_producto),
                           CONSTRAINT uq_productos_codigo_interno UNIQUE (codigo_interno),
                           CONSTRAINT fk_productos_categoria  FOREIGN KEY (id_categoria)  REFERENCES categorias(id_categoria)   ON UPDATE CASCADE ON DELETE RESTRICT,
                           CONSTRAINT fk_productos_plataforma FOREIGN KEY (id_plataforma) REFERENCES plataformas(id_plataforma) ON UPDATE CASCADE ON DELETE SET NULL,
                           CONSTRAINT fk_productos_genero     FOREIGN KEY (id_genero)     REFERENCES generos(id_genero)         ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE ejemplares (
                            id_ejemplar       CHAR(36)      NOT NULL DEFAULT (UUID()),
                            id_producto       CHAR(36)      NOT NULL,
                            numero_serie      VARCHAR(60)   NOT NULL,
                            condicion         ENUM('nuevo','usado') NOT NULL DEFAULT 'nuevo',
                            costo             DECIMAL(10,2) NOT NULL,
                            precio_venta      DECIMAL(10,2) NOT NULL,
                            estado            ENUM('disponible','rentado','vendido','apartado','reservado','baja') NOT NULL DEFAULT 'disponible',
                            tiene_caja        BOOLEAN       NOT NULL DEFAULT TRUE,
                            tiene_manual      BOOLEAN       NOT NULL DEFAULT TRUE,
                            observaciones     VARCHAR(255)  NULL,
                            fecha_ingreso     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            id_cliente_origen CHAR(36)      NULL,
                            CONSTRAINT pk_ejemplares PRIMARY KEY (id_ejemplar),
                            CONSTRAINT uq_ejemplares_serie UNIQUE (numero_serie),
                            CONSTRAINT fk_ejemplares_producto FOREIGN KEY (id_producto)       REFERENCES productos(id_producto) ON UPDATE CASCADE ON DELETE RESTRICT,
                            CONSTRAINT fk_ejemplares_cliente  FOREIGN KEY (id_cliente_origen) REFERENCES clientes(id_cliente)  ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- =====================================================================
-- 4. PROVEEDORES Y COMPRAS
-- =====================================================================

CREATE TABLE proveedores (
                             id_proveedor CHAR(36)     NOT NULL DEFAULT (UUID()),
                             nombre       VARCHAR(150) NOT NULL,
                             contacto     VARCHAR(150) NULL,
                             telefono     VARCHAR(20)  NULL,
                             correo       VARCHAR(150) NULL,
                             direccion    VARCHAR(255) NULL,
                             activo       BOOLEAN      NOT NULL DEFAULT TRUE,
                             CONSTRAINT pk_proveedores PRIMARY KEY (id_proveedor)
) ENGINE=InnoDB;

CREATE TABLE compras (
                         id_compra    CHAR(36) NOT NULL DEFAULT (UUID()),
                         id_proveedor CHAR(36) NOT NULL,
                         id_usuario   CHAR(36) NOT NULL,
                         fecha_compra DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         estado       ENUM('pendiente','recibida','cancelada') NOT NULL DEFAULT 'pendiente',
                         CONSTRAINT pk_compras PRIMARY KEY (id_compra),
                         CONSTRAINT fk_compras_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor) ON UPDATE CASCADE ON DELETE RESTRICT,
                         CONSTRAINT fk_compras_usuario   FOREIGN KEY (id_usuario)   REFERENCES usuarios(id_usuario)       ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE detalle_compras (
                                 id_detalle_compra CHAR(36)      NOT NULL DEFAULT (UUID()),
                                 id_compra         CHAR(36)      NOT NULL,
                                 id_producto       CHAR(36)      NOT NULL,
                                 cantidad          INT           NOT NULL,
                                 costo_unitario    DECIMAL(10,2) NOT NULL,
                                 subtotal          DECIMAL(10,2) GENERATED ALWAYS AS (cantidad * costo_unitario) VIRTUAL,
                                 CONSTRAINT pk_detalle_compras PRIMARY KEY (id_detalle_compra),
                                 CONSTRAINT fk_detcompra_compra   FOREIGN KEY (id_compra)   REFERENCES compras(id_compra)     ON UPDATE CASCADE ON DELETE CASCADE,
                                 CONSTRAINT fk_detcompra_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON UPDATE CASCADE ON DELETE RESTRICT,
                                 CONSTRAINT chk_detcompra_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB;

-- Enlace 1:N entre una linea de compra y los ejemplares que genera.
-- UNIQUE(id_ejemplar) garantiza que cada ejemplar provenga de una sola linea.
CREATE TABLE detalle_compra_ejemplar (
                                         id_detalle_compra CHAR(36) NOT NULL,
                                         id_ejemplar       CHAR(36) NOT NULL,
                                         CONSTRAINT pk_detalle_compra_ejemplar PRIMARY KEY (id_detalle_compra, id_ejemplar),
                                         CONSTRAINT uq_dce_ejemplar UNIQUE (id_ejemplar),
                                         CONSTRAINT fk_dce_detalle  FOREIGN KEY (id_detalle_compra) REFERENCES detalle_compras(id_detalle_compra) ON UPDATE CASCADE ON DELETE CASCADE,
                                         CONSTRAINT fk_dce_ejemplar FOREIGN KEY (id_ejemplar)       REFERENCES ejemplares(id_ejemplar)             ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE compras_usado (
                               id_compra_usado CHAR(36)      NOT NULL DEFAULT (UUID()),
                               id_cliente      CHAR(36)      NOT NULL,
                               id_usuario      CHAR(36)      NOT NULL,
                               id_ejemplar     CHAR(36)      NOT NULL,
                               fecha_compra    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               precio_compra   DECIMAL(10,2) NOT NULL,
                               observaciones   VARCHAR(255)  NULL,
                               CONSTRAINT pk_compras_usado PRIMARY KEY (id_compra_usado),
                               CONSTRAINT uq_comprausado_ejemplar UNIQUE (id_ejemplar),
                               CONSTRAINT fk_comprausado_cliente  FOREIGN KEY (id_cliente)  REFERENCES clientes(id_cliente)   ON UPDATE CASCADE ON DELETE RESTRICT,
                               CONSTRAINT fk_comprausado_usuario  FOREIGN KEY (id_usuario)  REFERENCES usuarios(id_usuario)   ON UPDATE CASCADE ON DELETE RESTRICT,
                               CONSTRAINT fk_comprausado_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplares(id_ejemplar) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =====================================================================
-- 5. VENTAS (POS)
-- =====================================================================

CREATE TABLE ventas (
                        id_venta    CHAR(36)      NOT NULL DEFAULT (UUID()),
                        folio       VARCHAR(30)   NOT NULL,
                        id_cliente  CHAR(36)      NOT NULL,
                        id_usuario  CHAR(36)      NOT NULL,
                        fecha_venta DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        descuento   DECIMAL(10,2) NOT NULL DEFAULT 0,
                        impuestos   DECIMAL(10,2) NOT NULL DEFAULT 0,
                        estado      ENUM('completada','cancelada') NOT NULL DEFAULT 'completada',
                        CONSTRAINT pk_ventas PRIMARY KEY (id_venta),
                        CONSTRAINT uq_ventas_folio UNIQUE (folio),
                        CONSTRAINT fk_ventas_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente) ON UPDATE CASCADE ON DELETE RESTRICT,
                        CONSTRAINT fk_ventas_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE detalle_ventas (
                                id_detalle_venta CHAR(36)      NOT NULL DEFAULT (UUID()),
                                id_venta         CHAR(36)      NOT NULL,
                                id_ejemplar      CHAR(36)      NOT NULL,
                                cantidad         INT           NOT NULL DEFAULT 1,
                                precio_unitario  DECIMAL(10,2) NOT NULL,
                                subtotal         DECIMAL(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) VIRTUAL,
                                CONSTRAINT pk_detalle_ventas PRIMARY KEY (id_detalle_venta),
                                CONSTRAINT fk_detventa_venta    FOREIGN KEY (id_venta)    REFERENCES ventas(id_venta)       ON UPDATE CASCADE ON DELETE CASCADE,
                                CONSTRAINT fk_detventa_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplares(id_ejemplar) ON UPDATE CASCADE ON DELETE RESTRICT,
                                CONSTRAINT uq_detventa_venta_ejemplar UNIQUE (id_venta, id_ejemplar),
                                CONSTRAINT chk_detventa_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB;

-- =====================================================================
-- 6. RENTAS
-- =====================================================================

CREATE TABLE rentas (
                        id_renta          CHAR(36)      NOT NULL DEFAULT (UUID()),
                        id_cliente        CHAR(36)      NOT NULL,
                        id_usuario        CHAR(36)      NOT NULL,
                        id_ejemplar       CHAR(36)      NOT NULL,
                        fecha_renta       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        fecha_limite      DATE          NOT NULL,
                        fecha_devolucion  DATETIME      NULL,
                        monto_renta       DECIMAL(10,2) NOT NULL,
                        deposito          DECIMAL(10,2) NOT NULL DEFAULT 0,
                        monto_extra       DECIMAL(10,2) NOT NULL DEFAULT 0,
                        condicion_retorno ENUM('buena','con_desgaste','danado','incompleto') NULL,
                        estado            ENUM('activa','devuelta','vencida','cancelada') NOT NULL DEFAULT 'activa',
                        CONSTRAINT pk_rentas PRIMARY KEY (id_renta),
                        CONSTRAINT fk_rentas_cliente  FOREIGN KEY (id_cliente)  REFERENCES clientes(id_cliente)   ON UPDATE CASCADE ON DELETE RESTRICT,
                        CONSTRAINT fk_rentas_usuario  FOREIGN KEY (id_usuario)  REFERENCES usuarios(id_usuario)   ON UPDATE CASCADE ON DELETE RESTRICT,
                        CONSTRAINT fk_rentas_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplares(id_ejemplar) ON UPDATE CASCADE ON DELETE RESTRICT,
                        CONSTRAINT chk_renta_devuelta CHECK (estado <> 'devuelta' OR fecha_devolucion IS NOT NULL),
                        CONSTRAINT chk_renta_fechas   CHECK (fecha_devolucion IS NULL OR fecha_devolucion >= fecha_renta)
) ENGINE=InnoDB;

-- =====================================================================
-- 7. APARTADOS
-- =====================================================================

CREATE TABLE apartados (
                           id_apartado      CHAR(36)      NOT NULL DEFAULT (UUID()),
                           folio            VARCHAR(30)   NOT NULL,
                           id_cliente       CHAR(36)      NOT NULL,
                           id_usuario       CHAR(36)      NOT NULL,
                           id_ejemplar      CHAR(36)      NOT NULL,
                           fecha_inicio     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           fecha_limite     DATE          NOT NULL,
                           importe_anticipo DECIMAL(10,2) NOT NULL DEFAULT 0,
                           estado           ENUM('activo','liquidado','cancelado','vencido') NOT NULL DEFAULT 'activo',
                           CONSTRAINT pk_apartados PRIMARY KEY (id_apartado),
                           CONSTRAINT uq_apartados_folio UNIQUE (folio),
                           CONSTRAINT fk_apartados_cliente  FOREIGN KEY (id_cliente)  REFERENCES clientes(id_cliente)   ON UPDATE CASCADE ON DELETE RESTRICT,
                           CONSTRAINT fk_apartados_usuario  FOREIGN KEY (id_usuario)  REFERENCES usuarios(id_usuario)   ON UPDATE CASCADE ON DELETE RESTRICT,
                           CONSTRAINT fk_apartados_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplares(id_ejemplar) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =====================================================================
-- 8. E-COMMERCE Y PEDIDOS
-- =====================================================================

CREATE TABLE pedidos (
                         id_pedido     CHAR(36)      NOT NULL DEFAULT (UUID()),
                         id_cliente    CHAR(36)      NOT NULL,
                         fecha_pedido  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         descuento     DECIMAL(10,2) NOT NULL DEFAULT 0,
                         impuestos     DECIMAL(10,2) NOT NULL DEFAULT 0,
                         costo_envio   DECIMAL(10,2) NOT NULL DEFAULT 0,
                         paqueteria    VARCHAR(100)  NULL,
                         numero_guia   VARCHAR(100)  NULL,
                         fecha_envio   DATETIME      NULL,
                         fecha_entrega DATETIME      NULL,
                         estado        ENUM('pendiente_pago','pagado','preparando_envio','enviado','entregado','cancelado','reembolsado') NOT NULL DEFAULT 'pendiente_pago',
                         CONSTRAINT pk_pedidos PRIMARY KEY (id_pedido),
                         CONSTRAINT fk_pedidos_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente) ON UPDATE CASCADE ON DELETE RESTRICT,
                         CONSTRAINT chk_pedido_envio   CHECK (estado NOT IN ('enviado','entregado') OR fecha_envio IS NOT NULL),
                         CONSTRAINT chk_pedido_entrega CHECK (fecha_entrega IS NULL OR fecha_envio IS NOT NULL),
                         CONSTRAINT chk_pedido_fechas  CHECK (fecha_entrega IS NULL OR fecha_entrega >= fecha_envio)
) ENGINE=InnoDB;

-- Direccion de envio del pedido (1:1). Se separa de pedidos porque guardar
-- id_cliente e id_direccion juntos permitiria la FD id_direccion -> id_cliente,
-- cuyo determinante no es superclave de pedidos (violacion de BCNF).
CREATE TABLE pedido_envio (
                              id_pedido    CHAR(36) NOT NULL,
                              id_direccion CHAR(36) NOT NULL,
                              CONSTRAINT pk_pedido_envio PRIMARY KEY (id_pedido),
                              CONSTRAINT fk_pedenvio_pedido    FOREIGN KEY (id_pedido)    REFERENCES pedidos(id_pedido)                 ON UPDATE CASCADE ON DELETE CASCADE,
                              CONSTRAINT fk_pedenvio_direccion FOREIGN KEY (id_direccion) REFERENCES direcciones_cliente(id_direccion) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE detalle_pedidos (
                                 id_detalle_pedido CHAR(36)      NOT NULL DEFAULT (UUID()),
                                 id_pedido         CHAR(36)      NOT NULL,
                                 id_ejemplar       CHAR(36)      NOT NULL,
                                 cantidad          INT           NOT NULL DEFAULT 1,
                                 precio_unitario   DECIMAL(10,2) NOT NULL,
                                 subtotal          DECIMAL(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) VIRTUAL,
                                 CONSTRAINT pk_detalle_pedidos PRIMARY KEY (id_detalle_pedido),
                                 CONSTRAINT fk_detpedido_pedido   FOREIGN KEY (id_pedido)   REFERENCES pedidos(id_pedido)     ON UPDATE CASCADE ON DELETE CASCADE,
                                 CONSTRAINT fk_detpedido_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplares(id_ejemplar) ON UPDATE CASCADE ON DELETE RESTRICT,
                                 CONSTRAINT uq_detpedido_ejemplar UNIQUE (id_ejemplar),
                                 CONSTRAINT chk_detpedido_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB;

-- =====================================================================
-- 9. PAGOS
-- =====================================================================

-- pagos: caja unica para cobros y reembolsos de cualquier operacion.
--   tipo_movimiento: 'cobro' (entra) | 'reembolso' (sale).
--   concepto: naturaleza del movimiento; 'deposito' es garantia (no ingreso).
--   monto siempre positivo: el signo lo da tipo_movimiento.
CREATE TABLE pagos (
                       id_pago         CHAR(36)      NOT NULL DEFAULT (UUID()),
                       id_venta        CHAR(36)      NULL,
                       id_pedido       CHAR(36)      NULL,
                       id_renta        CHAR(36)      NULL,
                       id_apartado     CHAR(36)      NULL,
                       monto           DECIMAL(10,2) NOT NULL,
                       tipo_movimiento ENUM('cobro','reembolso') NOT NULL DEFAULT 'cobro',
                       concepto        ENUM('venta','pedido','renta','deposito','recargo','apartado') NOT NULL,
                       metodo_pago     ENUM('efectivo','tarjeta','transferencia','otro') NOT NULL DEFAULT 'efectivo',
                       fecha_pago      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT pk_pagos PRIMARY KEY (id_pago),
                       CONSTRAINT fk_pagos_venta    FOREIGN KEY (id_venta)    REFERENCES ventas(id_venta)       ON UPDATE RESTRICT ON DELETE RESTRICT,
                       CONSTRAINT fk_pagos_pedido   FOREIGN KEY (id_pedido)   REFERENCES pedidos(id_pedido)     ON UPDATE RESTRICT ON DELETE RESTRICT,
                       CONSTRAINT fk_pagos_renta    FOREIGN KEY (id_renta)    REFERENCES rentas(id_renta)       ON UPDATE RESTRICT ON DELETE RESTRICT,
                       CONSTRAINT fk_pagos_apartado FOREIGN KEY (id_apartado) REFERENCES apartados(id_apartado) ON UPDATE RESTRICT ON DELETE RESTRICT,
                       CONSTRAINT chk_pagos_una_operacion CHECK (
                           (id_venta IS NOT NULL) + (id_pedido IS NOT NULL) + (id_renta IS NOT NULL) + (id_apartado IS NOT NULL) = 1
                           ),
                       CONSTRAINT chk_pagos_monto CHECK (monto >= 0),
                       CONSTRAINT chk_pagos_concepto CHECK (
                           (concepto = 'venta'    AND id_venta    IS NOT NULL) OR
                           (concepto = 'pedido'   AND id_pedido   IS NOT NULL) OR
                           (concepto IN ('renta','deposito','recargo') AND id_renta IS NOT NULL) OR
                           (concepto = 'apartado' AND id_apartado IS NOT NULL)
                           )
) ENGINE=InnoDB;

-- =====================================================================
-- 10. MOVIMIENTOS DE INVENTARIO
-- =====================================================================

CREATE TABLE movimientos_inventario (
                                        id_movimiento    CHAR(36)     NOT NULL DEFAULT (UUID()),
                                        id_ejemplar      CHAR(36)     NOT NULL,
                                        id_usuario         CHAR(36)     NOT NULL,
                                        tipo_movimiento  ENUM('entrada','salida','ajuste','baja') NOT NULL,
                                        motivo           ENUM('compra_proveedor','compra_usado','devolucion_cliente',
                                            'venta','renta','apartado','reserva','danio','cancelacion','ajuste_manual') NOT NULL,
                                        cantidad         INT          NOT NULL DEFAULT 1,
                                        fecha_movimiento DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        observaciones    VARCHAR(255) NULL,
                                        CONSTRAINT pk_movimientos_inventario PRIMARY KEY (id_movimiento),
                                        CONSTRAINT fk_movinv_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplares(id_ejemplar) ON UPDATE CASCADE ON DELETE RESTRICT,
                                        CONSTRAINT fk_movinv_usuario  FOREIGN KEY (id_usuario)  REFERENCES usuarios(id_usuario)   ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =====================================================================
-- 11. PROMOCIONES
-- =====================================================================

CREATE TABLE promociones (
                             id_promocion        CHAR(36)      NOT NULL DEFAULT (UUID()),
                             nombre              VARCHAR(150)  NOT NULL,
                             descripcion         VARCHAR(255)  NULL,
                             tipo_descuento      ENUM('porcentaje','monto_fijo') NOT NULL DEFAULT 'porcentaje',
                             valor_descuento     DECIMAL(10,2) NOT NULL,
                             fecha_inicio        DATE          NOT NULL,
                             fecha_fin           DATE          NOT NULL,
                             id_usuario          CHAR(36)      NOT NULL,
                             id_usuario_autoriza CHAR(36)      NULL,
                             activo              BOOLEAN       NOT NULL DEFAULT TRUE,
                             CONSTRAINT pk_promociones PRIMARY KEY (id_promocion),
                             CONSTRAINT fk_promo_usuario  FOREIGN KEY (id_usuario)          REFERENCES usuarios(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
                             CONSTRAINT fk_promo_autoriza FOREIGN KEY (id_usuario_autoriza) REFERENCES usuarios(id_usuario) ON UPDATE CASCADE ON DELETE SET NULL,
                             CONSTRAINT chk_promo_fechas  CHECK (fecha_fin >= fecha_inicio),
                             CONSTRAINT chk_promo_valor   CHECK ((tipo_descuento = 'porcentaje' AND valor_descuento BETWEEN 0 AND 100)
                                                                 OR (tipo_descuento = 'monto_fijo' AND valor_descuento >= 0))
) ENGINE=InnoDB;

-- Alcance de la promocion. id_referencia es polimorfico (producto/categoria/
-- plataforma), por lo que no admite FK directa: la integridad se valida en la
-- aplicacion. Se evita asi almacenar los tres alcances en promociones.
CREATE TABLE promocion_alcance (
                                   id_promocion  CHAR(36) NOT NULL,
                                   tipo_alcance  ENUM('producto','categoria','plataforma') NOT NULL,
                                   id_referencia CHAR(36) NOT NULL,
                                   CONSTRAINT pk_promocion_alcance PRIMARY KEY (id_promocion, tipo_alcance, id_referencia),
                                   CONSTRAINT fk_alcance_promocion FOREIGN KEY (id_promocion) REFERENCES promociones(id_promocion) ON UPDATE CASCADE ON DELETE CASCADE,
                                   KEY idx_alcance_referencia (tipo_alcance, id_referencia)
) ENGINE=InnoDB;

-- =====================================================================
-- 12. DEVOLUCIONES Y GARANTÍAS
-- =====================================================================

CREATE TABLE devoluciones_garantia (
                                       id_devolucion      CHAR(36)      NOT NULL DEFAULT (UUID()),
                                       id_detalle_venta   CHAR(36)      NOT NULL,
                                       fecha_devolucion   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       motivo             VARCHAR(150)  NOT NULL,
                                       condicion_recibida ENUM('buena','con_desgaste','danado','incompleto') NOT NULL DEFAULT 'buena',
                                       estado             ENUM('solicitada','aprobada','rechazada','reembolsada','cambiada','saldo_favor') NOT NULL DEFAULT 'solicitada',
                                       monto_reembolso    DECIMAL(10,2) NOT NULL DEFAULT 0,
                                       id_usuario         CHAR(36)      NOT NULL,
                                       CONSTRAINT pk_devoluciones_garantia PRIMARY KEY (id_devolucion),
                                       CONSTRAINT uq_devgar_detalle_venta UNIQUE (id_detalle_venta),
                                       CONSTRAINT fk_devgar_detalle_venta FOREIGN KEY (id_detalle_venta) REFERENCES detalle_ventas(id_detalle_venta) ON UPDATE CASCADE ON DELETE RESTRICT,
                                       CONSTRAINT fk_devgar_usuario       FOREIGN KEY (id_usuario)       REFERENCES usuarios(id_usuario)             ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =====================================================================
-- 13. CONFIGURACIÓN
-- =====================================================================

-- Parametros de negocio clave/valor (p. ej. recargo_por_dia de rentas).
-- Politica global: si la tarifa cambia, las rentas viejas se recalculan con la
-- nueva (decision consciente; ver docs/database-structure.md).
CREATE TABLE configuracion (
                               clave               VARCHAR(50)  NOT NULL,
                               valor               VARCHAR(255) NOT NULL,
                               descripcion         VARCHAR(255) NULL,
                               fecha_actualizacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               id_usuario          CHAR(36)     NULL,
                               CONSTRAINT pk_configuracion PRIMARY KEY (clave),
                               CONSTRAINT fk_config_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- =====================================================================
-- 14. TRIGGERS DE COHERENCIA
-- =====================================================================

DELIMITER $$

-- Solo los videojuegos pueden tener genero y clasificacion.
DROP TRIGGER IF EXISTS trg_productos_genero_ins$$
CREATE TRIGGER trg_productos_genero_ins BEFORE INSERT ON productos
FOR EACH ROW
BEGIN
    IF (NEW.id_genero IS NOT NULL OR NEW.clasificacion IS NOT NULL)
       AND (SELECT tipo FROM categorias WHERE id_categoria = NEW.id_categoria) <> 'videojuego' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo videojuegos pueden tener genero/clasificacion';
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_productos_genero_upd$$
CREATE TRIGGER trg_productos_genero_upd BEFORE UPDATE ON productos
FOR EACH ROW
BEGIN
    IF (NEW.id_genero IS NOT NULL OR NEW.clasificacion IS NOT NULL)
       AND (SELECT tipo FROM categorias WHERE id_categoria = NEW.id_categoria) <> 'videojuego' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo videojuegos pueden tener genero/clasificacion';
    END IF;
END$$

DELIMITER ;