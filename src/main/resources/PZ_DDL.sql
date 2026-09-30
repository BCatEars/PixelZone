-- =====================================================================
-- BASE DE DATOS: PIXEL ZONE
-- Proyecto escolar - Esquema simplificado y revisado (3FN)
-- Reglas: SIN AUTO_INCREMENT, SIN ALTER TABLE
--         PK CHAR(36) DEFAULT (UUID())
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

CREATE TABLE productos (
                           id_producto       CHAR(36)      NOT NULL DEFAULT (UUID()),
                           id_categoria      CHAR(36)      NOT NULL,
                           id_plataforma     CHAR(36)      NULL,
                           codigo_interno    VARCHAR(30)   NOT NULL,
                           codigo_barras     VARCHAR(60)   NULL,
                           nombre            VARCHAR(150)  NOT NULL,
                           descripcion       TEXT          NULL,
                           tipo              ENUM('videojuego','consola','accesorio','otro') NOT NULL DEFAULT 'videojuego',
                           genero            VARCHAR(80)   NULL,
                           clasificacion     VARCHAR(10)   NULL,
                           edicion           VARCHAR(100)  NULL,
                           fecha_lanzamiento DATE          NULL,
                           precio_nuevo      DECIMAL(10,2) NULL,
                           precio_usado      DECIMAL(10,2) NULL,
                           rentable          BOOLEAN       NOT NULL DEFAULT FALSE,
                           activo            BOOLEAN       NOT NULL DEFAULT TRUE,
                           CONSTRAINT pk_productos PRIMARY KEY (id_producto),
                           CONSTRAINT uq_productos_codigo_interno UNIQUE (codigo_interno),
                           CONSTRAINT fk_productos_categoria  FOREIGN KEY (id_categoria)  REFERENCES categorias(id_categoria)   ON UPDATE CASCADE ON DELETE RESTRICT,
                           CONSTRAINT fk_productos_plataforma FOREIGN KEY (id_plataforma) REFERENCES plataformas(id_plataforma) ON UPDATE CASCADE ON DELETE SET NULL
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
                                CONSTRAINT uq_detventa_ejemplar UNIQUE (id_ejemplar),
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
                        CONSTRAINT fk_rentas_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplares(id_ejemplar) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =====================================================================
-- 7. APARTADOS
-- =====================================================================

CREATE TABLE apartados (
                           id_apartado      CHAR(36)      NOT NULL DEFAULT (UUID()),
                           folio            VARCHAR(30)   NOT NULL,
                           id_cliente       CHAR(36)      NOT NULL,
                           id_ejemplar      CHAR(36)      NOT NULL,
                           fecha_inicio     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           fecha_limite     DATE          NOT NULL,
                           importe_anticipo DECIMAL(10,2) NOT NULL DEFAULT 0,
                           estado           ENUM('activo','liquidado','cancelado','vencido') NOT NULL DEFAULT 'activo',
                           CONSTRAINT pk_apartados PRIMARY KEY (id_apartado),
                           CONSTRAINT uq_apartados_folio UNIQUE (folio),
                           CONSTRAINT fk_apartados_cliente  FOREIGN KEY (id_cliente)  REFERENCES clientes(id_cliente)   ON UPDATE CASCADE ON DELETE RESTRICT,
                           CONSTRAINT fk_apartados_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplares(id_ejemplar) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =====================================================================
-- 8. E-COMMERCE Y PEDIDOS
-- =====================================================================

CREATE TABLE pedidos (
                         id_pedido     CHAR(36)      NOT NULL DEFAULT (UUID()),
                         id_direccion  CHAR(36)      NOT NULL,
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
                         CONSTRAINT fk_pedidos_direccion FOREIGN KEY (id_direccion) REFERENCES direcciones_cliente(id_direccion) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE detalle_pedidos (
                                 id_detalle_pedido CHAR(36)      NOT NULL DEFAULT (UUID()),
                                 id_pedido         CHAR(36)      NOT NULL,
                                 id_producto       CHAR(36)      NOT NULL,
                                 cantidad          INT           NOT NULL DEFAULT 1,
                                 precio_unitario   DECIMAL(10,2) NOT NULL,
                                 subtotal          DECIMAL(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) VIRTUAL,
                                 CONSTRAINT pk_detalle_pedidos PRIMARY KEY (id_detalle_pedido),
                                 CONSTRAINT fk_detpedido_pedido   FOREIGN KEY (id_pedido)   REFERENCES pedidos(id_pedido)     ON UPDATE CASCADE ON DELETE CASCADE,
                                 CONSTRAINT fk_detpedido_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON UPDATE CASCADE ON DELETE RESTRICT,
                                 CONSTRAINT chk_detpedido_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB;

-- =====================================================================
-- 9. PAGOS
-- =====================================================================

CREATE TABLE pagos (
                       id_pago     CHAR(36)      NOT NULL DEFAULT (UUID()),
                       id_venta    CHAR(36)      NULL,
                       id_pedido   CHAR(36)      NULL,
                       id_renta    CHAR(36)      NULL,
                       id_apartado CHAR(36)      NULL,
                       monto       DECIMAL(10,2) NOT NULL,
                       metodo_pago ENUM('efectivo','tarjeta','transferencia','otro') NOT NULL DEFAULT 'efectivo',
                       fecha_pago  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT pk_pagos PRIMARY KEY (id_pago),
                       CONSTRAINT fk_pagos_venta    FOREIGN KEY (id_venta)    REFERENCES ventas(id_venta)       ON UPDATE RESTRICT ON DELETE RESTRICT,
                       CONSTRAINT fk_pagos_pedido   FOREIGN KEY (id_pedido)   REFERENCES pedidos(id_pedido)     ON UPDATE RESTRICT ON DELETE RESTRICT,
                       CONSTRAINT fk_pagos_renta    FOREIGN KEY (id_renta)    REFERENCES rentas(id_renta)       ON UPDATE RESTRICT ON DELETE RESTRICT,
                       CONSTRAINT fk_pagos_apartado FOREIGN KEY (id_apartado) REFERENCES apartados(id_apartado) ON UPDATE RESTRICT ON DELETE RESTRICT,
                       CONSTRAINT chk_pagos_una_operacion CHECK (
                           (id_venta IS NOT NULL) + (id_pedido IS NOT NULL) + (id_renta IS NOT NULL) + (id_apartado IS NOT NULL) = 1
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
                             id_producto         CHAR(36)      NULL,
                             id_categoria        CHAR(36)      NULL,
                             id_plataforma       CHAR(36)      NULL,
                             id_usuario          CHAR(36)      NOT NULL,
                             id_usuario_autoriza CHAR(36)      NULL,
                             activo              BOOLEAN       NOT NULL DEFAULT TRUE,
                             CONSTRAINT pk_promociones PRIMARY KEY (id_promocion),
                             CONSTRAINT fk_promo_producto   FOREIGN KEY (id_producto)         REFERENCES productos(id_producto)   ON UPDATE CASCADE ON DELETE SET NULL,
                             CONSTRAINT fk_promo_categoria  FOREIGN KEY (id_categoria)        REFERENCES categorias(id_categoria) ON UPDATE CASCADE ON DELETE SET NULL,
                             CONSTRAINT fk_promo_plataforma FOREIGN KEY (id_plataforma)       REFERENCES plataformas(id_plataforma) ON UPDATE CASCADE ON DELETE SET NULL,
                             CONSTRAINT fk_promo_usuario    FOREIGN KEY (id_usuario)          REFERENCES usuarios(id_usuario)     ON UPDATE CASCADE ON DELETE RESTRICT,
                             CONSTRAINT fk_promo_autoriza   FOREIGN KEY (id_usuario_autoriza) REFERENCES usuarios(id_usuario)     ON UPDATE CASCADE ON DELETE SET NULL
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