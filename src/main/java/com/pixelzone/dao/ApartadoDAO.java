package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Apartado;
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
 * Transacciones de apartados: alta con anticipo (y su pago), cancelacion y
 * liquidacion. El ejemplar se marca como apartado/vendido/disponible y el
 * kardex se mantiene sincronizado.
 */
public class ApartadoDAO {

    private static final String SQL_LISTAR =
            "SELECT a.id_apartado, a.id_ejemplar, a.folio, c.nombre AS cliente, "
                    + "       p.nombre AS producto, e.numero_serie, a.fecha_limite, "
                    + "       a.importe_anticipo, a.estado "
                    + "FROM apartados a "
                    + "INNER JOIN clientes c ON a.id_cliente = c.id_cliente "
                    + "INNER JOIN ejemplares e ON a.id_ejemplar = e.id_ejemplar "
                    + "INNER JOIN productos p ON e.id_producto = p.id_producto "
                    + "ORDER BY a.fecha_inicio DESC";

    private static final String SQL_INSERT =
            "INSERT INTO apartados (id_apartado, folio, id_cliente, id_ejemplar, fecha_limite, "
                    + "importe_anticipo, estado) VALUES (?, ?, ?, ?, ?, ?, 'activo')";

    private static final String SQL_APARTAR =
            "UPDATE ejemplares SET estado = 'apartado' WHERE id_ejemplar = ? AND estado = 'disponible'";

    private static final String SQL_LIBERAR =
            "UPDATE ejemplares SET estado = 'disponible' WHERE id_ejemplar = ? AND estado = 'apartado'";

    private static final String SQL_VENDER =
            "UPDATE ejemplares SET estado = 'vendido' WHERE id_ejemplar = ? AND estado = 'apartado'";

    private static final String SQL_CANCELAR =
            "UPDATE apartados SET estado = 'cancelado' WHERE id_apartado = ? AND estado = 'activo'";

    private static final String SQL_LIQUIDAR =
            "UPDATE apartados SET estado = 'liquidado' WHERE id_apartado = ? AND estado = 'activo'";

    private static final String SQL_SALDO =
            "SELECT a.importe_anticipo, e.precio_venta "
                    + "FROM apartados a "
                    + "INNER JOIN ejemplares e ON a.id_ejemplar = e.id_ejemplar "
                    + "WHERE a.id_apartado = ? AND a.estado = 'activo' FOR UPDATE";

    private static final String SQL_PAGO =
            "INSERT INTO pagos (id_pago, id_venta, id_pedido, id_renta, id_apartado, monto, metodo_pago) "
                    + "VALUES (?, NULL, NULL, NULL, ?, ?, ?)";

    public List<Apartado> listar() throws DAOException {
        List<Apartado> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Apartado a = new Apartado();
                a.setIdApartado(rs.getString("id_apartado"));
                a.setIdEjemplar(rs.getString("id_ejemplar"));
                a.setFolio(rs.getString("folio"));
                a.setNombreCliente(rs.getString("cliente"));
                a.setNombreProducto(rs.getString("producto"));
                a.setNumeroSerie(rs.getString("numero_serie"));
                a.setFechaLimite(rs.getObject("fecha_limite", LocalDate.class));
                a.setImporteAnticipo(rs.getBigDecimal("importe_anticipo"));
                a.setEstado(rs.getString("estado"));
                lista.add(a);
            }
        } catch (SQLException e) {
            throw new DAOException("Error al listar apartados: " + e.getMessage(), e);
        }
        return lista;
    }

    public void registrar(String idCliente, String idEjemplar, LocalDate fechaLimite,
                          BigDecimal anticipo, String metodoPago, String idUsuario) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String idApartado = UUID.randomUUID().toString();
                String folio = "APT-" + System.currentTimeMillis();
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
                    ps.setString(1, idApartado);
                    ps.setString(2, folio);
                    ps.setString(3, idCliente);
                    ps.setString(4, idEjemplar);
                    ps.setObject(5, fechaLimite);
                    ps.setBigDecimal(6, anticipo);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(SQL_APARTAR)) {
                    ps.setString(1, idEjemplar);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("El ejemplar ya no esta disponible para apartado.");
                    }
                }
                Kardex.registrar(conn, idEjemplar, idUsuario, "salida", "apartado",
                        "Apartado folio " + folio);
                if (anticipo != null && anticipo.signum() > 0) {
                    registrarPago(conn, idApartado, anticipo, metodoPago);
                }
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo registrar el apartado"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }

    public void cancelar(String idApartado, String idEjemplar, String idUsuario) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(SQL_CANCELAR)) {
                    ps.setString(1, idApartado);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("El apartado no esta activo.");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(SQL_LIBERAR)) {
                    ps.setString(1, idEjemplar);
                    ps.executeUpdate();
                }
                Kardex.registrar(conn, idEjemplar, idUsuario, "entrada", "cancelacion",
                        "Cancelacion de apartado");
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo cancelar el apartado"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }

    public void liquidar(String idApartado, String idEjemplar, String metodoPago,
                         String idUsuario) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal saldo = BigDecimal.ZERO;
                try (PreparedStatement ps = conn.prepareStatement(SQL_SALDO)) {
                    ps.setString(1, idApartado);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("El apartado no esta activo.");
                        }
                        BigDecimal anticipo = rs.getBigDecimal("importe_anticipo");
                        BigDecimal precio = rs.getBigDecimal("precio_venta");
                        saldo = precio.subtract(anticipo);
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(SQL_LIQUIDAR)) {
                    ps.setString(1, idApartado);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(SQL_VENDER)) {
                    ps.setString(1, idEjemplar);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("El ejemplar no esta apartado.");
                    }
                }
                Kardex.registrar(conn, idEjemplar, idUsuario, "salida", "venta",
                        "Liquidacion de apartado");
                if (saldo.signum() > 0) {
                    registrarPago(conn, idApartado, saldo, metodoPago);
                }
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo liquidar el apartado"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }

    private void registrarPago(Connection conn, String idApartado, BigDecimal monto,
                               String metodoPago) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_PAGO)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, idApartado);
            ps.setBigDecimal(3, monto);
            ps.setString(4, metodoPago);
            ps.executeUpdate();
        }
    }
}
