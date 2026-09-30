Análisis completo y actualizado con el esquema DDL oficial (`PZ_DDL.sql` / `pixel_zone`), ajustando el alcance al **Punto de Venta Físico en Mostrador (POS)** sin checkout en línea, resolviendo las inconsistencias de diseño y respondiendo punto por punto al cuestionario.

---

## 🔴 Errores contra el Esquema Fijo (Puntos 1 al 6)

1. **Tabla Fantasma `auditoria` (Eliminada):**
* El script `PZ_DDL.sql` contiene exactamente **24 tablas**. La tabla `auditoria` **no existe** en la base de datos `pixel_zone`.


* *Acción:* Se elimina por completo de las listas de UUIDs (Q3) y de tablas fuera de alcance (Q6).


2. **Regla de UUID en `perfil_permisos`:**
* `perfil_permisos` utiliza una **clave primaria compuesta `(id_perfil, id_permiso)**` y no tiene columna de UUID propia.


* *Acción:* Es una tabla puente de solo lectura. Se excluye formalmente de la regla de generación de UUIDs en Java.




3. **Alineación de Alcance en Compras (`compras` vs `compras_usado`):**
* `compras` y `detalle_compras` (proveedores mayoristas): Son de **solo lectura / consulta** y se consultan mediante el procedimiento almacenado `sp_compras_proveedor`.


* `compras_usado` (adquisición de seminuevos a clientes): Es de **escritura directa en Java** dentro del flujo de entrada de inventario usado.




4. **Generación Concurrente de Folios (`ventas` y `apartados`):**
* No existe tabla de correlativos en el DDL. La estrategia race-safe libre de colisiones sin modificar el DDL es:



$$\text{Folio} = \text{"VTA-"} + \text{System.currentTimeMillis()}$$


* *Nota para la entrega:* Se documentará que los datos semilla del DML usan formato `VTA-001`, mientras que las transacciones dinámicas generan folios con sufijo temporal.




5. **Centinela en `sp_productos_por_plataforma`:**
* El procedimiento no contiene la condición `OR p_plat = ''`. Enviar `""` evalúa `pl.nombre LIKE '%%'`, lo cual descarta productos con `id_plataforma IS NULL` (productos multiconsola o accesorios).


* *Solución UI:* Para listar "Todos los productos", la interfaz Swing consultará directamente la tabla `productos` (JOIN con `categorias` y `plataformas`) a través de la capa DAO, sin llamar al SP con cadena vacía. Si lo que se desea es el inventario físico por ejemplar, se usa la vista real `vw_inventario_ejemplares` (el objeto `vw_inventario_disponible` no existe).




6. **Literales ENUM Estrictos (Sin Acentos ni Caracteres Especiales):**
* `movimientos_inventario.motivo`: `'compra_proveedor'`, `'compra_usado'`, `'devolucion_cliente'`, `'venta'`, `'renta'`, `'apartado'`, `'reserva'`, `'danio'`, `'cancelacion'`, `'ajuste_manual'` (respetando `'danio'` sin `ñ` y `'devolucion_cliente'` sin acento).


* `devoluciones_garantia.condicion_recibida` y `rentas.condicion_retorno`: `'danado'` (sin `ñ`).


* `clientes.tipo_cliente`: `'registrado'`, `'mostrador'`, `'anonimo'`.





---

## ⚠️ Contradicciones Internas y Simplificación de Arquitectura

7. **Drivers JDBC:** Se elimina HikariCP. La conexión se gestiona mediante `DriverManager.getConnection()` envuelto en bloques `try-with-resources`.


8. **Manejo de Excepciones y Conexiones:** Se elimina la clase `ServiceException`. Las capas DAO propagan `SQLException` o una excepción liviana `DAOException`. Se eliminan las llamadas explícitas a `conn.close()` fuera de los bloques de gestión automática de recursos.


9. **Cierre de Conexiones:** Cada operación o transacción abre y cierra su conexión mediante `try-with-resources`.


10. **Entorno de Pruebas:** Evaluación y ejecuciones realizadas contra el motor MySQL 8 local.


11. **Configuración de Credenciales:** `../src/main/resources/db.properties` empacado dentro del proyecto con usuario y contraseña del entorno escolar local.


12. **Target de Compilador:** `../pom.xml` fijado en **Java 21 LTS**. Se purgan las referencias a Java 26 en la documentación (`README.md` y `../GuionPresentacion.md`).


13. **Nombre del Artefacto Ejecutable:** `<finalName>pixel-zone-1.0.0-executable</finalName>` configurado en `../pom.xml`.



---

## 🎯 Respuestas a las 2 Preguntas Clave

### 1. Confirmación de Alcance: Opción B (POS Físico en Mostrador)

* **Punto de Venta Físico en Mostrador (En Java Swing):** El personal de caja (`caja_ana` / permiso `GESTION_VENTAS`) procesa ventas físicas inmediatas. Al cobrar, Java ejecuta una **transacción atómica con `setAutoCommit(false)**` sobre 5 tablas:


1. `INSERT INTO ventas` (genera UUID y folio `VTA-timestamp`).


2. `INSERT INTO detalle_ventas` (asocia el `id_ejemplar` único con `cantidad = 1`, excluyendo `subtotal` por ser columna virtual).


3. `UPDATE ejemplares SET estado = 'vendido'` (bloquea la copia física).


4. `INSERT INTO movimientos_inventario` (registra salida en Kardex con `tipo_movimiento = 'salida'`, `motivo = 'venta'`).


5. `INSERT INTO pagos` (registra el cobro vinculando `id_venta` y dejando nulos los otros tres campos del Check).




* **E-Commerce (Solo Lectura / Consulta):** Las tablas `pedidos` y `detalle_pedidos` no realizan checkout interactivo desde la app (ya que representan órdenes de la tienda web). Se consultan mediante la vista real `vw_pedidos_ecommerce` (no existe `vw_pedidos_totales` ni un procedimiento almacenado para pedidos).



### 2. Documentación del RBAC Heurístico

* **Decisión:** **Sí, se documenta formalmente en el `README.md**`.


* **Mapeo explícito en la documentación:**
* `GESTION_VENTAS`: Módulo de Clientes + Ejecución del Punto de Venta Físico (POS).


* `GESTION_INVENTARIO`: Módulo de Productos, Ejemplares, Proveedores + Compra de Usados a clientes.


* `GESTION_RENTAS`: Registro y devolución de alquileres de videojuegos.


* **Administrador (`admin_pixel`):** Posee los tres permisos asignados en `perfil_permisos`.





---

## 📋 Cuestionario Maestro "Grille Me" (Rounds 0 a 7)

### Round 0 — Respuestas al Contrato

1. **Valores JComboBox exactos:**
* `clientes.tipo_cliente`: `"registrado"`, `"mostrador"`, `"anonimo"`.


* `ejemplares.condicion`: `"nuevo"`, `"usado"`.


* `ejemplares.estado`: `"disponible"`, `"rentado"`, `"vendido"`, `"apartado"`, `"reservado"`, `"baja"`.




2. **String JDBC:** `pixel_zone` (guión bajo simple, coincidiendo con `PZ_DDL.sql` línea 8).


3. **Generación de UUIDs:** Java los genera con `UUID.randomUUID().toString()` y los inserta explícitamente en todas las tablas maestras y transaccionales (excepto `perfil_permisos` que usa PK compuesta).


4. **Columnas Virtuales Excluidas de `INSERT`:**
* `detalle_compras.subtotal`

* `detalle_ventas.subtotal`

* `detalle_pedidos.subtotal`



5. **Arco Exclusivo en `pagos` (`chk_pagos_una_operacion`):** Java inserta el UUID de la operación en curso (`id_venta`, `id_pedido`, `id_renta` o `id_apartado`) y asigna `Types.NULL` (o excluye la columna en el SQL) para las otras tres.



---

### Round 1 — Alcance y Dominio

6. **Escritura Directa en Java:** `clientes`, `direcciones_cliente`, `productos`, `ejemplares`, `proveedores`, `ventas`, `detalle_ventas`, `pagos`, `compras_usado`, `rentas`, `apartados`, `movimientos_inventario`, `usuarios`.


* **Lectura / SP / Vistas:** `compras`, `detalle_compras`, `pedidos`, `detalle_pedidos`, `promociones`, `devoluciones_garantia`.




7. **Trazabilidad del Kardex:** La aplicación escribe explícitamente en `movimientos_inventario` en cada cambio de estado de un ejemplar.


8. **Compra de Usados a Clientes:** En una sola transacción:
1. `INSERT INTO ejemplares` (`condicion = 'usado'`, `estado = 'disponible'`, `id_cliente_origen = id_cliente`).


2. `INSERT INTO compras_usado` (enlazando el `id_ejemplar` recien creado).


3. `INSERT INTO movimientos_inventario` (`tipo_movimiento = 'entrada'`, `motivo = 'compra_usado'`).




9. **Detalle de Ventas (`uq_detventa_ejemplar`):** `detalle_ventas.id_ejemplar` es ÚNICO. En la UI la cantidad para ejemplares físicos está fijada en `1` (`cantidad = 1`).


10. **Precios en Ejemplares:** `productos` tiene precios sugeridos base (`precio_nuevo`/`precio_usado`). Al crear un `ejemplar`, la UI sugiere el precio del catálogo pero permite ajustarlo; el `precio_venta` en `ejemplares` queda congelado e independiente de futuros cambios en `productos`.



---

### Round 2 — Arquitectura y Build

11. **Toolchain:** Target configurado en `../pom.xml` a **Java 21 LTS**.
12. **Estructura de Paquetes:** `com.pixelzone.config`, `com.pixelzone.dao`, `com.pixelzone.model`, `com.pixelzone.ui`, `com.pixelzone.exception`. Ningún panel construye SQL directo; todo pasa por la capa DAO.
13. **Suministro de Credenciales:** Archivo `db.properties` en `../src/main/resources/db.properties`.
14. **Ciclo de Conexión:** `DriverManager.getConnection()` abierto y cerrado explícitamente por operación utilizando `try-with-resources`.
15. **Empaquetado:** `maven-shade-plugin` generando `target/pixel-zone-1.0.0-executable.jar`. Comando de ejecución: `java -jar target/pixel-zone-1.0.0-executable.jar`.

---

### Round 3 — Especificaciones JDBC

16. **URL JDBC Completa:**
    `jdbc:mysql://localhost:3306/pixel_zone?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8`
17. **Manejo Monetario:** `java.math.BigDecimal` para todos los montos de tipo `DECIMAL(10,2)`.
18. **Mapeo de Fechas/Horas:**
* `DATE` $\rightarrow$ `java.time.LocalDate`
* `DATETIME` $\rightarrow$ `java.time.LocalDateTime`
* `CURRENT_TIMESTAMP` $\rightarrow$ Evaluado por MySQL o enviado desde Java con `LocalDateTime.now()`.


19. **Recuperación de UUIDs:** Dado que Java genera los UUIDs con `UUID.randomUUID()` antes de enviar el `INSERT`, el ID ya está disponible en memoria inmediatamente sin necesidad de consultas adicionales.
20. **Booleanos (`TINYINT(1)`):** Lectura con `rs.getBoolean("activo")` o `rs.getObject("es_principal", Boolean.class)` si permite nulos.
21. **Consultas con `LIKE`:** Concatenación del comodín en el parámetro Java (`stmt.setString(1, "%" + busqueda + "%")`).
22. **Estructura `try-with-resources`:**

```java
public List<Producto> listar() throws DAOException {
    String sql = "SELECT id_producto, nombre, precio_nuevo FROM productos WHERE activo = true";
    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
        List<Producto> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapRow(rs));
        }
        return lista;
    } catch (SQLException e) {
        throw new DAOException("Error al listar productos: " + e.getMessage(), e);
    }
}

```

---

### Round 4 — Transacciones e Integridad

23. **Atomicidad en Venta POS:** Transacción única sobre la misma conexión con `conn.setAutoCommit(false)`, ejecutando los 5 `INSERT`/`UPDATE` e invocando `conn.commit()`.
24. **Falla en Transacción:** Captura de `SQLException`, ejecución de `conn.rollback()` y propagación hacia la UI con `JOptionPane.showMessageDialog`.
25. **Control de Concurrencia:** La restricción `uq_detventa_ejemplar UNIQUE (id_ejemplar)` en `detalle_ventas` impide que dos cajeros vendan la misma copia física simultáneamente (Error MySQL 1062).


26. **Generación de Folios:** `folio = "VTA-" + System.currentTimeMillis()`.
27. **Manejo de Borrado Restringido (`RESTRICT`):** Captura del código de error MySQL 1451 (`SQLState 23000`) mostrando un mensaje limpio al usuario.
28. **Validación de `CHECK`:** Las validaciones de formato y rangos (`cantidad > 0`) se realizan en la UI Java antes de armar la consulta SQL.
29. **Ingreso de Stock:** El registro de `compras_usado` genera la copia física en `ejemplares` (`INSERT INTO ejemplares` y luego `INSERT INTO compras_usado`). Para inventario nuevo, la pantalla de Ejemplares crea las copias manualmente; `sp_compras_proveedor` es **solo consulta** y no inserta nada. Además, `compras`/`detalle_compras` referencian `productos`, no `ejemplares`.



---

### Round 5 — Autenticación y RBAC

30. **Login:** Consulta parametrizada comparando `nombre_usuario` y la contraseña enviada.
31. **Permisos de Clientes y Proveedores:**
* Clientes $\rightarrow$ Vinculado a `GESTION_VENTAS`.


* Proveedores $\rightarrow$ Vinculado a `GESTION_INVENTARIO`.




32. **Determinación de Administrador:** El perfil posee las 3 claves de permisos (`GESTION_VENTAS`, `GESTION_INVENTARIO`, `GESTION_RENTAS`).


33. **Seguridad RBAC:** Control visual en la UI deshabilitando pestañas y botones según el rol activo.


34. **Caché de Permisos:** Los permisos se cargan en la clase `UserSession` al iniciar sesión.

---

### Round 6 — Interfaz de Usuario y Motores SQL

35. **Navegación:** `JTabbedPane` con paneles independientes (`JPanel`), cada uno con su propio `JTable` y `DefaultTableModel`.
36. **Subprocesos (Threading):** Consultas pesadas ejecutadas sobre `SwingWorker` para no bloquear el hilo de eventos de Swing (EDT).
37. **Sentinela en Procedimientos Almacenados:** Si el usuario selecciona "Todos", la UI envía `""` para `sp_ejemplares_por_estado` y `sp_pagos_por_metodo`. Para `sp_productos_por_plataforma` (que **no** tiene centinela, y donde `LIKE '%%'` descarta `id_plataforma IS NULL`), la capa DAO consulta directamente la tabla `productos` con JOIN a `categorias`/`plataformas`.


38. **Funciones Escalares:** Los resultados de `SELECT fn_x(?)` se despliegan en componentes `JLabel` de la interfaz.
39. **Campos Opcionales:** *Tooltips* orientativos en los campos de texto de la UI.
40. **Cierre de Sesión:** Invocación de `dispose()`, reinicio de `UserSession` y apertura de la ventana de Login. Conexiones JDBC cerradas automáticamente.
41. **Lectura de `ResultSet`:** Acceso por nombres de columna (`rs.getString("nombre")`).

---

### Round 7 — Verificación y Entregables

42. **Pruebas:** Servidor MySQL 8 local con la base de datos poblada.
43. **Orden de Scripts:**
1. `PZ_DDL.sql` (Esquema de tablas).


2. `PZ_DML.sql` (Carga de datos semilla).


3. `PZ_PL.sql` (Vistas, Funciones y Procedimientos).




44. **Criterios de Aceptación:** Compilación Maven sin errores, inicio de sesión RBAC, flujo POS atómico operativo y ejecución de vistas/procedimientos.
45. **Documentación:** `README.md` actualizado con las decisiones de diseño y configuración de entorno.



---

## ⚡ Rapid-Fire YES / NO

| Pregunta | Respuesta |
| --- | --- |
| ¿Java genera UUIDs? | **SÍ** *(para las 23 tablas con PK simple)*<br> |
| ¿`setAutoCommit(false)` para escrituras multitabla? | **SÍ**<br> |
| ¿`BigDecimal` para valores monetarios? | **SÍ**<br> |
| ¿Todo el código SQL utiliza `PreparedStatement`? | **SÍ**<br> |
| ¿Uso de `try-with-resources` en todas las operaciones? | **SÍ**<br> |
| ¿La aplicación escribe en `movimientos_inventario`? | **SÍ**<br> |
| ¿La aplicación inserta pagos directamente? | **SÍ** *(en la transacción del Punto de Venta)*<br> |
| ¿Se conserva el diseño de 7 pestañas principales? | **SÍ**<br> |
| ¿Se acepta Java 26 como target principal? | **NO** *(Fijado en Java 21 LTS)* |
| ¿Los scripts SQL se mantienen sin modificaciones? | **SÍ**<br> |

---

## 💻 Código de la Transacción del Punto de Venta (POS) en Java

```java
package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class VentaPOSDAO {

    public String registrarVentaMostrador(
            String idCliente,
            String idUsuario,
            List<String> idsEjemplares,
            BigDecimal descuento,
            BigDecimal impuestos,
            BigDecimal montoPago,
            String metodoPago) throws DAOException {

        String idVenta = UUID.randomUUID().toString();
        String idPago = UUID.randomUUID().toString();
        String folio = "VTA-" + System.currentTimeMillis();

        if (descuento == null) descuento = BigDecimal.ZERO;
        if (impuestos == null) impuestos = BigDecimal.ZERO;

        String sqlVenta = """
            INSERT INTO ventas (id_venta, folio, id_cliente, id_usuario, descuento, impuestos, estado)
            VALUES (?, ?, ?, ?, ?, ?, 'completada')
        """;

        String sqlDetalleVenta = """
            INSERT INTO detalle_ventas (id_detalle_venta, id_venta, id_ejemplar, cantidad, precio_unitario)
            VALUES (?, ?, ?, 1, ?)
        """;

        String sqlUpdateEjemplar = """
            UPDATE ejemplares SET estado = 'vendido' WHERE id_ejemplar = ? AND estado = 'disponible'
        """;

        String sqlKardex = """
            INSERT INTO movimientos_inventario (id_movimiento, id_ejemplar, id_usuario, tipo_movimiento, motivo, cantidad, observaciones)
            VALUES (?, ?, ?, 'salida', 'venta', 1, ?)
        """;

        String sqlPago = """
            INSERT INTO pagos (id_pago, id_venta, id_pedido, id_renta, id_apartado, monto, metodo_pago)
            VALUES (?, ?, NULL, NULL, NULL, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Insertar Venta
                try (PreparedStatement stmtV = conn.prepareStatement(sqlVenta)) {
                    stmtV.setString(1, idVenta);
                    stmtV.setString(2, folio);
                    stmtV.setString(3, idCliente);
                    stmtV.setString(4, idUsuario);
                    stmtV.setBigDecimal(5, descuento);
                    stmtV.setBigDecimal(6, impuestos);
                    stmtV.executeUpdate();
                }

                // 2. Insertar Detalle, Actualizar Ejemplar y Registrar Kardex
                for (String idEjemplar : idsEjemplares) {
                    BigDecimal precioUnitario = consultarPrecioEjemplar(conn, idEjemplar);

                    try (PreparedStatement stmtD = conn.prepareStatement(sqlDetalleVenta)) {
                        stmtD.setString(1, UUID.randomUUID().toString());
                        stmtD.setString(2, idVenta);
                        stmtD.setString(3, idEjemplar);
                        stmtD.setBigDecimal(4, precioUnitario);
                        stmtD.executeUpdate();
                    }

                    try (PreparedStatement stmtU = conn.prepareStatement(sqlUpdateEjemplar)) {
                        stmtU.setString(1, idEjemplar);
                        int afectadas = stmtU.executeUpdate();
                        if (afectadas == 0) {
                            throw new SQLException("El ejemplar " + idEjemplar + " ya no esta disponible.");
                        }
                    }

                    try (PreparedStatement stmtK = conn.prepareStatement(sqlKardex)) {
                        stmtK.setString(1, UUID.randomUUID().toString());
                        stmtK.setString(2, idEjemplar);
                        stmtK.setString(3, idUsuario);
                        stmtK.setString(4, "Venta POS Folio: " + folio);
                        stmtK.executeUpdate();
                    }
                }

                // 3. Insertar Pago
                try (PreparedStatement stmtP = conn.prepareStatement(sqlPago)) {
                    stmtP.setString(1, idPago);
                    stmtP.setString(2, idVenta);
                    stmtP.setBigDecimal(3, montoPago);
                    stmtP.setString(4, metodoPago);
                    stmtP.executeUpdate();
                }

                conn.commit();
                return folio;

            } catch (SQLException e) {
                conn.rollback();
                throw new DAOException("Error en la transacción POS: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new DAOException("Error de conexión JDBC: " + e.getMessage(), e);
        }
    }

    private BigDecimal consultarPrecioEjemplar(Connection conn, String idEjemplar) throws SQLException {
        String sql = "SELECT precio_venta FROM ejemplares WHERE id_ejemplar = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idEjemplar);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("precio_venta");
            }
            throw new SQLException("Ejemplar no encontrado: " + idEjemplar);
        }
    }
}

```