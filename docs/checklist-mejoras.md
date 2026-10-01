# PIXEL ZONE — Checklist de reestructuración y mejoras

> Registro de cambios pendientes sobre la base de datos y la UI Swing.
> Cada punto indica **estado**, **razón** y **propuesta**, con los archivos tocados.
>
> **Leyenda:** `[x]` hecho · `[ ]` pendiente · `[?]` requiere verificación en vivo.

---

## 0. Contexto: reestructuración de BD (primer paso)

Ya aplicada en `PZ_DDL.sql`, `PZ_DML.sql` y `PZ_PL.sql`:

- [x] `categorias.tipo` nuevo; `productos.tipo` eliminado (transitiva `id_producto → id_categoria → tipo`).
- [x] `pedidos.id_direccion` → `pedidos.id_cliente`; dirección de envío en `pedido_envio` (1:1).
- [x] `detalle_pedidos.id_producto` → `detalle_pedidos.id_ejemplar`.
- [x] `apartados.id_usuario` + FK.
- [x] `promocion_alcance` (alcance de promociones fuera de `promociones`).
- [x] `detalle_ventas`: `UNIQUE(id_ejemplar)` → `UNIQUE(id_venta, id_ejemplar)`.
- [x] `CHECK` de coherencia en `rentas`, `pedidos` y `promociones`.
- [x] DML coherente: ejemplares faltantes de la compra recibida + kardex completo.
- [x] `vw_pedidos_ecommerce` y `sp_productos_por_plataforma` actualizados.

Deuda derivada de lo anterior (aplicada y verificada contra MySQL real):

- [x] **`ProductoDAO`**: `p.tipo` fuera del `SELECT`/`INSERT`/`UPDATE`; ahora `c.tipo AS tipo` (el JOIN a `categorias` ya existía e `id_categoria` es `NOT NULL`, así que `INNER JOIN` es correcto).
- [x] **`PanelProductos`**: `comboTipo` eliminado; el tipo se muestra solo en la tabla (derivado), no se captura.
- [x] **`Producto`**: `tipo` queda como campo de presentación (poblado por JOIN, documentado en el javadoc).
- [x] **`ApartadoDAO.SQL_INSERT`**: incluye `id_usuario` y enlaza `idUsuario`.
- [x] **Verificado en runtime**: `ProductoDAO.listar/crear/actualizar/eliminar` y `ApartadoDAO.registrar` ejecutados contra MySQL 8; sin `Unknown column 'p.tipo'` y con `id_usuario` persistido.
- [x] **Docs**: actualizados a 29 tablas y al esquema nuevo (`database-structure.md` reescrito, `checklist.md`, `answers_checklist.md`, `README.md` y `GuionDirectivos.md`).

---

## 1. Autorefresco al navegar

- [x] Interfaz `ui/Recargable` creada; `PanelCrudBase` la implementa; `MainFrame` llama `recargar()` al seleccionar un módulo.
- [x] Al crear un ejemplar aparece de inmediato en Punto de Venta y Rentas.
- [ ] Verificar que **los 9 paneles CRUD** recargan **tablas y combos**: `PanelProductos`, `PanelEjemplares`, `PanelProveedores`, `PanelCompraUsado`, `PanelClientes`, `PanelUsuarios`, `PanelPuntoDeVenta`, `PanelRentas`, `PanelApartados`.
  - *Razón:* un `recargar()` que no recargue catálogos deja combos obsoletos aunque la tabla esté fresca.
  - *Propuesta:* prueba manual recorriendo cada módulo tras editar datos en otro; documentar en `README`.
  - *Archivos:* `src/main/java/com/pixelzone/ui/*`.

---

## 2. Mensajes claros de "sin stock"

- [x] Punto de Venta: `"No hay ejemplares disponibles. Crea copias en Inventario ▸ Ejemplares."` (`PanelPuntoDeVenta.refrescar/cobrar`).
- [x] Rentas: `"Verifica que existan ejemplares 'disponible' y que el producto sea 'Rentable'."` + aviso en barra de estado (`PanelRentas`).
- [x] Apartados: mensaje equivalente (`PanelApartados`).
- [ ] *Idea:* extraer un helper único (p. ej. `Mensajes.sinStock(tipo)`) para no duplicar literales.

---

## 3. Alta masiva de ejemplares — APLICADO

- [x] Generador de series extraído a `util/SerieGenerator` (`SN-<codigo_interno>-NNN`), reutilizado por `CompraDAO` y `EjemplarDAO`.
- [x] `EjemplarDAO.crearLote(List<Ejemplar>, idUsuario)`: una transacción; serie automática por ejemplar (si viene vacía) + un kardex `entrada`/`ajuste_manual` por ejemplar.
- [x] `PanelEjemplares`: campo "Cantidad del lote" + botón **"Generar lote"** (usa producto/condición/costos/caja/manual/observaciones/cliente del formulario; las series se autogeneran).
- [x] **Verificado en runtime** contra MySQL 8: un lote de 5 creó 5 ejemplares `disponible` con series `SN-VJ-002-001…005` y 5 movimientos de kardex.
- *Nota:* el lote es "alta manual" (`ajuste_manual`); para stock de proveedor se usa el módulo §7 (que liga línea↔ejemplar).
- *Archivos:* `SerieGenerator`, `EjemplarDAO`, `PanelEjemplares`, `CompraDAO`.

---

## 4. Separar "registrar renta" de "devolver renta" — APLICADO

- [x] `PanelRentas` quedó como **solo alta** (cliente, ejemplar rentable, fecha límite, monto, depósito, método de pago). Se retiraron el botón "Devolver" y los campos de devolución.
- [x] Nuevo **`PanelDevolucionesRenta`**: tabla filtrada a rentas `activa`/`vencida`; al seleccionar muestra en solo-lectura cliente, serie, fecha límite, días de atraso, recargo y depósito; captura condición de retorno, monto extra y método de reembolso.
- [x] `MainFrame`: módulo "Devoluciones de renta" bajo Caja.
- [x] **Verificado en runtime**: la devolución toma el cliente/serie de la renta seleccionada (no se puede devolver "de otro cliente").
- *Archivos:* `PanelRentas`, `PanelDevolucionesRenta`, `RentaDAO`, `MainFrame`.
- [ ] **No existe** panel para `devoluciones_garantia` (tabla sin UI). Distinto de la devolución de renta.

---

## 5. Estados inválidos al crear ejemplares — APLICADO

- [x] `PanelEjemplares.comboEstado` **eliminado**. El estado se muestra en un label de solo lectura ("Estado actual").
- [x] **Alta:** `EjemplarDAO.SQL_INSERT` ya no incluye `estado`; la BD aplica `DEFAULT 'disponible'` aunque el objeto traiga otro valor.
- [x] **Edición:** `SQL_UPDATE` ya no toca `estado`; el estado solo cambia por operaciones. Se retiró la lógica de kardex por cambio de estado en `actualizar()`.
- [x] **Corrección manual:** botones "Dar de baja" (`baja`) y **nuevo** "Reactivar" (`baja → disponible`), ambos con kardex.
- [x] **Verificado en runtime** contra MySQL 8: crear con `estado='vendido'` → `disponible`; editar con `estado='rentado'` → sigue `disponible` y actualiza precio; baja → `baja`; reactivar → `disponible`.
- *Archivos:* `PanelEjemplares`, `EjemplarDAO`.

> Decisión: el alta solo produce `disponible` (no se ofreció `baja` desde el alta); un ejemplar dañado se crea y luego se da de baja con el botón.

---

## 6. Selección múltiple de objetos — APLICADO (venta + devolución)

- [x] **Venta:** `PanelPuntoDeVenta` usa `MULTIPLE_INTERVAL_SELECTION` y cobra varios ejemplares.
- [x] **Devolución en lote:** `PanelDevolucionesRenta` con selección múltiple. Valida que todas las rentas sean del **mismo cliente**; condición y monto extra se editan **por fila** en la tabla (columnas editables).
- [x] `RentaDAO.devolverLote(...)` en **una transacción**, reutilizando `devolverEnTransaccion` (la misma lógica del `devolver` individual); rollback total si algo falla.
- [x] **Verificado en runtime**: 2 rentas vencidas del mismo cliente → 2 `devuelta`, 2 ejemplares `disponible`, 2 `reembolso/deposito` + 2 `cobro/recargo`; con una renta inválida en el lote, rollback total (la renta válida siguió `vencida`).
- [ ] **Renta múltiple:** alta de varias rentas a la vez — **fuera de alcance** por ahora (añade complejidad a `PanelRentas` sin caso de uso claro).
- *Archivos:* `PanelDevolucionesRenta`, `RentaDAO`, `DevolucionRenta`, `Renta`, `PanelCrudBase`.

---

## 7. Compra de stock obligatoriamente a un proveedor — APLICADO

- [x] Nuevo módulo **"Compras a proveedor"** (`PanelCompras`) con proveedor obligatorio y captura de líneas (producto, cantidad, costo, precio de venta).
- [x] `CompraDAO.registrar(...)` en **una transacción**: `compras` (estado `recibida`) + `detalle_compras` + un `ejemplares` por unidad (serie `SN-<cod>-NNN`) + `movimientos_inventario` (`entrada`/`compra_proveedor`) + enlace.
- [x] **DDL:** nueva tabla `detalle_compra_ejemplar` (1:N línea→ejemplar, `UNIQUE(id_ejemplar)`); 26 → **27 tablas**.
- [x] **DML:** el seed enlaza los 10 ejemplares nuevos de la compra con sus líneas.
- [x] `MainFrame`: módulo "Compras a proveedor" bajo Inventario.
- [x] **Verificado en runtime** contra MySQL 8: registrar 2 líneas (2 + 1 unidades) creó 1 compra, 2 detalles, 3 ejemplares (`SN-VJ-003-001/002`, `SN-ACC-001-001`), 3 enlaces y 3 movimientos.
- *Decisión:* tabla de enlace `detalle_compra_ejemplar` con `UNIQUE(id_ejemplar)` en vez de `ejemplares.id_compra` (evita reordenar el DDL, es BCNF-limpia y da la línea exacta).
- *Archivos:* `PZ_DDL.sql`, `PZ_DML.sql`, `CompraDAO`, `LineaCompra`, `PanelCompras`, `MainFrame`.

---

## 8. Redundancia tipo/categoría

- [x] Resuelto en BD: `tipo` vive en `categorias` (`PZ_DDL.sql`).
- [x] Resuelto en Java (punto 0): `comboTipo` fuera de `PanelProductos`; `ProductoDAO` lee `c.tipo AS tipo` sin persistirlo.

---

## 9. Rentas vencidas y recargo por retraso — APLICADO

- [x] **Modelo (opción 2):** tabla `configuracion(clave, valor, ...)` con `recargo_por_dia` (seed `20.00`) y `dias_gracia_renta`. `ConfiguracionDAO.obtenerDecimal(clave)`.
- [x] **Marcar vencidas:** `RentaDAO` ejecuta `UPDATE rentas SET estado='vencida' WHERE estado='activa' AND fecha_limite < CURDATE()` al listar (sin job).
- [x] **Cálculo:** `dias_atraso = GREATEST(DATEDIFF(CURDATE(), fecha_limite), 0)`; `recargo = dias_atraso * recargo_por_dia`, mostrado en `PanelDevolucionesRenta` con semáforo (verde / amarillo ≤3 / rojo >3) por fila.
- [x] **Cobro:** la devolución pasa `recargo + monto_extra` a `RentaDAO.devolver()`, que registra `reembolso/deposito` y `cobro/recargo`.
- [x] `vw_rentas_activas` ahora incluye `Dias_Atraso`.
- [x] **Verificado en runtime**: renta con fecha límite hace 5 días → `vencida`, `dias=5`, `recargo=100`; pagos `cobro/renta 180`, `cobro/deposito 300`, `reembolso/deposito 300`, `cobro/recargo 100`; `fn_ingresos_por_metodo('efectivo')=760`.
- *Trade-off documentado:* al ser política global, una renta vieja se recalcula con la tarifa nueva (aceptable para el alcance escolar).
- *Archivos:* `PZ_DDL.sql`, `PZ_DML.sql`, `PZ_PL.sql`, `ConfiguracionDAO`, `RentaDAO`, `PanelDevolucionesRenta`, `MainFrame`.

---

## 10. Estado "devuelto" al crear la devolución — VERIFICADO

- [x] `grep -rni "devuelto" src/` **no devuelve coincidencias**: el literal no existe en el código.
- [x] Confirmado: la renta pasa a `'devuelta'` y el **ejemplar** a `'disponible'`; no hay estado de ejemplar `'devuelto'`.
- [x] Encabezado renombrado a **"Estado renta"** en `PanelRentas` y `PanelDevolucionesRenta` para no confundirlo con el estado del ejemplar.
- *Conclusión:* falsa alarma (probablemente de una UI anterior).

---

## 11. Dropdowns con buscador — APLICADO

- [x] Nuevo **`ui/ComboBuscable<T>`**: `JComboBox` editable con filtro incremental (`contains`, case-insensitive), `displayFn` inyectada, `setItems` que conserva la selección, `preseleccionar(Predicate)`, Enter (primer match) / Escape (limpiar) y sincronización del editor al perder foco. El filtrado se difiere con `SwingUtilities.invokeLater` (no se puede mutar el modelo dentro de la notificación del documento) y el `DocumentListener` se retira en los cambios programáticos para evitar filtros espurios.
- [x] Aplicado en: `PanelPuntoDeVenta` (cliente, con preselección del anónimo), `PanelRentas`, `PanelApartados`, `PanelEjemplares`, `PanelCompraUsado`, `PanelCompras` (proveedor y producto) y `PanelProductos` (categoría, plataforma).
- [x] **Verificado**: test headless del componente (filtro por substring, `preseleccionar`, `setItems` conserva selección) + `MainFrame` instanciado con todos los paneles contra la BD viva.
- *Archivos:* `ComboBuscable`, paneles listados.

---

## 12. Navegación: pestañas por módulo — APLICADO

- [x] `MainFrame` reescrito a **híbrido**: `JTabbedPane` superior por módulo (Caja, Inventario, Actores, Consulta SQL) + lista lateral con `CardLayout` dentro de cada módulo.
- [x] `Recargable.recargar()` se llama al cambiar de item y al cambiar de módulo, para no mostrar datos obsoletos.
- [x] Los módulos vacíos (sin ítems permitidos por RBAC) no se agregan; cada item mantiene su propio `JTable`/`DefaultTableModel`.
- [x] **Verificado**: `MainFrame` se instancia con todos los paneles contra la BD viva sin errores.
- *Archivos:* `MainFrame`.

---

## 13. Look & Feel: FlatLaf — APLICADO

- [x] Dependencia `com.formdev:flatlaf:3.7.2` en `pom.xml` (Java 8+, compatible con Java 21; sin dependencias transitivas).
- [x] `Main.main` llama `FlatLightLaf.setup()` **antes** de crear cualquier componente.
- [x] `MainFrame`: botón **"Tema claro/oscuro"** que alterna `FlatLightLaf`/`FlatDarkLaf` con `UIManager.setLookAndFeel` + `updateComponentTreeUI`.
- [x] **Verificado**: `FlatLaf Light` aplicado al arranque, `MainFrame` construido con todos los paneles, y toggle a `FlatLaf Dark` correcto.
- *Nota:* los colores del semáforo de `PanelDevolucionesRenta` son claros y siguen legibles en ambos temas; conviene revisarlos visualmente en la demo.
- *Archivos:* `pom.xml`, `Main`, `MainFrame`.

---

## 14. Deuda de documentación

- [x] Actualizar `docs/database-structure.md` (reescrito: 29 tablas, `pedido_envio`, `promocion_alcance`, `detalle_compra_ejemplar`, `configuracion`, `generos`, `categorias.tipo`, `apartados.id_usuario`, `pagos` con `tipo_movimiento`/`concepto`, "categoría = tipo" y cuatro ejes).
- [x] Actualizar `docs/checklist.md` / `docs/answers_checklist.md` (29 tablas, target Java 21, scripts SQL **sí** modificados, pregunta del arco de `pagos` reescrita).
- [x] Actualizar `docs/README.md` y `GuionDirectivos.md` (conteos, arco de pagos, 1:1 de `pedido_envio`, 3FN/BCNF).
- [x] Documentar `checklist-mejoras.md` en el índice de `docs/README.md`.

---

## 15. Orden sugerido de ejecución

1. Cerrar deuda de la reestructuración (punto 0) para que el proyecto compile.
2. Puntos 5 (estados de ejemplar) y 7 (compras a proveedor) — **aplicados**.
3. Puntos 9 y 4 — **aplicados**.
4. Puntos 3 y 6 — **aplicados**.
5. Puntos 12 y 11 — **aplicados**.
6. Punto 13 (FlatLaf) — **aplicado**.
7. Punto 14 (docs) en cada hito.
8. Punto 16 (dato + catálogo/validación de `genero`) — **aplicado**.
9. Puntos 17 (cliente anónimo) y 18 (pago de renta + depósito) — **aplicados**. Punto 19 (`promocion_alcance`) sigue como deuda consciente.

---

## 16. Género y clasificación: dato sucio + falta de catálogo

> **No** es un problema de normalización: en `productos` no existe una FD `X → genero`
> cuyo determinante no sea superclave, así que 3FN/BCNF se mantienen. El problema es
> **calidad de datos** (valores mal asignados) + **ausencia de catálogo/validación**.
> Por eso va como punto aparte del 8 (que trata la redundancia `categoría`/`tipo`).

### 16.1. DML: `genero`/`clasificacion` solo aplican a videojuegos

- [x] En `PZ_DML.sql`, `genero` y `clasificacion` quedan en `NULL` para todo producto que **no** sea `videojuego` (aplicado: el `Control DualSense Blanco` ya no trae `'Hardware'`/`'E'`).
  - *Problema actual:* el `Control DualSense Blanco` (categoría `Accesorios`) tiene `genero='Hardware'` y `clasificacion='E'`.
  - *Razón:* `genero` es la temática (Acción/Aventura/Carreras) y `clasificacion` es la ESRB (E/E10+/T); ambos son ejes **de videojuego**, no de consola/accesorio.
  - *Acción:* poner `NULL` en esas dos columnas para la fila del control (y revisar futuras altas no-videojuego).
  - *Archivos:* `PZ_DML.sql`.

### 16.2. Semántica: "categoría = tipo" es observacional, no formal

- [x] Documentar la equivalencia (hecho aquí y en `PZ_DDL.sql`).
  - Se cumple porque el DML sembró **una** categoría por tipo (`Videojuegos→videojuego`, `Consolas→consola`, `Accesorios→accesorio`) y la regla de negocio lo asume.
  - El esquema **no** obliga a que `categorias.nombre` coincida con `categorias.tipo`: nada impide crear `"Videojuegos retro"` con `tipo='consola'`.
  - Si se quisiera formalizar, un `CHECK` entre dos columnas libres no basta; se necesitaría validación en app/trigger, o eliminar uno de los dos campos. Decisión: mantener la equivalencia como convención documentada.

### 16.3. Cuatro ejes de clasificación

| Eje | Columna | Valores | Aplica a |
| --- | --- | --- | --- |
| Naturaleza / tipo | `categorias.tipo` | videojuego, consola, accesorio, otro | todo |
| Género temático | `productos.id_genero` → `generos` | Acción, Aventura, Carreras… | **solo videojuegos** |
| Clasificación | `productos.clasificacion` (ENUM) | E, E10+, T, M, AO, RP | **solo videojuegos** |
| Plataforma | `productos.id_plataforma` | PS5, Switch, Xbox | videojuegos y accesorios |

- [x] **Matiz de plataforma (decidido):** se acepta `id_plataforma = NULL` como "multiplataforma/universal"; **no** se crea tabla puente `producto_plataforma` por ahora. Si aparece necesidad real de compatibilidad múltiple, se agrega entonces.

### 16.4. Catálogo y validación — APLICADO

- [x] **Género: catálogo + FK.** Tabla `generos` (`id_genero`, `nombre` UNIQUE, `descripcion`) y `productos.id_genero` (FK, `ON DELETE SET NULL`). Crece sin `ALTER TABLE`.
- [x] **Clasificación: ENUM.** `productos.clasificacion ENUM('E','E10+','T','M','AO','RP')` (conjunto ESRB cerrado).
- [x] **Triggers de coherencia:** `trg_productos_genero_ins` y `trg_productos_genero_upd` rechazan `id_genero`/`clasificacion` no nulos cuando la categoría no es `videojuego` (MySQL no permite `CHECK` cross-tabla).
- [x] **UI (`PanelProductos`):** `ComboBuscable<Genero>` + `JComboBox` ESRB; al elegir una categoría no-videojuego se **deshabilitan y limpian** ambos campos.
- [x] `GeneroDAO` (listar) y `ProductoDAO` (`LEFT JOIN generos`; INSERT/UPDATE con `id_genero`).
- [x] **Verificado en runtime:** `ProductoDAO.listar()` deriva el género; crear un videojuego con género se guarda; crear un accesorio con género es **rechazado por el trigger**.
- *Archivos:* `PZ_DDL.sql`, `PZ_DML.sql`, `Genero`, `GeneroDAO`, `Producto`, `ProductoDAO`, `PanelProductos`.
  - *Propuesta:* opción 1 si se quieren reportes por género; opción 3 si se busca el mínimo cambio de esquema. La 2 sola es frágil.

---

## 17. Cliente anónimo/mostrador — DECIDIDO: opción B (aplicada)

**Modelo adoptado:** se mantienen los 3 tipos con semántica estricta:

| `tipo_cliente` | significado | ejemplo |
| --- | --- | --- |
| `registrado` | cuenta con datos de contacto | Carlos Ramírez, Fernanda López |
| `mostrador` | venta de paso con datos capturados, sin cuenta | Miguel Ángel Torres |
| `anonimo` | fila genérica reutilizable para ventas sin identificación | **Público General** |

- [x] Semántica de los 3 tipos documentada en `PZ_DML.sql`.
- [x] Fila genérica `400...004` renombrada a **"Público General"** (tipo `anonimo`).
- [x] `PanelPuntoDeVenta` preselecciona al cliente `anonimo`; si el cajero ya había elegido otro, restaura esa selección al recargar.
- [x] `ventas.id_cliente` se mantiene `NOT NULL`: toda venta referencia un cliente (la genérica para las de paso). Sin cambio de DDL.
- *Archivos:* `PZ_DML.sql`, `PanelPuntoDeVenta`.

> Descartadas: **A** (2 tipos: se pierde la distinción de paso con/sin datos) y **C** (`id_cliente` nullable: impacta vistas y `INNER JOIN clientes`).

---

## 18. Pago de renta + depósito — DECIDIDO: garantía + Modelo 2 + `concepto` + desglose (aplicado)

**Reglas adoptadas:**
- El **depósito es garantía**: no suma a ingresos.
- `pagos` modela cobros y reembolsos con `tipo_movimiento` y `concepto`; `monto` siempre positivo.
- Flujo de renta **desglosado**:
  - Alta: 2 filas → `cobro`/`renta` por `monto_renta` + `cobro`/`deposito` por el depósito.
  - Devolución: 1–2 filas → `reembolso`/`deposito` por el depósito + `cobro`/`recargo` por `monto_extra` (si aplica).

**Cambios aplicados:**
- [x] **DDL `pagos`**: columnas `tipo_movimiento ENUM('cobro','reembolso')` y `concepto ENUM('venta','pedido','renta','deposito','recargo','apartado')`, más `chk_pagos_monto` y `chk_pagos_concepto` (coherencia concepto ↔ operación).
- [x] **DML**: los pagos semilla llevan `tipo_movimiento`/`concepto`; la renta se desglosó en `renta` (180) + `deposito` (300).
- [x] **PL `fn_ingresos_por_metodo`**: ahora suma con `CASE`, ignora `concepto='deposito'` y resta `tipo_movimiento='reembolso'`.
- [x] **`RentaDAO.registrar`**: inserta el cobro de renta y el del depósito (2 filas).
- [x] **`RentaDAO.devolver`**: lee el depósito (`FOR UPDATE`) y registra reembolso de depósito + cobro de recargo.
- [x] **`VentaPOSDAO` / `ApartadoDAO`**: sus `INSERT INTO pagos` incluyen `tipo_movimiento='cobro'` y `concepto` (`venta` / `apartado`).
- [x] **`PanelRentas`**: campos "Método de pago" y "Método reembolso"; `RentaDAO.registrar/devolver` reciben el método.
- *Archivos:* `PZ_DDL.sql`, `PZ_DML.sql`, `PZ_PL.sql`, `RentaDAO`, `VentaPOSDAO`, `ApartadoDAO`, `PanelRentas`.

> Trade-off asumido: con `concepto`, todos los `INSERT INTO pagos` cambiaron (ya no basta el default). A cambio, los ingresos excluyen depósitos y los reembolsos restan correctamente.
> Nota: `docs/answers_checklist.md` (pregunta del arco de `pagos`) queda desactualizado; se corrige en el punto 14.

---

## 19. `promocion_alcance`: polimórfica vs tablas puente — DECIDIDO: deuda consciente

- [x] **Se mantiene la tabla polimórfica `promocion_alcance`.** El documento describe promociones aplicables a productos, categorías o plataformas **sin exigir FK real ni exclusividad**, así que el diseño actual cumple el requisito funcional.
- Deuda aceptada: `id_referencia` no admite FK directa (integridad validada en la app). Se migraría a tres tablas puente (`promocion_producto`, `promocion_categoria`, `promocion_plataforma`) solo si la rúbrica exige integridad referencial total o si aparece UI de promociones.
- *Archivos:* `PZ_DDL.sql`, `PZ_DML.sql` (sin cambios).

---

## 20. Deudas conscientes (no se implementan en esta versión)

- [x] **Promociones polimórficas** (§19): sin FK en `id_referencia`; validación en app.
- [x] **Auditoría de cancelaciones:** el documento de requerimientos pide conservar fecha, motivo y usuario de cancelación; el esquema actual solo persiste `estado`. Se decidió **no implementarlo ahora** para simplificar la producción (coherente con presentaciones previas). Cuando el flujo de cancelaciones tenga UI real: añadir `fecha_cancelacion`, `motivo_cancelacion`, `id_usuario_cancela` a `ventas`, `rentas`, `apartados` y `pedidos`, con `CHECK` de coherencia.
