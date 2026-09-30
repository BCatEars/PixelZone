# PIXEL ZONE — Análisis del esquema de base de datos

Análisis del script `PZ_DDL.sql` (raíz del proyecto): estructura, relaciones,
normalización y coherencia con la aplicación Java.

> **Alcance:** el esquema vive en tres scripts dentro de `src/main/resources/`:
> `PZ_DDL.sql` (24 `CREATE TABLE`, 391 líneas, motor InnoDB, charset
> `utf8mb4` / colación `utf8mb4_unicode_ci`), `PZ_DML.sql` (datos semilla) y
> `PZ_PL.sql` (5 vistas, 5 procedimientos y 5 funciones). El análisis de las
> tablas de abajo corresponde a `PZ_DDL.sql`; la coherencia con la aplicación
> se detalla en la sección 6.

---

## 1. Reglas de diseño declaradas

El encabezado del script fija reglas deliberadas (probablemente requisitos de la
asignatura):

- `CREATE DATABASE IF NOT EXISTS pixel_zone` (un solo guion bajo).
- **Sin `AUTO_INCREMENT`.** Todas las PK son `CHAR(36)` con
  `DEFAULT (UUID())` (UUID generado por MySQL 8.0.13+).
- **Sin `ALTER TABLE`.** Todo se declara en el `CREATE TABLE`.
- Claves foráneas con política explícita `ON UPDATE` / `ON DELETE`.
- Uso intensivo de `ENUM` para estados y tipos.

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

### 2.3 Catálogo e inventario (4)
| Tabla         | PK              | Notas                                                                                         |
|---------------|-----------------|-----------------------------------------------------------------------------------------------|
| `categorias`  | `id_categoria`  | `nombre` UNIQUE                                                                               |
| `plataformas` | `id_plataforma` | `nombre` UNIQUE                                                                               |
| `productos`   | `id_producto`   | FK categoría (RESTRICT) y plataforma (SET NULL); `codigo_interno` UNIQUE; precios nuevo/usado |
| `ejemplares`  | `id_ejemplar`   | **Unidad física**; FK producto y cliente de origen; `numero_serie` UNIQUE; condición/estado   |

### 2.4 Proveedores y compras (4)
| Tabla             | PK                  | Notas                                            |
|-------------------|---------------------|--------------------------------------------------|
| `proveedores`     | `id_proveedor`      | datos de contacto                                |
| `compras`         | `id_compra`         | encabezado; FK proveedor y usuario               |
| `detalle_compras` | `id_detalle_compra` | líneas; `subtotal` **columna generada virtual**  |
| `compras_usado`   | `id_compra_usado`   | compra de usados a cliente; `id_ejemplar` UNIQUE |

### 2.5 Ventas (2)
| Tabla            | PK                 | Notas                                                                       |
|------------------|--------------------|-----------------------------------------------------------------------------|
| `ventas`         | `id_venta`         | encabezado; `folio` UNIQUE; descuento/impuestos separados                   |
| `detalle_ventas` | `id_detalle_venta` | líneas; `id_ejemplar` **UNIQUE** (una venta por copia); `subtotal` generado |

### 2.6 Rentas y apartados (2)
| Tabla       | PK            | Notas                                                                |
|-------------|---------------|----------------------------------------------------------------------|
| `rentas`    | `id_renta`    | fechas renta/límite/devolución, montos, condición de retorno, estado |
| `apartados` | `id_apartado` | `folio` UNIQUE, anticipo, estado                                     |

### 2.7 E-commerce (2)
| Tabla             | PK                  | Notas                                                |
|-------------------|---------------------|------------------------------------------------------|
| `pedidos`         | `id_pedido`         | FK → `direcciones_cliente`; envío/guía/fechas/estado |
| `detalle_pedidos` | `id_detalle_pedido` | líneas; `subtotal` generado                          |

### 2.8 Transversales (4)
| Tabla                    | PK              | Notas                                                          |
|--------------------------|-----------------|----------------------------------------------------------------|
| `pagos`                  | `id_pago`       | **Arco exclusivo**: 4 FK opcionales + `CHECK` de exactamente 1 |
| `movimientos_inventario` | `id_movimiento` | Libro mayor (kardex): tipo/motivo/cantidad                     |
| `promociones`            | `id_promocion`  | Alcance opcional por producto/categoría/plataforma             |
| `devoluciones_garantia`  | `id_devolucion` | Ligada a `detalle_ventas` (UNIQUE), con estado y reembolso     |

**Total: 24 tablas.** Perfil claramente transaccional (POS) con soporte de
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
                           └─N promociones ──> producto/categoría/plataforma

clientes 1──N direcciones_cliente 1──N pedidos ──N detalle_pedidos ──> productos
   │ 1──N ventas / rentas / apartados / compras_usado
   └─ ejemplares.id_cliente_origen (SET NULL)

ventas | pedidos | rentas | apartados 1──N pagos (arco exclusivo)
detalle_ventas 1──1 devoluciones_garantia
```

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
  `perfil_permisos`, que usa PK natural compuesta.
- **UNIQUE relevantes:** `usuarios.nombre_usuario`, `perfiles.nombre`,
  `permisos.nombre`, `productos.codigo_interno`, `ejemplares.numero_serie`,
  `ventas.folio`, `apartados.folio`, `detalle_ventas.id_ejemplar`,
  `compras_usado.id_ejemplar`, `devoluciones_garantia.id_detalle_venta`.
- **CHECK:** `cantidad > 0` en los tres `detalle_*`; arco exclusivo en `pagos`
  (exactamente una de las cuatro operaciones).
- **ON DELETE:** `CASCADE` en relaciones de composición (perfil_permisos,
  direcciones_cliente, detalle_compras, detalle_ventas, detalle_pedidos);
  `RESTRICT` en la mayoría de referencias maestras; `SET NULL` en
  `productos.id_plataforma`, `ejemplares.id_cliente_origen` y alcance de
  promociones. `pagos` usa `RESTRICT` en ambos sentidos.
- **Columna generada:** `subtotal` en `detalle_compras`, `detalle_ventas` y
  `detalle_pedidos` como `VIRTUAL` (`cantidad * precio`), evitando redundancia.

---

## 5. Análisis de normalización

### 5.1 Formas normales

**1FN — atómico:** ✔ Cumple. No hay grupos repetitivos: las direcciones,
teléfonos y líneas de documento se extrajeron a tablas propias; todos los
atributos son atómicos. Los `ENUM` almacenan un único valor por fila (siguen
siendo atómicos).

**2FN — dependencia funcional completa de la PK:** ✔ Cumple. Todas las tablas
tienen PK de un solo atributo (UUID), salvo `perfil_permisos`, que es una
tabla puente N:M sin atributos no clave; por tanto, no existen dependencias
parciales posibles.

**3FN — sin dependencias transitivas:** ✔ Cumple en lo esencial.
- Los atributos de catálogo se referencian por FK (`productos.id_categoria`,
  `productos.id_plataforma`) en lugar de duplicar nombre/descripción.
- `detalle_*.precio_unitario` / `costo_unitario` **no** son una violación
  transitiva: son *instantáneas históricas* del precio al momento de la
  operación y deben congelarse aunque el precio del producto cambie después.
  Es una desnormalización intencional y correcta.
- `subtotal` es derivable de `cantidad × precio`; al ser columna generada
  virtual, no se almacena ni puede desincronizarse → sin anomalía de
  actualización.

**BCNF:** ✔ En la práctica. Todas las dependencias no triviales parten de una
superclave (la PK). El único caso a discutir es `pagos` (ver 5.2), pero no
genera dependencias donde un determinante no sea clave.

**4FN / 5FN:** No se observan dependencias multivaluadas independientes. El
caso más cercano es el alcance múltiple de `promociones` (ver 5.2).

**Conclusión:** el esquema es sólido y coherente con su declaración de **3FN**;
podría considerarse prácticamente en **BCNF**.

### 5.2 Puntos de diseño que rozan la normalización

1. **`pagos` — arco exclusivo (exclusive arc).**
   Cuatro FK anulables (`id_venta`, `id_pedido`, `id_renta`, `id_apartado`)
   con `CHECK` de exactamente una. Es una modelación válida y normalizada de
   una relación polimórfica, pero:
   - Deja columnas dispersas y complica las FK (`RESTRICT` global).
   - Alternativa más limpia: una entidad `operacion` (o `documento_cobro`) a la
     que todas las operaciones apunten, y `pagos` referencie solo a esa.
   - No viola 3FN/BCNF; es una decisión de modelado, no un defecto formal.

2. **`promociones` — alcance múltiple.**
   `id_producto`, `id_categoria`, `id_plataforma` son anulables y **no hay
   CHECK** que limite cuántos aplican, por lo que una promo puede apuntar a
   varios ámbitos a la vez (posible conducta ambigua no documentada).
   Para normalizar de forma estricta el alcance se modelaría como
   `promocion_alcance(id_promocion, tipo_alcance, id_referencia)` o tablas
   puente separadas. Tampoco rompe 3FN, pero mejora la integridad semántica.
   Además, falta un `CHECK (fecha_fin >= fecha_inicio)`.

3. **`direcciones_cliente.es_principal`.**
   No hay restricción que garantice una sola dirección principal por cliente;
   se resolvería con un índice/UNIQUE parcial o un flag en `clientes`.

4. **ENUM como catálogo.** `tipo_cliente`, `condicion`, `estado`,
   `metodo_pago`, etc. usan `ENUM`. Es cómodo y no viola 1FN, pero si un
   dominio necesita atributos (descripción, orden, vigencia) o cambia con
   frecuencia, conviene una tabla catálogo referenciada.

5. **Fechas sin validación.** Faltan `CHECK` como
   `fecha_devolucion >= fecha_renta` o `fecha_entrega >= fecha_envio`;
   la integridad temporal recae en la aplicación.

6. **Índices.** Solo existen PK/UNIQUE y los índices implícitos de las FK.
   Búsquedas frecuentes (p. ej. `ventas.fecha_venta`, `movimientos_inventario`
   por ejemplar/fecha) podrían beneficiarse de índices explícitos, pero el
   script prohíbe `ALTER TABLE` (se podrían declarar en el `CREATE`).

---

## 6. Coherencia con la aplicación Java

La aplicación (`src/main/java`) se construyó **contra este esquema congelado**;
los desajustes de la versión anterior ya están resueltos:

| Aspecto                             | Estado actual                                                                                |
|-------------------------------------|----------------------------------------------------------------------------------------------|
| Nombre BD                           | Unificado en **`pixel_zone`** (un guion bajo) en `db.properties` y los 3 scripts             |
| Vistas / procedimientos / funciones | Implementados en `PZ_PL.sql` y consumidos desde los paneles "Consulta SQL" vía `ConsultaDAO` |
| Datos                               | `PZ_DML.sql` carga las 24 tablas, incluido el perfil `Administrador`                         |
| `clientes.tipo_cliente`             | Los combos usan exactamente `registrado`, `mostrador`, `anonimo`                             |
| `ejemplares.condicion`              | `nuevo`, `usado`                                                                             |
| `ejemplares.estado`                 | Los 6 literales en minúsculas                                                                |
| Administrador                       | `UserSession.esAdmin()` = el perfil posee los 3 permisos sembrados                           |
| Generación de UUID                  | Java (`UUID.randomUUID()`) e inserción explícita; convive con `DEFAULT (UUID())`             |
| Columnas `subtotal`                 | Excluidas de los `INSERT` (son `GENERATED ... VIRTUAL`)                                      |
| Arco exclusivo de `pagos`           | Un solo FK no nulo; los otros tres `NULL`                                                    |
| Kardex                              | La app inserta `movimientos_inventario` en cada cambio de estado, en la misma transacción    |

---

## 7. Fortalezas y debilidades

### Fortalezas
- Esquema **normalizado (≈BCNF)** y bien organizado por dominios.
- Uso correcto del patrón **producto vs. ejemplar**, evitando redundancia.
- Integridad referencial explícita con políticas `ON DELETE/UPDATE` coherentes.
- Columnas calculadas `subtotal` como virtuales (sin redundancia en disco).
- Restricciones `UNIQUE` y `CHECK` que codifican reglas de negocio clave.
- `movimientos_inventario` como kardex auditable, alimentado por la app.

### Debilidades / mejoras
- Faltan validaciones temporales (`CHECK` de fechas) y del alcance único en
  `promociones`; la integridad temporal recae en la aplicación.
- Modelos de arco exclusivo (`pagos`) y alcance polimórfico (`promociones`)
  podrían simplificarse para mejorar integridad y consultas.
- Dependencia de `UUID()` por defecto (requiere MySQL 8.0.13+).
- Ausencia de índices secundarios para consultas de reportes.
- `promociones` y `devoluciones_garantia` no tienen consumidor en la app ni en
  los objetos programables.

---

## 8. Recomendaciones concretas

1. Añadir `CHECK` de fechas (`fecha_devolucion >= fecha_renta`,
   `fecha_entrega >= fecha_envio`) y un mecanismo para una única dirección
   principal por cliente.
2. Normalizar el alcance de `promociones` (tabla puente) y valorar una entidad
   de operación única para el arco de `pagos`.
3. Definir índices en fechas (`ventas.fecha_venta`) y claves de reporte frecuentes.
4. Incorporar `promociones` y `devoluciones_garantia` a las vistas o a un módulo
   de la app si el alcance crece.
5. En producción: hashear contraseñas, externalizar credenciales y usar un pool
   de conexiones.
