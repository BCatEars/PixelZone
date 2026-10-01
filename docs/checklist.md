# PIXEL ZONE — Checklist para escribir el nuevo programa Java

> La base de datos y los scripts SQL (`PZ_DDL.sql`, `PZ_DML.sql`, `PZ_PL.sql`) fueron
> el **contrato congelado** durante la planeación. Los puntos marcados **[TRAP]** son
> donde el esquema mordía. Ver `README.md` §6 ("Proceso de planeación") para las fases.
>
> **Estado: CERRADO.** Las 45 preguntas (más la ráfaga rápida) fueron respondidas en
> `answers_checklist.md` y aplicadas en el código. **Después el contrato se
> reestructuró** (3FN/BCNF): `productos.tipo` → `categorias.tipo`, `pedidos` +
> `pedido_envio`, `detalle_pedidos.id_ejemplar`, `apartados.id_usuario`,
> `promocion_alcance`, y `pagos` con `tipo_movimiento`/`concepto`. El detalle y el
> seguimiento están en `checklist-mejoras.md`.

---
    
## Ronda 0 — Contrato (responder primero)

- [x] **1. Valores ENUM.** `clientes.tipo_cliente` solo acepta `registrado`, `mostrador`, `anonimo`. `ejemplares.condicion` solo acepta `nuevo`, `usado`. `ejemplares.estado` solo acepta `disponible|rentado|vendido|apartado|reservado|baja`. Escribe la lista **exacta** de strings para cada `JComboBox`. Prohibido: "Seminuevo", "VIP", "Frecuente", "Ocasional".
- [x] **2. Nombre de la BD.** `PZ_DDL.sql:8` crea `pixel_zone` (un guion bajo). `PZ_DML.sql:1` y `PZ_PL.sql:5` hacen `USE pixel_zone`. Define el string exacto del JDBC URL y confirma que **no** usarás `pixel__zone`.
- [x] **3. [TRAP] Generación de UUID.** Las PK son `CHAR(36) DEFAULT (UUID())`. `getGeneratedKeys()` **no** devuelve ese default como con `AUTO_INCREMENT`. Decide: ¿Java genera con `UUID.randomUUID()` e inserta explícito, o la BD lo genera y relees por otra clave única? Aplica a **cada** tabla donde insertes.
- [x] **4. [TRAP] Columnas generadas.** `detalle_compras.subtotal`, `detalle_ventas.subtotal` y `detalle_pedidos.subtotal` son `GENERATED ALWAYS AS (...) VIRTUAL`. Enumera **todas** las columnas que debes excluir de los `INSERT`.
- [x] **5. [TRAP] Arco exclusivo en `pagos`.** El `CHECK` exige exactamente uno de `id_venta / id_pedido / id_renta / id_apartado`. Define cómo construyes los pagos sin violarlo y cómo la UI sabe a qué operación pertenece el pago.

---

## Ronda 1 — Alcance (ser brutalmente específico)

- [x] **6. Tablas de escritura vs solo lectura.** ¿A cuáles de las **29 tablas** **escribe** la app y cuáles son solo lectura? Si mantienes el alcance antiguo (`clientes`, `productos`, `ejemplares`, `proveedores`), declara explícitamente qué operaciones se insertan desde la app.
- [x] **7. [TRAP] El kardex que nadie alimenta.** No hay **triggers**. `movimientos_inventario` (consumido por `vw_kardex_movimientos`) solo existe si algo lo inserta. ¿Quién lo escribe: la app, un trigger nuevo, o nadie (y mientes en la presentación)?
- [x] **8. [TRAP] Huevo y gallina del usado.** `compras_usado.id_ejemplar` es `NOT NULL UNIQUE` y referencia un `ejemplares` existente. Define el orden exacto de inserts y de dónde sale ese primer `ejemplares`.
- [x] **9. `detalle_ventas` tiene `UNIQUE (id_venta, id_ejemplar)`.** *(Antes era `UNIQUE (id_ejemplar)`; se cambió para permitir revender un ejemplar cancelado sin duplicarlo en la misma venta.)* ¿Fuerzas `cantidad = 1` en la UI? Define la regla.
- [x] **10. Precios producto vs ejemplar.** `productos` tiene `precio_nuevo`/`precio_usado`; `ejemplares` tiene su propio `costo`/`precio_venta`. Al crear un ejemplar, ¿copias precios del producto, los captura el usuario, o hay default? ¿Qué pasa cuando el precio del producto cambia después?

---

## Ronda 2 — Build y arquitectura

- [x] **11.** `../pom.xml` fija `maven.compiler.release = 21` (Java 21 LTS). Confirma el toolchain que compila de verdad.
- [x] **12.** Da los **nombres de paquetes** y la **lista de clases** reales, no "MVC". ¿Qué clase es dueña de la `Connection`? ¿Hay capa DAO o los paneles vuelven a construir SQL a mano (el README viejo admitía SQL crudo en paneles)?
- [x] **13.** ¿Cómo se suministran URL/usuario/contraseña: constantes, `.properties`, o variables de entorno? El README viejo hardcodeaba `root` sin contraseña; ¿repites ese pecado?
- [x] **14.** Ciclo de vida de la conexión: ¿una `Connection` para toda la app, una por operación, o pool (HikariCP)? Justifica contra el diseño antiguo "abrir/cerrar por operación".
- [x] **15.** Empaquetado: ¿`javac`/`java -cp`, `maven-jar-plugin`, o fat jar? Escribe el comando exacto de arranque para el día de la demo y confirma que funciona offline.

---

## Ronda 3 — JDBC (parámetros, no vaguedades)

- [x] **16.** Escribe el JDBC URL completo. Cubre al menos: `serverTimezone`, `useSSL`/`sslMode`, `allowPublicKeyRetrieval`, `characterEncoding`/`connectionCollation` y el nombre de BD de la Q2.
- [x] **17.** ¿`BigDecimal` o `double` para `DECIMAL(10,2)`? Indica dónde llamas `setBigDecimal`/`getBigDecimal`.
- [x] **18.** Fechas: ¿`java.time` (`LocalDate`, `LocalDateTime`) o `java.sql.Date/Timestamp`? Mapea `DATE`, `DATETIME` y las columnas `DEFAULT CURRENT_TIMESTAMP`.
- [x] **19. [TRAP] Recuperación del UUID** (continuación de Q3): si la BD genera el UUID, ¿cómo refrescas la tabla tras el insert sin conocer el id nuevo? Responde en una frase.
- [x] **20.** Booleanos/`TINYINT(1)`: ¿`getBoolean`, `getInt` o `getObject` para `activo`, `rentable`, `tiene_caja`, `es_principal`? ¿Y cómo escribes booleanos anulables?
- [x] **21.** Todo `SELECT ... LIKE` (búsqueda por nombre/serie/código): ¿`PreparedStatement` con `?` o concatenación de `%`? Sé honesto.
- [x] **22.** `try-with-resources` en `Connection`/`Statement`/`ResultSet` — ¿sí o no? Muestra la forma de un método.

---

## Ronda 4 — Transacciones e integridad (donde más se falla)

- [x] **23. [TRAP] Atomicidad.** Una "venta" toca `ventas` + `detalle_ventas` + `ejemplares.estado` + `pagos` + `movimientos_inventario`. ¿Es **una** transacción con `setAutoCommit(false)` y un solo `commit`, o cinco conexiones independientes? Si son independientes, justifica cómo evitas un juego medio vendido.
- [x] **24.** Si algo falla a medio camino, ¿qué se revierte y qué ve el usuario? Define el contrato `rollback()` + mensaje de error.
- [x] **25.** Nivel de aislamiento: ¿dejas InnoDB en `REPEATABLE READ`? ¿Te importan dos cajeros vendiendo el mismo ejemplar a la vez? ¿Cómo te salva `uq_detventa_ejemplar`?
- [x] **26. [TRAP] Generación de folios.** ¿De dónde salen `VTA-001`, `APT-001`? Si haces `SELECT MAX(folio)+1`, dos sesiones chocan contra `uq_ventas_folio`/`uq_apartados_folio`. Define un esquema de folios a prueba de carrera.
- [x] **27.** Borrados: `clientes`, `categorias`, `productos`, `usuarios` son `RESTRICT`. Si el usuario borra un cliente con ventas, ¿la app captura el `SQLException` de FK y muestra mensaje, pre-verifica dependencias, o falla en silencio?
- [x] **28.** `CHECK (cantidad > 0)`: si teclean 0, ¿validas en Java o dejas que MySQL lance el error crudo?
- [x] **29.** Comprar stock con `compras`/`detalle_compras` referencia `productos`, no `ejemplares`. ¿La app crea luego los `ejemplares` individuales o `compras` es decorativa?

---

## Ronda 5 — Auth y RBAC (el diseño más débil)

- [x] **30.** Login: `usuarios` join `perfiles` con `activo = 1`. ¿Parametrizado? ¿Qué pasa con 0 filas vs 1 fila? **[TRAP]** Las contraseñas están en texto plano (`hash_admin123`): comparas strings crudos, no fingas hashing.
- [x] **31. [TRAP] No existe `GESTION_CLIENTES` ni `GESTION_PROVEEDORES`.** Los permisos sembrados son solo `GESTION_VENTAS`, `GESTION_INVENTARIO`, `GESTION_RENTAS`. ¿Qué permiso controla los botones de **Clientes** y **Proveedores**? ¿O los ve todo el mundo? Responde con precisión.
- [x] **32.** No hay columna `es_admin`. `Administrador` tiene los tres permisos; `Vendedor POS` tiene VENTAS+RENTAS; `Almacenista` tiene INVENTARIO. ¿Cómo decides "admin": heurística de los tres, UUID fijo `30000000-...-001`, o nombre que contiene "admin"? Elige uno y defiéndelo.
- [x] **33.** Deshabilitar botones en el cliente **no** es control de acceso: el usuario de BD es `root`. ¿Añades usuarios/`GRANT`s reales de MySQL o admites que el RBAC es cosmético?
- [x] **34.** Los permisos se cargan al login y quedan en memoria. Si alguien edita `perfil_permisos` con la app abierta, ¿cambia algo? ¿Te importa?

---

## Ronda 6 — UI y llamadas al motor

- [x] **35.** ¿Swing u otra cosa? Si es Swing: cómo mapea el `JTabbedPane` de 7 pestañas a tus clases y si sigue habiendo **una** `JTable`/`DefaultTableModel` compartida (cambiar de contexto borra la tabla).
- [x] **36.** Hilos: ¿llamadas a BD en el EDT o en `SwingWorker`/hilo de fondo? ¿Qué pasa con la UI mientras corre una consulta lenta?
- [x] **37. [TRAP] Centinela de procedimientos.** `sp_ejemplares_por_estado` tiene `WHERE e.estado = p_est OR p_est = ''`, y `sp_pagos_por_metodo`/`sp_productos_por_plataforma` hacen algo similar. ¿La UI permite pasar cadena vacía para "todos"? Si manda `NULL`, la rama `= ''` falla.
- [x] **38.** Las funciones se llaman con `SELECT fn_x(?) AS Resultado`. ¿Cómo muestras un escalar: tabla de una fila/una columna, o etiqueta? Decide.
- [x] **39.** Argumentos vacíos/opcionales: `sp_estado_apartados` matchea `a.folio LIKE ... OR a.estado = p_fol`. ¿El usuario sabe que puede teclear un estado en vez de un folio? ¿Lo insinúas en la UI?
- [x] **40.** Logout / "cambiar usuario" cierra el frame y reabre login. ¿Cierra la `Connection`? Auditoría de fugas: qué queda abierto al salir y qué está garantizado cerrar.
- [x] **41.** Selección→formulario: rellenas por **índice de columna**. ¿Qué pasa el día que una vista agrega una columna? ¿Usas `ResultSetMetaData`/nombres de columna en su lugar?

---

## Ronda 7 — Verificación y entregable

- [x] **42.** ¿Cómo pruebas el código de BD sin MySQL vivo? ¿Testcontainers, `mysql:8` en Docker, dump versionado, o "le pico a los botones"?
- [x] **43.** ¿Existe un script que corra `PZ_DDL.sql` → `PZ_DML.sql` → `PZ_PL.sql` en el orden correcto? Nota: antes nunca mencionaste `PZ_PL.sql` — confirma que sabes que las vistas/procedimientos/funciones **no** están en `PZ_DDL.sql`.
- [x] **44.** ¿Cuál es tu definición de "terminado" para el nuevo programa Java? Lista los criterios de aceptación.
- [x] **45.** ¿Reescribirás README/Guion para que coincidan con la realidad (sin el fantasma `FuncionesProcedimientosVistas.sql`, sin `pixel__zone`, sin afirmar código Java que no existe)?

---

## Ráfaga rápida (responder todo)

- [x] ¿Java genera los UUIDs?
- [x] ¿`setAutoCommit(false)` en escrituras multi-tabla?
- [x] ¿`BigDecimal` para dinero?
- [x] ¿Todo el SQL con `PreparedStatement`?
- [x] ¿`try-with-resources` en todas partes?
- [x] ¿La app escribe en `movimientos_inventario`?
- [x] ¿La app inserta en `pagos`?
- [x] ¿Mantienes el layout de 7 pestañas? *(Histórico; hoy es híbrido: `JTabbedPane` por módulo + lista lateral + `CardLayout`.)*
- [x] ¿Aceptas Java 26 como target? *(Histórico; fijado en Java 21 LTS.)*
- [x] ¿**No** tocarás los scripts SQL? *(Histórico. Hoy **sí** se reestructuraron: ver `checklist-mejoras.md`.)*

---

## Predicción: dónde te van a doler las preguntas

1. `pagos` (arco exclusivo) + columnas generadas.
2. Kardex (`movimientos_inventario`) nunca escrito.
3. Carrera de folios / recuperación de UUID.
4. No existe permiso para Clientes/Proveedores.
5. Fronteras de transacción en el flujo de venta.
