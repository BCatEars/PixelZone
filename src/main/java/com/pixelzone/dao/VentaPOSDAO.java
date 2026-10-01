package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.util.Sql;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Transaccion del Punto de Venta: {@code ventas} + {@code detalle_ventas} +
 * estado del ejemplar + kardex + {@code pagos}, todo en un solo commit.
 */
public class VentaPOSDAO {

    private static final String SQL_PRECIO =
            "SELECT precio_venta, estado FROM ejemplares WHERE id_ejemplar = ? FOR UPDATE";

    private static final String SQL_INSERT_VENTA =
            "INSERT INTO ventas (id_venta, folio, id_cliente, id_usuario, descuento, impuestos, estado) "
                    + "VALUES (?, ?, ?, ?, ?, ?, 'completada')";

    private static final String SQL_INSERT_DETALLE =
            "INSERT INTO detalle_ventas (id_detalle_venta, id_venta, id_ejemplar, cantidad, precio_unitario) "
                    + "VALUES (?, ?, ?, 1, ?)";

    private static final String SQL_VENDER_EJEMPLAR =
            "UPDATE ejemplares SET estado = 'vendido' WHERE id_ejemplar = ? AND estado = 'disponible'";

    private static final String SQL_INSERT_PAGO =
            "INSERT INTO pagos (id_pago, id_venta, id_pedido, id_renta, id_apartado, monto, "
                    + "tipo_movimiento, concepto, metodo_pago) "
                    + "VALUES (?, ?, NULL, NULL, NULL, ?, 'cobro', 'venta', ?)";

    /**
     * Registra una venta completa.
     *
     * @return el folio generado.
     */
    public String registrarVenta(String idCliente, String idUsuario, List<String> idsEjemplares,
                                 BigDecimal descuento, BigDecimal impuestos,
                                 String metodoPago) throws DAOException {
        if (idsEjemplares == null || idsEjemplares.isEmpty()) {
            throw new DAOException("La venta debe incluir al menos un ejemplar.");
        }
        String idVenta = UUID.randomUUID().toString();
        String folio = "VTA-" + System.currentTimeMillis();

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<BigDecimal> precios = new ArrayList<>();
                BigDecimal subtotal = BigDecimal.ZERO;
                for (String idEjemplar : idsEjemplares) {
                    BigDecimal precio = precioDisponible(conn, idEjemplar);
                    precios.add(precio);
                    subtotal = subtotal.add(precio);
                }
                BigDecimal total = subtotal.subtract(descuento).add(impuestos);

                insertarVenta(conn, idVenta, folio, idCliente, idUsuario, descuento, impuestos);
                for (int i = 0; i < idsEjemplares.size(); i++) {
                    String idEjemplar = idsEjemplares.get(i);
                    insertarDetalle(conn, idVenta, idEjemplar, precios.get(i));
                    marcarVendido(conn, idEjemplar);
                    Kardex.registrar(conn, idEjemplar, idUsuario, "salida", "venta",
                            "Venta POS folio " + folio);
                }
                insertarPago(conn, idVenta, total, metodoPago);

                conn.commit();
                return folio;
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo registrar la venta"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }

    private BigDecimal precioDisponible(Connection conn, String idEjemplar) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_PRECIO)) {
            ps.setString(1, idEjemplar);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("El ejemplar no existe: " + idEjemplar);
                }
                if (!"disponible".equals(rs.getString("estado"))) {
                    throw new SQLException("El ejemplar ya no esta disponible: " + idEjemplar);
                }
                return rs.getBigDecimal("precio_venta");
            }
        }
    }

    private void insertarVenta(Connection conn, String idVenta, String folio, String idCliente,
                               String idUsuario, BigDecimal descuento, BigDecimal impuestos)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_VENTA)) {
            ps.setString(1, idVenta);
            ps.setString(2, folio);
            ps.setString(3, idCliente);
            ps.setString(4, idUsuario);
            ps.setBigDecimal(5, descuento);
            ps.setBigDecimal(6, impuestos);
            ps.executeUpdate();
        }
    }

    private void insertarDetalle(Connection conn, String idVenta, String idEjemplar,
                                 BigDecimal precio) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_DETALLE)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, idVenta);
            ps.setString(3, idEjemplar);
            ps.setBigDecimal(4, precio);
            ps.executeUpdate();
        }
    }

    private void marcarVendido(Connection conn, String idEjemplar) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_VENDER_EJEMPLAR)) {
            ps.setString(1, idEjemplar);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("El ejemplar ya no esta disponible: " + idEjemplar);
            }
        }
    }

    private void insertarPago(Connection conn, String idVenta, BigDecimal total,
                              String metodoPago) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_PAGO)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, idVenta);
            ps.setBigDecimal(3, total);
            ps.setString(4, metodoPago);
            ps.executeUpdate();
        }
    }
}
