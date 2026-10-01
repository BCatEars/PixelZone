package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.DevolucionRenta;
import com.pixelzone.model.Renta;
import com.pixelzone.util.Sql;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Transacciones de rentas: alta (marca el ejemplar como rentado) y devolucion
 * (lo regresa a disponible). Kardex en ambos sentidos. La devolucion puede ser
 * individual o en lote (misma transaccion).
 */
public class RentaDAO {

    private static final String SQL_LISTAR =
            "SELECT r.id_renta, r.id_ejemplar, r.id_cliente, c.nombre AS cliente, p.nombre AS producto, "
                    + "       e.numero_serie, r.fecha_limite, r.monto_renta, r.deposito, r.estado "
                    + "FROM rentas r "
                    + "INNER JOIN clientes c ON r.id_cliente = c.id_cliente "
                    + "INNER JOIN ejemplares e ON r.id_ejemplar = e.id_ejemplar "
                    + "INNER JOIN productos p ON e.id_producto = p.id_producto "
                    + "ORDER BY r.fecha_renta DESC";

    private static final String SQL_LISTAR_PENDIENTES =
            "SELECT r.id_renta, r.id_ejemplar, r.id_cliente, c.nombre AS cliente, p.nombre AS producto, "
                    + "       e.numero_serie, r.fecha_limite, r.monto_renta, r.deposito, r.estado "
                    + "FROM rentas r "
                    + "INNER JOIN clientes c ON r.id_cliente = c.id_cliente "
                    + "INNER JOIN ejemplares e ON r.id_ejemplar = e.id_ejemplar "
                    + "INNER JOIN productos p ON e.id_producto = p.id_producto "
                    + "WHERE r.estado IN ('activa', 'vencida') "
                    + "ORDER BY r.fecha_limite";

    private static final String SQL_MARCAR_VENCIDAS =
            "UPDATE rentas SET estado = 'vencida' WHERE estado = 'activa' AND fecha_limite < CURDATE()";

    private static final String SQL_INSERT =
            "INSERT INTO rentas (id_renta, id_cliente, id_usuario, id_ejemplar, fecha_limite, "
                    + "monto_renta, deposito, estado) VALUES (?, ?, ?, ?, ?, ?, ?, 'activa')";

    private static final String SQL_RENTAR =
            "UPDATE ejemplares SET estado = 'rentado' WHERE id_ejemplar = ? AND estado = 'disponible'";

    private static final String SQL_DEVOLVER =
            "UPDATE rentas SET fecha_devolucion = CURRENT_TIMESTAMP, condicion_retorno = ?, "
                    + "monto_extra = ?, estado = 'devuelta' "
                    + "WHERE id_renta = ? AND estado IN ('activa', 'vencida')";

    private static final String SQL_LIBERAR =
            "UPDATE ejemplares SET estado = 'disponible' WHERE id_ejemplar = ? AND estado = 'rentado'";

    private static final String SQL_DEPOSITO =
            "SELECT deposito FROM rentas WHERE id_renta = ? FOR UPDATE";

    private static final String SQL_CLIENTE =
            "SELECT id_cliente FROM rentas WHERE id_renta = ?";

    private static final String SQL_PAGO =
            "INSERT INTO pagos (id_pago, id_venta, id_pedido, id_renta, id_apartado, monto, "
                    + "tipo_movimiento, concepto, metodo_pago) "
                    + "VALUES (?, NULL, NULL, ?, NULL, ?, ?, ?, ?)";

    public List<Renta> listar() throws DAOException {
        return consultar(SQL_LISTAR);
    }

    /** Rentas activas o vencidas, para el modulo de devoluciones. */
    public List<Renta> listarPendientes() throws DAOException {
        return consultar(SQL_LISTAR_PENDIENTES);
    }

    private List<Renta> consultar(String sql) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Marca vencidas al vuelo (sin job): activa + fecha_limite pasada.
            // Es idempotente: solo toca filas 'activa' ya vencidas.
            try (PreparedStatement ps = conn.prepareStatement(SQL_MARCAR_VENCIDAS)) {
                ps.executeUpdate();
            }
            List<Renta> lista = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Renta r = new Renta();
                    r.setIdRenta(rs.getString("id_renta"));
                    r.setIdEjemplar(rs.getString("id_ejemplar"));
                    r.setIdCliente(rs.getString("id_cliente"));
                    r.setNombreCliente(rs.getString("cliente"));
                    r.setNombreProducto(rs.getString("producto"));
                    r.setNumeroSerie(rs.getString("numero_serie"));
                    r.setFechaLimite(rs.getObject("fecha_limite", LocalDate.class));
                    r.setMontoRenta(rs.getBigDecimal("monto_renta"));
                    r.setDeposito(rs.getBigDecimal("deposito"));
                    r.setEstado(rs.getString("estado"));
                    lista.add(r);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Error al listar rentas: " + e.getMessage(), e);
        }
    }

    public void registrar(String idCliente, String idEjemplar, LocalDate fechaLimite,
                          BigDecimal monto, BigDecimal deposito, String idUsuario,
                          String metodoPago) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String idRenta = UUID.randomUUID().toString();
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
                    ps.setString(1, idRenta);
                    ps.setString(2, idCliente);
                    ps.setString(3, idUsuario);
                    ps.setString(4, idEjemplar);
                    ps.setObject(5, fechaLimite);
                    ps.setBigDecimal(6, monto);
                    ps.setBigDecimal(7, deposito);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(SQL_RENTAR)) {
                    ps.setString(1, idEjemplar);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("El ejemplar ya no esta disponible para renta.");
                    }
                }
                Kardex.registrar(conn, idEjemplar, idUsuario, "salida", "renta",
                        "Renta registrada");
                registrarPago(conn, idRenta, "cobro", "renta", monto, metodoPago);
                if (deposito != null && deposito.signum() > 0) {
                    registrarPago(conn, idRenta, "cobro", "deposito", deposito, metodoPago);
                }
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo registrar la renta"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }

    public void devolver(String idRenta, String idEjemplar, String condicionRetorno,
                         BigDecimal montoExtra, String idUsuario, String metodoPago) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                devolverEnTransaccion(conn, idRenta, idEjemplar, condicionRetorno,
                        montoExtra, idUsuario, metodoPago);
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo registrar la devolucion"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }

    /**
     * Devuelve varias rentas en una sola transaccion. Cada {@link DevolucionRenta}
     * lleva su propia condicion y monto extra. Si alguna falla, se revierte todo.
     */
    public void devolverLote(List<DevolucionRenta> devoluciones, String idUsuario,
                             String metodoPago) throws DAOException {
        if (devoluciones == null || devoluciones.isEmpty()) {
            throw new DAOException("No hay rentas que devolver.");
        }
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Red de seguridad: el lote debe ser del mismo cliente.
                String idCliente = null;
                for (DevolucionRenta d : devoluciones) {
                    String actual = idClienteActual(conn, d.getIdRenta());
                    if (actual == null) {
                        throw new SQLException("La renta no existe: " + d.getIdRenta());
                    }
                    if (idCliente == null) {
                        idCliente = actual;
                    } else if (!idCliente.equals(actual)) {
                        throw new SQLException("Todas las rentas del lote deben ser del mismo cliente.");
                    }
                }
                for (DevolucionRenta d : devoluciones) {
                    devolverEnTransaccion(conn, d.getIdRenta(), d.getIdEjemplar(),
                            d.getCondicionRetorno(), d.getMontoExtra(), idUsuario, metodoPago);
                }
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo registrar la devolucion en lote"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }

    private void devolverEnTransaccion(Connection conn, String idRenta, String idEjemplar,
                                       String condicionRetorno, BigDecimal montoExtra,
                                       String idUsuario, String metodoPago) throws SQLException {
        BigDecimal deposito = depositoActual(conn, idRenta);
        try (PreparedStatement ps = conn.prepareStatement(SQL_DEVOLVER)) {
            ps.setString(1, condicionRetorno);
            ps.setBigDecimal(2, montoExtra);
            ps.setString(3, idRenta);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("La renta no esta activa: " + idRenta);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(SQL_LIBERAR)) {
            ps.setString(1, idEjemplar);
            ps.executeUpdate();
        }
        Kardex.registrar(conn, idEjemplar, idUsuario, "entrada", "devolucion_cliente",
                "Devolucion de renta (" + condicionRetorno + ")");
        if (deposito != null && deposito.signum() > 0) {
            registrarPago(conn, idRenta, "reembolso", "deposito", deposito, metodoPago);
        }
        if (montoExtra != null && montoExtra.signum() > 0) {
            registrarPago(conn, idRenta, "cobro", "recargo", montoExtra, metodoPago);
        }
    }

    private BigDecimal depositoActual(Connection conn, String idRenta) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DEPOSITO)) {
            ps.setString(1, idRenta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("deposito") : null;
            }
        }
    }

    private String idClienteActual(Connection conn, String idRenta) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_CLIENTE)) {
            ps.setString(1, idRenta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("id_cliente") : null;
            }
        }
    }

    private void registrarPago(Connection conn, String idRenta, String tipoMovimiento,
                               String concepto, BigDecimal monto, String metodoPago)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_PAGO)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, idRenta);
            ps.setBigDecimal(3, monto);
            ps.setString(4, tipoMovimiento);
            ps.setString(5, concepto);
            ps.setString(6, metodoPago);
            ps.executeUpdate();
        }
    }
}
