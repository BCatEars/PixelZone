package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.CompraUsado;
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
 * Compra de un ejemplar usado a un cliente: crea el {@code ejemplar} fisico,
 * registra {@code compras_usado} y el kardex, en una sola transaccion.
 */
public class CompraUsadoDAO {

    private static final String SQL_LISTAR =
            "SELECT cu.id_compra_usado, p.nombre AS producto, e.numero_serie, "
                    + "       c.nombre AS cliente, cu.precio_compra "
                    + "FROM compras_usado cu "
                    + "INNER JOIN ejemplares e ON cu.id_ejemplar = e.id_ejemplar "
                    + "INNER JOIN productos p ON e.id_producto = p.id_producto "
                    + "INNER JOIN clientes c ON cu.id_cliente = c.id_cliente "
                    + "ORDER BY cu.fecha_compra DESC";

    private static final String SQL_INSERT_EJEMPLAR =
            "INSERT INTO ejemplares (id_ejemplar, id_producto, numero_serie, condicion, costo, "
                    + "precio_venta, estado, tiene_caja, tiene_manual, observaciones, id_cliente_origen) "
                    + "VALUES (?, ?, ?, 'usado', ?, ?, 'disponible', TRUE, TRUE, ?, ?)";

    private static final String SQL_INSERT_COMPRA =
            "INSERT INTO compras_usado (id_compra_usado, id_cliente, id_usuario, id_ejemplar, "
                    + "precio_compra, observaciones) VALUES (?, ?, ?, ?, ?, ?)";

    public List<CompraUsado> listar() throws DAOException {
        List<CompraUsado> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CompraUsado c = new CompraUsado();
                c.setIdCompraUsado(rs.getString("id_compra_usado"));
                c.setNombreProducto(rs.getString("producto"));
                c.setNumeroSerie(rs.getString("numero_serie"));
                c.setNombreCliente(rs.getString("cliente"));
                c.setPrecioCompra(rs.getBigDecimal("precio_compra"));
                lista.add(c);
            }
        } catch (SQLException e) {
            throw new DAOException("Error al listar compras de usados: " + e.getMessage(), e);
        }
        return lista;
    }

    public void registrar(String idProducto, String numeroSerie, BigDecimal precioCompra,
                          BigDecimal precioVenta, String idClienteOrigen, String idUsuario,
                          String observaciones) throws DAOException {
        String idEjemplar = UUID.randomUUID().toString();
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_EJEMPLAR)) {
                    ps.setString(1, idEjemplar);
                    ps.setString(2, idProducto);
                    ps.setString(3, numeroSerie);
                    ps.setBigDecimal(4, precioCompra);
                    ps.setBigDecimal(5, precioVenta);
                    ps.setString(6, observaciones);
                    ps.setString(7, idClienteOrigen);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_COMPRA)) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, idClienteOrigen);
                    ps.setString(3, idUsuario);
                    ps.setString(4, idEjemplar);
                    ps.setBigDecimal(5, precioCompra);
                    ps.setString(6, observaciones);
                    ps.executeUpdate();
                }
                Kardex.registrar(conn, idEjemplar, idUsuario, "entrada", "compra_usado",
                        "Compra de usado a cliente");
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo registrar la compra"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }
}
