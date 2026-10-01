# PIXEL ZONE — Análisis del esquema de base de datos

Análisis del script `PZ_DDL.sql` (raíz del proyecto): estructura, relaciones,
normalización y coherencia con la aplicación Java.

> **Alcance:** el esquema vive en tres scripts dentro de `src/main/resources/`:
> `PZ_DDL.sql` (29 `CREATE TABLE` y 2 triggers, motor InnoDB, charset
> `utf8mb4` / colación `utf8mb4_unicode_ci`), `PZ_DML.sql` (datos semilla) y
> `PZ_PL.sql` (5 vistas, 5 procedimientos y 5 funciones). El análisis de las tablas de abajo
> corresponde a `PZ_DDL.sql`; la coherencia con la aplicación se detalla en la
> sección 6.

---

## 1. Reglas de diseño declaradas

El encabezado del script fija reglas deliberadas (requisitos de la asignatura):

- `CREATE DATABASE IF NOT EXISTS pixel_zone` (un solo guion bajo).
- **Sin `AUTO_INCREMENT`.** Todas las PK son `CHAR(36)` con
  `DEFAULT (UUID())` (UUID generado por MySQL 8.0.13+).
- **Sin `ALTER TABLE`.** Todo se declara en el `CREATE TABLE`.
- Claves foráneas con política explícita `ON UPDATE` / `ON DELETE`.
- Uso intensivo de `ENUM` para estados y tipos.
- **Encoding utf8mb4 de extremo a extremo.** Los scripts deben cargarse con
  `mysql --default-character-set=utf8mb4` y el JDBC usa
  `characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci`. Sin el flag
  del cliente, los acentos del seed se guardan doble-codificados.

---

## 2. Inventario de tablas por dominio

### 2.1 Seguridad y usuarios (4)
| Tabla             | PK                        | Notas                                              |
|-------------------|---------------------------|----------------------------------------------------|
| `perfiles`        | `id_perfil`               | `nombre` UNIQUE                                    |
| `permisos`        | `id_permiso`              | `nombre` UNIQUE                                    |
| `perfil_permisos` | `(id_perfil, id_permiso)` | **PK compuesta**; tabla puente N:M pura            |
| `usuarios`        | `id_usuario`              | FK → `perfiles`; `nombre_usuario` UNIQUE; `activo` |

### 2.2 Clientes (2)
| Tabla                 | PK             | Notas                                               |
|-----------------------|----------------|-----------------------------------------------------|
| `clientes`            | `id_cliente`   | `tipo_cliente` ENUM(registrado, mostrador, anonimo) |
| `direcciones_cliente` | `id_direccion` | FK → `clientes` (1:N); `es_principal`               |

### 2.3 Catálogo e inventario (5)
| Tabla         | PK              | Notas                                                                                          |
|---------------|-----------------|------------------------------------------------------------------------------------------------|
| `categorias`  | `id_categoria`  | `nombre` UNIQUE; **`tipo`** ENUM(videojuego, consola, accesorio, otro)                          |
| `plataformas` | `id_plataforma` | `nombre` UNIQUE                                                                                 |
| `generos`     | `id_genero`     | `nombre` UNIQUE; temática de videojuego (Acción, Aventura, RPG…)                                |
| `productos`   | `id_producto`   | FK categoría (RESTRICT) y plataforma/género (SET NULL); `clasificacion` ENUM ESRB; precios      |
| `ejemplares`  | `id_ejemplar`   | **Unidad física**; FK producto y cliente de origen; `numero_serie` UNIQUE; condición/estado     |

> **`tipo` vive en `categorias`, no en `productos`.** Antes `productos.tipo` era
> una dependencia transitiva (`id_producto → id_categoria → tipo`) que violaba
> 3FN. Ahora el tipo se deriva por JOIN desde `categorias.tipo` (ver §5).

### 2.4 Proveedores y compras (5)
| Tabla                     | PK                                 | Notas                                                         |
|---------------------------|------------------------------------|---------------------------------------------------------------|
| `proveedores`             | `id_proveedor`                     | datos de contacto                                             |
| `compras`                 | `id_compra`                        | encabezado; FK proveedor y usuario                            |
| `detalle_compras`         | `id_detalle_compra`                | líneas; `subtotal` **columna generada virtual**               |
| `detalle_compra_ejemplar` | `(id_detalle_compra, id_ejemplar)` | enlace 1:N línea→ejemplar; `UNIQUE(id_ejemplar)`              |
| `compras_usado`           | `id_compra_usado`                  | compra de usados a cliente; `id_ejemplar` UNIQUE              |

### 2.5 Ventas (2)
| Tabla            | PK                 | Notas                                                                                     |
|------------------|--------------------|-------------------------------------------------------------------------------------------|
| `ventas`         | `id_venta`         | encabezado; `folio` UNIQUE; descuento/impuestos separados                                 |
| `detalle_ventas` | `id_detalle_venta` | líneas; **`UNIQUE (id_venta, id_ejemplar)`** evita duplicar la copia en la misma venta; `subtotal` generado |

### 2.6 Rentas y apartados (2)
| Tabla       | PK            | Notas                                                                                  |
|-------------|---------------|----------------------------------------------------------------------------------------|
| `rentas`    | `id_renta`    | fechas, montos, condición de retorno, estado; `CHECK` de devolución y de fechas         |
| `apartados` | `id_apartado` | `folio` UNIQUE, anticipo, estado; **`id_usuario`** (quién lo creó, además del kardex)   |

### 2.7 E-commerce (3)
| Tabla             | PK                  | Notas                                                        |
|-------------------|---------------------|--------------------------------------------------------------|
| `pedidos`         | `id_pedido`         | FK → `clientes`; envío/guía/fechas/estado; `CHECK` de envío   |
| `pedido_envio`    | `id_pedido`         | **1:1** con `pedidos`; FK → `direcciones_cliente`             |
| `detalle_pedidos` | `id_detalle_pedido` | líneas; **`id_ejemplar`** (UNIQUE); `subtotal` generado       |

> **`pedidos.id_direccion` se separó a `pedido_envio`.** Guardar `id_cliente`
> e `id_direccion` en `pedidos` permitiría la FD `id_direccion → id_cliente`,
> cuyo determinante no es superclave → violación de BCNF (ver §5).

### 2.8 Transversales (5)
| Tabla                    | PK              | Notas                                                                                      |
|--------------------------|-----------------|--------------------------------------------------------------------------------------------|
| `pagos`                  | `id_pago`       | **Arco exclusivo** (4 FK + `CHECK` de exactamente 1), `tipo_movimiento`, `concepto`         |
| `movimientos_inventario` | `id_movimiento` | Libro mayor (kardex): tipo/motivo/cantidad                                                 |
| `promociones`            | `id_promocion`  | Datos generales; `CHECK` de fechas y de valor                                              |
| `promocion_alcance`      | `(id_promocion, tipo_alcance, id_referencia)` | Alcance polimórfico: producto / categoría / plataforma        |
| `devoluciones_garantia`  | `id_devolucion` | Ligada a `detalle_ventas` (UNIQUE), con estado y reembolso                                  |

### 2.9 Configuración (1)
| Tabla           | PK (natural) | Notas                                                                 |
|-----------------|--------------|-----------------------------------------------------------------------|
| `configuracion` | `clave`      | Parámetros de negocio clave/valor (p. ej. `recargo_por_dia` de rentas) |

**Total: 29 tablas.** Perfil claramente transaccional (POS) con soporte de
inventario, rentas, apartados, e-commerce, promociones y devoluciones.

---

## 3. Relaciones principales (mapa lógico)

```
perfiles 1──N usuarios ─┬─N compras ──N detalle_compras ──> productos
   │ N:M (perfil_permisos)│                                  │
permisos                   ├─N ventas ──N detalle_ventas ──> ejemplares ──> productos
                           ├─N rentas ─────────────────────> ejemplares
                           ├─N compras_usado ──────────────> ejemplares
                           ├─N movimientos_inventario ─────> ejemplares
                           └─N promociones ──N promocion_alcance ──> producto|categoría|plataforma

clientes 1──N direcciones_cliente
clientes 1──N pedidos ──N detalle_pedidos ──> ejemplares ──> productos
pedidos 1──1 pedido_envio ──> direcciones_cliente
clientes 1──N ventas / rentas / apartados / compras_usado
clientes 1──N ejemplares.id_cliente_origen (SET NULL)

ventas | pedidos | rentas | apartados 1──N pagos (arco exclusivo)
detalle_ventas 1──1 devoluciones_garantia
```

- **Trazabilidad de compra:** `detalle_compra_ejemplar` enlaza cada ejemplar
  nuevo con la línea de `detalle_compras` que lo originó (1:N garantizado por
  `UNIQUE(id_ejemplar)`); el costo unitario real queda en `detalle_compras`.
- **Jerarquía producto → ejemplar:** `productos` guarda el modelo comercial;
  `ejemplares` la copia física con serie, costo y estado. Es la decisión
  estructural más importante y evita duplicar atributos de catálogo.
- **Documentos vs. líneas:** todos los movimientos de dinero (compras, ventas,
  pedidos) usan el patrón encabezado + `detalle_*`.
- **Kardex:** `movimientos_inventario` desacopla el historial de cambios de
  estado/stock de la tabla `ejemplares`.

---

## 4. Claves y restricciones de integridad

- **PK:** surrogate UUID (`CHAR(36)`) en todas las tablas salvo
  `perfil_permisos`, `promocion_alcance` y `detalle_compra_ejemplar` (PK compuesta)
  y `configuracion` (PK natural `clave`).
- **UNIQUE relevantes:** `usuarios.nombre_usuario`, `perfiles.nombre`,
  `permisos.nombre`, `productos.codigo_interno`, `ejemplares.numero_serie`,
  `ventas.folio`, `apartados.folio`,
  **`detalle_ventas (id_venta, id_ejemplar)`**, `detalle_pedidos.id_ejemplar`,
  `compras_usado.id_ejemplar`, `devoluciones_garantia.id_detalle_venta`,
  `detalle_compra_ejemplar.id_ejemplar`, `categorias.nombre`, `plataformas.nombre`,
  `generos.nombre`.
- **CHECK:**
  - `cantidad > 0` en `detalle_compras`, `detalle_ventas` y `detalle_pedidos`.
  - `pagos`: `chk_pagos_una_operacion` (exactamente una de las cuatro FKs),
    `chk_pagos_monto (monto >= 0)` y `chk_pagos_concepto` (coherencia
    `concepto` ↔ operación).
  - `rentas`: `chk_renta_devuelta` y `chk_renta_fechas`.
  - `pedidos`: `chk_pedido_envio`, `chk_pedido_entrega` y `chk_pedido_fechas`.
  - `promociones`: `chk_promo_fechas` y `chk_promo_valor`.
- **ON DELETE:** `CASCADE` en relaciones de composición (`perfil_permisos`,
  `direcciones_cliente`, `detalle_compras`, `detalle_ventas`,
  `detalle_pedidos`, `pedido_envio`, `promocion_alcance`); `RESTRICT` en la mayoría de referencias
  maestras (incluida `detalle_compra_ejemplar.id_ejemplar`, para preservar la
  trazabilidad); `SET NULL` en `productos.id_plataforma`, `productos.id_genero`,
  `ejemplares.id_cliente_origen`, `promociones.id_usuario_autoriza` y
  `configuracion.id_usuario`. `pagos` usa `RESTRICT` en ambos sentidos.
- **Columna generada:** `subtotal` en `detalle_compras`, `detalle_ventas` y
  `detalle_pedidos` como `VIRTUAL` (`cantidad * precio`), evitando redundancia.
- **Triggers:** `trg_productos_genero_ins` y `trg_productos_genero_upd`
  rechazan `id_genero`/`clasificacion` no nulos cuando la categoría no es
  `videojuego` (MySQL no permite `CHECK` cross-tabla).

---

## 5. Análisis de normalización

### 5.1 Formas normales

**1FN — atómico:** ✔ Cumple. No hay grupos repetitivos: las direcciones,
teléfonos y líneas de documento se extrajeron a tablas propias; todos los
atributos son atómicos. Los `ENUM` almacenan un único valor por fila (siguen
siendo atómicos).

**2FN — dependencia funcional completa de la PK:** ✔ Cumple. Todas las tablas
tienen PK de un solo atributo (UUID), salvo `perfil_permisos` y
`promocion_alcance`, que son tablas puente sin atributos no clave dependientes
de parte de la clave.

**3FN — sin dependencias transitivas:** ✔ Cumple.
- Los atributos de catálogo se referencian por FK en lugar de duplicar
  nombre/descripción.
- **`productos.tipo` se eliminó** (movido a `categorias.tipo`): era una
  dependencia transitiva `id_producto → id_categoria → tipo`.
- `detalle_*.precio_unitario` / `costo_unitario` **no** son una violación
  transitiva: son *instantáneas históricas* del precio al momento de la
  operación y deben congelarse aunque el precio del producto cambie después.
- `subtotal` es derivable de `cantidad × precio`; al ser columna generada
  virtual, no se almacena ni puede desincronizarse.

**BCNF:** ✔ En la práctica.
- En `pedidos`, guardar `id_cliente` e `id_direccion` juntos permitiría
  `id_direccion → id_cliente` con determinante no superclave. Se resolvió
  sacando la dirección a `pedido_envio`.
- En `detalle_pedidos`, guardar `id_producto` e `id_ejemplar` juntos permitiría
  `id_ejemplar → id_producto`; ahora solo se guarda `id_ejemplar` (con UNIQUE).
- `pagos` es un arco exclusivo válido; no genera FDs con determinante no clave.

**4FN / 5FN:** No se observan dependencias multivaluadas independientes. El
alcance múltiple de promociones se modeló en `promocion_alcance`.

**Conclusión:** el esquema es sólido y coherente con su declaración de **3FN** y
**BCNF**. El único punto estructural con deuda consciente es la FK polimórfica
de `promocion_alcance` (ver 5.2).

### 5.2 Puntos de diseño que rozan la normalización

1. **`promocion_alcance` — FK polimórfica (deuda consciente aceptada).**
   `id_referencia` apunta a producto, categoría o plataforma según
   `tipo_alcance`, por lo que **no admite FK directa**: la integridad se valida
   en la aplicación. El documento de requerimientos no exige FK real ni
   exclusividad, así que se **decidió mantenerla** (ver §19 de
   `checklist-mejoras.md`). Alternativa más estricta, si la rúbrica lo pide:
   tres tablas puente (`promocion_producto`, `promocion_categoria`,
   `promocion_plataforma`) con FK reales.

2. **`pagos` — arco exclusivo (exclusive arc).**
   Cuatro FK anulables con `CHECK` de exactamente una, más
   `tipo_movimiento` (cobro/reembolso) y `concepto`. Es una modelación válida y
   normalizada; el `concepto='deposito'` es **garantía** y no cuenta como
   ingreso. Alternativa teórica: una entidad `operacion` única; no se adoptó.

3. **`direcciones_cliente.es_principal`.** No hay restricción que garantice una
   sola dirección principal por cliente; se resolvería con un índice/UNIQUE
   parcial o un flag en `clientes`.

4. **"categoría = tipo" es observacional, no formal.** La equivalencia entre
   `categorias.nombre` (`Videojuegos`) y `categorias.tipo` (`videojuego`) se
   cumple porque el DML sembró una categoría por tipo; el esquema **no** obliga
   a que el nombre coincida con el tipo.

5. **Cuatro ejes de clasificación de `productos`.** Conviven `categorias.tipo`
   (naturaleza), `productos.id_genero` → `generos` (temática),
   `productos.clasificacion` (ENUM ESRB) y `productos.id_plataforma`
   (compatibilidad). `id_genero` y `clasificacion` solo aplican a videojuegos:
   en los demás van en `NULL`, y los triggers `trg_productos_genero_ins/upd` lo
   imponen en la BD.

6. **ENUM como catálogo.** `tipo`, `condicion`, `estado`, `metodo_pago`,
   `concepto`, etc. usan `ENUM`. Es cómodo y no viola 1FN; si un dominio
   necesita atributos o cambia con frecuencia, conviene tabla catálogo.

7. **Índices.** Solo existen PK/UNIQUE y los índices implícitos de las FK
   (más `idx_alcance_referencia` en `promocion_alcance`). Búsquedas frecuentes
   por fecha podrían beneficiarse de índices declarados en el `CREATE`.

8. **Recargo por atraso como política global.** `recargo_por_dia` vive en
   `configuracion` (clave/valor), no en cada renta. Ventaja: fuente única y
   configurable; costo: si la tarifa cambia, las rentas viejas se recalculan con
   la nueva (decisión consciente para el alcance escolar; en producción se
   congelaría la tarifa en la renta al alta).

9. **Auditoría de cancelaciones (deuda consciente).** El documento de
   requerimientos pide conservar fecha, motivo y usuario de cancelación; el
   esquema actual solo persiste `estado`. Se decidió **no implementarlo en esta
   versión** para simplificar la producción. Cuando el flujo de cancelaciones
   tenga UI real: añadir `fecha_cancelacion`, `motivo_cancelacion` y
   `id_usuario_cancela` a `ventas`, `rentas`, `apartados` y `pedidos`, con
   `CHECK` de coherencia.

---

## 6. Coherencia con la aplicación Java

La aplicación (`src/main/java`) se construyó **contra este esquema**; los
desajustes de la reestructuración ya están resueltos:

| Aspecto                             | Estado actual                                                                                |
|-------------------------------------|----------------------------------------------------------------------------------------------|
| Nombre BD                           | Unificado en **`pixel_zone`** (un guion bajo) en `db.properties` y los 3 scripts             |
| Vistas / procedimientos / funciones | Implementados en `PZ_PL.sql` y consumidos desde los paneles "Consulta SQL" vía `ConsultaDAO` |
| Datos                               | `PZ_DML.sql` carga las 29 tablas                                                              |
| `categorias.tipo` / `productos.tipo`| `ProductoDAO` lee `c.tipo AS tipo`; el `comboTipo` se retiró de `PanelProductos`              |
| `apartados.id_usuario`              | `ApartadoDAO.registrar()` lo inserta (además del kardex)                                      |
| `pagos`                             | La app escribe `tipo_movimiento` y `concepto`; las rentas desglosan renta + depósito          |
| `clientes.tipo_cliente`             | Los combos usan exactamente `registrado`, `mostrador`, `anonimo`; el POS preselecciona `anonimo` |
| `ejemplares.condicion` / `estado`   | Literales en minúsculas                                                                        |
| Administrador                       | `UserSession.esAdmin()` = el perfil posee los 3 permisos sembrados                             |
| Generación de UUID                  | Java (`UUID.randomUUID()`) e inserción explícita; convive con `DEFAULT (UUID())`               |
| Columnas `subtotal`                 | Excluidas de los `INSERT` (son `GENERATED ... VIRTUAL`)                                        |
| Arco exclusivo de `pagos`           | Un solo FK no nulo; los otros tres `NULL`                                                      |
| Kardex                              | La app inserta `movimientos_inventario` en cada cambio de estado, en la misma transacción      |

---

## 7. Fortalezas y debilidades

### Fortalezas
- Esquema **normalizado (3FN/BCNF)** y bien organizado por dominios.
- Uso correcto del patrón **producto vs. ejemplar**, evitando redundancia.
- Integridad referencial explícita con políticas `ON DELETE/UPDATE` coherentes.
- Columnas calculadas `subtotal` como virtuales (sin redundancia en disco).
- Restricciones `UNIQUE` y `CHECK` que codifican reglas de negocio clave
  (arco de pagos, fechas, valor de promoción, coherencia concepto↔operación).
- `movimientos_inventario` como kardex auditable, alimentado por la app.
- `pagos` distingue cobros/reembolsos y excluye el depósito (garantía) de los
  ingresos.
- `genero` como catálogo con FK (`generos`) y `clasificacion` como ENUM ESRB,
  con triggers que imponen que solo los videojuegos los tengan.

### Debilidades / mejoras
- `promocion_alcance.id_referencia` es polimórfica y **sin FK** (deuda
  consciente); migrar a tablas puente si la rúbrica exige integridad total.
- Una sola dirección principal por cliente sin restricción.
- Dependencia de `UUID()` por defecto (requiere MySQL 8.0.13+).
- Ausencia de índices secundarios para reportes.
- `promociones`, `promocion_alcance` y `devoluciones_garantia` no tienen
  consumidor en la app.

---

## 8. Recomendaciones concretas

1. Si se requiere integridad referencial estricta en promociones, migrar
   `promocion_alcance` a tres tablas puente con FK reales.
2. Resolver el catálogo/validación de `genero`/`clasificacion` junto con el
   rediseño de `PanelProductos`.
3. Añadir un mecanismo para una única dirección principal por cliente.
4. Definir índices en fechas (`ventas.fecha_venta`) y claves de reporte.
5. Incorporar `promociones`/`devoluciones_garantia` a la app si el alcance crece.
6. En producción: hashear contraseñas, externalizar credenciales y usar un pool
   de conexiones.
