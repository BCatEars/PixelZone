# PIXEL ZONE — Sistema de Gestión

Aplicación de escritorio **Java 21 + Swing + JDBC** que funciona como cliente de
la base de datos MySQL **`pixel_zone`** para administrar una tienda de
videojuegos (catálogo, inventario por ejemplar, clientes, compras, ventas,
rentas, apartados, e-commerce de solo lectura, kardex y control de acceso por
perfil).

Es un proyecto académico de **Fundamentos de Bases de Datos**: el grueso del
trabajo está en el modelo relacional (24 tablas en 3FN), y la aplicación es el
cliente que ejercita **todos** los objetos del motor:

- 24 tablas (CRUD y transacciones).
- 5 vistas.
- 5 procedimientos almacenados.
- 5 funciones almacenadas.

---

## 1. Requisitos previos

| Requisito         | Versión / detalle                          | Por qué                                                                           |
|-------------------|--------------------------------------------|-----------------------------------------------------------------------------------|
| **MySQL**         | 8.0.13 o superior                          | `DEFAULT (UUID())` requiere 8.0.13+; los `CHECK` y las columnas generadas también |
| **JDK**           | Java 21 (LTS)                              | Target fijado en `pom.xml` (`maven.compiler.release=21`)                          |
| **Maven**         | 3.8+ *o* IntelliJ IDEA con Maven integrado | `pom.xml` resuelve `mysql-connector-j` 9.7.0 y empaqueta el fat jar               |
| **Driver JDBC**   | `com.mysql:mysql-connector-j:9.7.0`        | Lo descarga Maven; no hay que instalarlo a mano                                   |
| **Base de datos** | Esquema `pixel_zone` cargado               | Ver sección 2                                                                     |

> La app **no** crea la base ni los datos: asume que los scripts ya se
> ejecutaron. Las credenciales de conexión van en `src/main/resources/db.properties`.

---

## 2. Puesta en marcha

### 2.1 Cargar la base de datos (una sola vez)

Ejecutar los tres scripts **en este orden** (el orden importa: `PZ_PL.sql`
depende de que existan las tablas):

```bash
mysql -u root -p < src/main/resources/PZ_DDL.sql   # 1. 24 tablas + restricciones
mysql -u root -p < src/main/resources/PZ_DML.sql   # 2. datos semilla (UUIDs fijos)
mysql -u root -p < src/main/resources/PZ_PL.sql    # 3. 5 vistas + 5 procedimientos + 5 funciones
```

| Script       | Contenido                                                                                                            |
|--------------|----------------------------------------------------------------------------------------------------------------------|
| `PZ_DDL.sql` | `CREATE DATABASE pixel_zone` + 24 `CREATE TABLE` (sin `AUTO_INCREMENT`, PK `CHAR(36)` con `DEFAULT (UUID())`)        |
| `PZ_DML.sql` | Datos semilla coherentes (perfiles, permisos, usuarios, catálogo, ejemplares, venta, renta, apartado, pago, kardex…) |
| `PZ_PL.sql`  | Objetos programables: 5 vistas `vw_*`, 5 procedimientos `sp_*`, 5 funciones `fn_*`                                   |

> Los scripts son el **contrato congelado**: la aplicación se adapta a ellos, no al revés.

### 2.2 Configurar la conexión

Editar `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/pixel_zone?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
db.user=root
db.password=TU_PASSWORD
```

### 2.3 Compilar y ejecutar

**Desde IntelliJ IDEA (recomendado):**
1. `File > Open` → seleccionar la carpeta del proyecto (detecta `pom.xml`).
2. `Project SDK` = 21.
3. Ejecutar la clase `com.pixelzone.Main`.

**Desde terminal con Maven:**

```bash
mvn -q -DskipTests package
java -jar target/pixel-zone-1.0.0-executable.jar
```

---

## 3. Uso de la aplicación

### 3.1 Inicio de sesión

Al arrancar aparece un diálogo modal. Credenciales sembradas por `PZ_DML.sql`
(contraseñas en **texto plano**, simplificación del demo):

| Usuario        | Contraseña      | Perfil        | Permisos                           |
|----------------|-----------------|---------------|------------------------------------|
| `admin_pixel`  | `hash_admin123` | Administrador | los 3                              |
| `caja_ana`     | `hash_ana456`   | Vendedor POS  | `GESTION_VENTAS`, `GESTION_RENTAS` |
| `almacen_luis` | `hash_luis789`  | Almacenista   | `GESTION_INVENTARIO`               |

La consulta de login valida `usuarios.activo = TRUE`, carga el perfil y sus
permisos desde `perfil_permisos`/`permisos`.

### 3.2 Navegación

La ventana principal tiene **navegación lateral por módulos** (árbol) y un
`CardLayout` a la derecha. Los módulos e ítems que el perfil no puede usar
**no se crean** (no aparecen deshabilitados: simplemente no existen).

| Módulo           | Ítems                               | Permiso requerido              |
|------------------|-------------------------------------|--------------------------------|
| **Caja**         | Punto de Venta                      | `GESTION_VENTAS`               |
|                  | Rentas                              | `GESTION_RENTAS`               |
|                  | Apartados                           | `GESTION_VENTAS`               |
| **Inventario**   | Productos                           | `GESTION_INVENTARIO`           |
|                  | Ejemplares                          | `GESTION_INVENTARIO`           |
|                  | Proveedores                         | `GESTION_INVENTARIO`           |
|                  | Compra de usados                    | `GESTION_INVENTARIO`           |
| **Actores**      | Clientes                            | `GESTION_VENTAS`               |
|                  | Usuarios                            | administrador (los 3 permisos) |
| **Consulta SQL** | Vistas / Procedimientos / Funciones | cualquier sesión               |

> El esquema no tiene `GESTION_CLIENTES` ni `GESTION_PROVEEDORES` ni columna
> `es_admin`: los mapeos de la tabla anterior son una **decisión documentada**
> de la aplicación (ver sección 7).

### 3.3 Qué hace cada módulo

Todos los módulos CRUD comparten el mismo patrón: **formulario a la izquierda,
tabla a la derecha**, botones *Guardar / Actualizar / Limpiar* (+ *Eliminar* o
*Dar de baja*). Al seleccionar una fila, el formulario se rellena solo.

| Módulo               | Operaciones                                                                                                                                                                                                           |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Clientes**         | CRUD; `tipo_cliente` ∈ `registrado`, `mostrador`, `anonimo`; activo/inactivo                                                                                                                                          |
| **Productos**        | CRUD; combos de categoría y plataforma; precios nuevo/usado; rentable/activo                                                                                                                                          |
| **Ejemplares**       | CRUD por copia física: serie única, condición (`nuevo`/`usado`), estado, caja/manual, observaciones, cliente de origen (solo si es usado), **sugerencia de precio desde el catálogo**; "Dar de baja" en vez de borrar |
| **Proveedores**      | CRUD                                                                                                                                                                                                                  |
| **Usuarios**         | Solo admin: crear/editar empleados y asignar perfil; no puede eliminarse a sí mismo                                                                                                                                   |
| **Punto de Venta**   | Selecciona cliente y uno o varios ejemplares **disponibles**, total en vivo, método de pago, **cobrar**                                                                                                               |
| **Rentas**           | Alta de renta (ejemplar pasa a `rentado`) y devolución (vuelve a `disponible`), con condición de retorno y monto extra                                                                                                |
| **Apartados**        | Alta con anticipo + pago, **liquidar** (venta con pago del saldo) y cancelar                                                                                                                                          |
| **Compra de usados** | Registra la compra a un cliente: crea el ejemplar usado, `compras_usado` y kardex                                                                                                                                     |
| **Vistas**           | 5 botones que ejecutan `SELECT * FROM vw_...`                                                                                                                                                                         |
| **Procedimientos**   | 5 botones que piden parámetro y ejecutan `CALL sp_... (?)` (vacío = "todos" donde el SP lo permite)                                                                                                                   |
| **Funciones**        | 5 botones que ejecutan `SELECT fn_... (?) AS Resultado` y muestran el escalar                                                                                                                                         |

### 3.4 Reglas de comportamiento importantes

- **Los UUID los genera Java** (`UUID.randomUUID()`), no la base.
- Los combos usan **exactamente** los literales `ENUM` del DDL (nada de
  "Seminuevo", "VIP", etc.).
- **Borrado físico vs baja lógica:** `clientes`, `productos`, `proveedores`,
  `ejemplares` y `usuarios` están referenciados con `RESTRICT`; si el registro
  tiene operaciones, la app muestra un mensaje y sugiere desactivarlo/darlo de baja.
- **Kardex siempre:** toda transición de estado de un ejemplar (alta, venta,
  renta, devolución, apartado, cancelación, baja) inserta su fila en
  `movimientos_inventario` **dentro de la misma transacción**.
- **Atomicidad:** ventas, rentas, apartados y compras de usados usan
  `setAutoCommit(false)` + `commit`/`rollback`.
- **Todas** las consultas usan `PreparedStatement`; las búsquedas `LIKE`
  parametrizan el patrón.

---

## 4. Arquitectura

Arquitectura en capas, sin framework, dentro de una sola aplicación Swing:

```
com.pixelzone
├── Main.java                 Arranque: login → ventana principal (y logout relanza login)
├── config
│   └── DatabaseConfig.java   Lectura de db.properties + Connection (DriverManager)
├── exception
│   └── DAOException.java     Excepción chequeada de la capa DAO
├── util
│   ├── Numeros.java          Parseo decimal tolerante a coma/punto
│   └── Sql.java              Traducción de errores MySQL (1062/1451/1452…) y rollback
├── security
│   └── Permisos.java         Constantes GESTION_*
├── session
│   └── UserSession.java      Usuario + permisos; tienePermiso()/esAdmin()
├── model                     DTOs (Cliente, Producto, Ejemplar, Renta, Apartado, …)
├── dao                       Acceso a datos (un DAO por dominio) + ConsultaDAO + Kardex
└── ui                        Login, MainFrame y un panel por módulo
```

**Convenciones:**
- Ningún panel construye SQL: todo pasa por la capa `dao`.
- Cada operación abre y cierra su conexión con `try-with-resources`
  (`DriverManager`, sin pool: aceptable para el alcance del proyecto).
- `ConsultaDAO` valida los nombres de `vw_*`/`sp_*`/`fn_*` contra listas blancas
  antes de interpolarlos.
- La tabla de resultados se llena por **nombre de columna** (no por índice), con
  columnas dinámicas tomadas de `ResultSetMetaData`.

---

## 5. Objetos de base de datos invocados

- **Vistas:** `vw_inventario_ejemplares`, `vw_historial_ventas`,
  `vw_rentas_activas`, `vw_pedidos_ecommerce`, `vw_kardex_movimientos`.
- **Procedimientos:** `sp_productos_por_plataforma`, `sp_ejemplares_por_estado`,
  `sp_pagos_por_metodo`, `sp_compras_proveedor`, `sp_estado_apartados`
  (un parámetro `IN`; devuelven `ResultSet`).
- **Funciones:** `fn_stock_disponible`, `fn_total_venta_folio`,
  `fn_ganancia_ejemplar`, `fn_ingresos_por_metodo`, `fn_rentas_activas_cliente`
  (un parámetro; devuelven un escalar).

---

## 6. Proceso de planeación del programa

La base de datos y los scripts SQL son la **única parte que se conservó** de la
versión anterior del proyecto. Por eso lo primero fue tratarlos como un
**contrato congelado** y planear la app *alrededor* de ellos, no al revés.

### 6.1 Cuestionario previo (`checklist.md`)

Antes de escribir una línea de código se elaboró un **cuestionario de 45
preguntas** agrupado en 7 rondas, con los puntos donde el esquema "muerde"
marcados como `[TRAP]`:

| Ronda | Tema                                                                                |
|-------|-------------------------------------------------------------------------------------|
| 0     | Contrato: ENUMs, nombre de BD, UUIDs, columnas generadas, arco exclusivo de `pagos` |
| 1     | Alcance: qué tablas se escriben/leen, kardex, compra de usados, unicidad de detalle |
| 2     | Build y arquitectura: Java, paquetes, credenciales, ciclo de conexión, empaquetado  |
| 3     | JDBC: URL, `BigDecimal`, `java.time`, booleanos, `LIKE`, `try-with-resources`       |
| 4     | Transacciones: atomicidad, rollback, concurrencia, folios, FK `RESTRICT`, `CHECK`   |
| 5     | Auth/RBAC: login, permisos, detección de admin, alcance de la seguridad             |
| 6     | UI: navegación, hilos (`SwingWorker`), centinelas de SP, funciones escalares        |
| 7     | Verificación: pruebas, orden de scripts, criterios de aceptación, documentación     |

### 6.2 Respuestas y revisión (`answers_checklist.md`)

Las respuestas se documentaron y luego se **revisaron contra el esquema real**.
Esa revisión detectó y corrigió errores antes de programar, por ejemplo:

- Una tabla fantasma (`auditoria`) que no existe entre las 24 reales.
- Vistas inexistentes (`vw_inventario_disponible`, `vw_pedidos_totales`) y un
  "procedimiento correspondiente" para pedidos que tampoco existe.
- La creencia de que `sp_compras_proveedor` escribe datos (es de **solo consulta**).
- Contradicciones de alcance (`compras` fuera de alcance vs. "genera entradas").
- Faltantes en el formulario de ejemplares (`observaciones`, `id_cliente_origen`).

### 6.3 Fases de construcción

La implementación se dividió en fases verificables; cada una se compiló y probó
contra la base real antes de pasar a la siguiente:

| Fase  | Entregable                                                                     | Verificación                                            |
|-------|--------------------------------------------------------------------------------|---------------------------------------------------------|
| **0** | `pom.xml`, `db.properties`, `DatabaseConfig`, `DAOException`                   | Prueba de humo: conecta y confirma esquema `pixel_zone` |
| **1** | Login, `UserSession`, `MainFrame` con RBAC                                     | Autenticación de los 3 perfiles y botón/tab restringido |
| **2** | CRUD: Clientes, Productos, Ejemplares, Proveedores, Usuarios + `PanelCrudBase` | Round-trips crear→editar→eliminar con limpieza          |
| **3** | Paneles de 5 vistas, 5 procedimientos y 5 funciones (`ConsultaDAO`)            | Las 15 llamadas ejecutadas y sus conteos/valores        |
| **4** | Transacciones: Punto de Venta, Rentas, Apartados, Compra de usados + kardex    | Flujos completos con `commit`/`rollback` y limpieza     |
| **5** | Documentación (`README`, `database-structure`, `Guion`, `checklist`)           | Coherencia con el proyecto real                         |

### 6.4 Decisiones clave surgidas del proceso

| Tema                      | Decisión                                                                                |
|---------------------------|-----------------------------------------------------------------------------------------|
| Generación de UUID        | En Java, insertado explícitamente (no `getGeneratedKeys()` con `DEFAULT (UUID())`)      |
| Columnas `subtotal`       | Excluidas de los `INSERT` (son `GENERATED ... VIRTUAL`)                                 |
| Arco exclusivo de `pagos` | Se llena un solo FK y los otros tres van `NULL`                                         |
| ENUMs                     | Los combos usan los literales exactos del DDL (`danado`/`danio` sin acento)             |
| Folios                    | `VTA-<millis>` y `APT-<millis>`; el `UNIQUE` protege colisiones                         |
| Kardex                    | Escrito por la app en cada cambio de estado, en la misma transacción                    |
| Borrado                   | Baja lógica / desactivación cuando hay `RESTRICT`                                       |
| Seguridad                 | RBAC a nivel de interfaz; la seguridad real recae en la capa Java (usuario MySQL único) |

---

## 7. Decisiones de diseño y limitaciones

- **Contraseñas en texto plano** y credenciales de BD en un `.properties`
  empaquetado: simplificaciones del demo académico, no de producción.
- **Sin pool de conexiones**: una conexión por operación con
  `try-with-resources`; suficiente para el alcance.
- **RBAC heurístico:** `Clientes`→`GESTION_VENTAS`, `Proveedores`→`GESTION_INVENTARIO`
  y "admin = los tres permisos" son decisiones de la app ante la ausencia de
  permisos/columna específicos en el esquema.
- **`vw_inventario_ejemplares`** no expone `id_ejemplar`, por lo que el CRUD de
  ejemplares usa una consulta con JOIN propia; la vista sí se consume en el
  módulo "Vistas".
- Tablas **sin consumidor directo** en la UI: `promociones` y
  `devoluciones_garantia` (solo existen en el esquema; ninguna vista/SP/función
  las usa tampoco).

## 8. Estructura del repositorio

```
.
├── pom.xml
├── PresentacionDirectivos         Especificaciones de la presentación ante directivos
├── GuionDirectivos.md             Guion de la presentación ante directivos (40 min)
├── GuionPresentacion.md           Guion de la presentación académica por integrante
├── checklist.md                   Cuestionario de planeación (45 preguntas)
├── answers_checklist.md           Respuestas y correcciones al cuestionario
├── overview/
│   ├── README.md                  Este documento
│   └── database-structure.md     Análisis del esquema y normalización
└── src/main/
    ├── java/com/pixelzone/        Código de la aplicación
    └── resources/
        ├── PZ_DDL.sql             Esquema (24 tablas)
        ├── PZ_DML.sql             Datos semilla
        ├── PZ_PL.sql              Vistas, procedimientos y funciones
        └── db.properties          Configuración de conexión (editable)
```
