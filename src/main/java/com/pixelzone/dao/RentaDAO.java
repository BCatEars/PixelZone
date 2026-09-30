package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
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
 * (lo regresa a disponible). Kardex en ambos sentidos.
 */
public class RentaDAO {

    private static final String SQL_LISTAR =
            "SELECT r.id_renta, r.id_ejemplar, c.nombre AS cliente, p.nombre AS producto, "
                    + "       e.numero_serie, r.fecha_limite, r.monto_renta, r.deposito, r.estado "
                    + "FROM rentas r "
                    + "INNER JOIN clientes c ON r.id_cliente = c.id_cliente "
                    + "INNER JOIN ejemplares e ON r.id_ejemplar = e.id_ejemplar "
                    + "INNER JOIN productos p ON e.id_producto = p.id_producto "
                    + "ORDER BY r.fecha_renta DESC";

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

    public List<Renta> listar() throws DAOException {
        List<Renta> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Renta r = new Renta();
                r.setIdRenta(rs.getString("id_renta"));
                r.setIdEjemplar(rs.getString("id_ejemplar"));
                r.setNombreCliente(rs.getString("cliente"));
                r.setNombreProducto(rs.getString("producto"));
                r.setNumeroSerie(rs.getString("numero_serie"));
                r.setFechaLimite(rs.getObject("fecha_limite", LocalDate.class));
                r.setMontoRenta(rs.getBigDecimal("monto_renta"));
                r.setDeposito(rs.getBigDecimal("deposito"));
                r.setEstado(rs.getString("estado"));
                lista.add(r);
            }
        } catch (SQLException e) {
            throw new DAOException("Error al listar rentas: " + e.getMessage(), e);
        }
        return lista;
    }

    public void registrar(String idCliente, String idEjemplar, LocalDate fechaLimite,
                          BigDecimal monto, BigDecimal deposito, String idUsuario) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
                    ps.setString(1, UUID.randomUUID().toString());
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
                         BigDecimal montoExtra, String idUsuario) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(SQL_DEVOLVER)) {
                    ps.setString(1, condicionRetorno);
                    ps.setBigDecimal(2, montoExtra);
                    ps.setString(3, idRenta);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("La renta no esta activa.");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(SQL_LIBERAR)) {
                    ps.setString(1, idEjemplar);
                    ps.executeUpdate();
                }
                Kardex.registrar(conn, idEjemplar, idUsuario, "entrada", "devolucion_cliente",
                        "Devolucion de renta (" + condicionRetorno + ")");
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo registrar la devolucion"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }
}
