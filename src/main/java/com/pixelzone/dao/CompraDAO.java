package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.LineaCompra;
import com.pixelzone.util.SerieGenerator;
import com.pixelzone.util.Sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registro de compras a proveedor. En una sola transaccion crea la compra, sus
 * lineas, los ejemplares fisicos (uno por unidad), el enlace
 * {@code detalle_compra_ejemplar} y el kardex de entrada.
 */
public class CompraDAO {

    private static final String SQL_INSERT_COMPRA =
            "INSERT INTO compras (id_compra, id_proveedor, id_usuario, estado) "
                    + "VALUES (?, ?, ?, 'recibida')";

    private static final String SQL_INSERT_DETALLE =
            "INSERT INTO detalle_compras (id_detalle_compra, id_compra, id_producto, cantidad, costo_unitario) "
                    + "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_EJEMPLAR =
            "INSERT INTO ejemplares (id_ejemplar, id_producto, numero_serie, condicion, costo, "
                    + "precio_venta, tiene_caja, tiene_manual, observaciones) "
                    + "VALUES (?, ?, ?, 'nuevo', ?, ?, TRUE, TRUE, ?)";

    private static final String SQL_INSERT_ENLACE =
            "INSERT INTO detalle_compra_ejemplar (id_detalle_compra, id_ejemplar) VALUES (?, ?)";

    private static final String SQL_LISTAR =
            "SELECT c.id_compra, pr.nombre AS proveedor, u.nombre_completo AS comprador, "
                    + "       c.fecha_compra, c.estado, "
                    + "       COALESCE(SUM(dc.cantidad), 0) AS articulos, "
                    + "       COALESCE(SUM(dc.subtotal), 0.00) AS costo_total "
                    + "FROM compras c "
                    + "INNER JOIN proveedores pr ON c.id_proveedor = pr.id_proveedor "
                    + "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario "
                    + "LEFT JOIN detalle_compras dc ON c.id_compra = dc.id_compra "
                    + "GROUP BY c.id_compra, pr.nombre, u.nombre_completo, c.fecha_compra, c.estado "
                    + "ORDER BY c.fecha_compra DESC";

    /**
     * Registra una compra recibida completa.
     *
     * @throws DAOException si falta el proveedor, no hay lineas o falla la
     *                      transaccion.
     */
    public void registrar(String idProveedor, String idUsuario, List<LineaCompra> lineas)
            throws DAOException {
        if (idProveedor == null || idProveedor.isBlank()) {
            throw new DAOException("La compra requiere un proveedor.");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new DAOException("La compra debe incluir al menos una linea.");
        }
        String idCompra = UUID.randomUUID().toString();
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                insertarCompra(conn, idCompra, idProveedor, idUsuario);
                for (LineaCompra linea : lineas) {
                    String idDetalle = UUID.randomUUID().toString();
                    insertarDetalle(conn, idDetalle, idCompra, linea);
                    for (int i = 0; i < linea.getCantidad(); i++) {
                        String idEjemplar = UUID.randomUUID().toString();
                        String serie = SerieGenerator.generar(conn, linea.getIdProducto());
                        insertarEjemplar(conn, idEjemplar, linea, serie);
                        insertarEnlace(conn, idDetalle, idEjemplar);
                        Kardex.registrar(conn, idEjemplar, idUsuario, "entrada", "compra_proveedor",
                                "Compra a proveedor " + idCompra.substring(0, 8));
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo registrar la compra"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }

    /** @return filas [id_compra, proveedor, comprador, fecha, estado, articulos, costo_total]. */
    public List<Object[]> listar() throws DAOException {
        List<Object[]> filas = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new Object[]{
                        rs.getString("id_compra"),
                        rs.getString("proveedor"),
                        rs.getString("comprador"),
                        rs.getTimestamp("fecha_compra"),
                        rs.getString("estado"),
                        rs.getInt("articulos"),
                        rs.getBigDecimal("costo_total")});
            }
        } catch (SQLException ex) {
            throw new DAOException("Error al listar compras: " + ex.getMessage(), ex);
        }
        return filas;
    }

    private void insertarCompra(Connection conn, String idCompra, String idProveedor,
                                String idUsuario) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_COMPRA)) {
            ps.setString(1, idCompra);
            ps.setString(2, idProveedor);
            ps.setString(3, idUsuario);
            ps.executeUpdate();
        }
    }

    private void insertarDetalle(Connection conn, String idDetalle, String idCompra,
                                 LineaCompra linea) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_DETALLE)) {
            ps.setString(1, idDetalle);
            ps.setString(2, idCompra);
            ps.setString(3, linea.getIdProducto());
            ps.setInt(4, linea.getCantidad());
            ps.setBigDecimal(5, linea.getCostoUnitario());
            ps.executeUpdate();
        }
    }

    private void insertarEjemplar(Connection conn, String idEjemplar, LineaCompra linea,
                                  String serie) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_EJEMPLAR)) {
            ps.setString(1, idEjemplar);
            ps.setString(2, linea.getIdProducto());
            ps.setString(3, serie);
            ps.setBigDecimal(4, linea.getCostoUnitario());
            ps.setBigDecimal(5, linea.getPrecioVenta());
            ps.setString(6, "Compra a proveedor");
            ps.executeUpdate();
        }
    }

    private void insertarEnlace(Connection conn, String idDetalle, String idEjemplar)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_ENLACE)) {
            ps.setString(1, idDetalle);
            ps.setString(2, idEjemplar);
            ps.executeUpdate();
        }
    }
}
