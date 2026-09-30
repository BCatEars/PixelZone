package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Cliente;
import com.pixelzone.util.Sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de {@code clientes}.
 */
public class ClienteDAO {

    private static final String SELECT_BASE =
            "SELECT id_cliente, nombre, telefono, correo, tipo_cliente, activo FROM clientes";

    private static final String SQL_LISTAR = SELECT_BASE + " ORDER BY nombre";
    private static final String SQL_ACTIVOS = SELECT_BASE + " WHERE activo = TRUE ORDER BY nombre";

    private static final String SQL_INSERT =
            "INSERT INTO clientes (id_cliente, nombre, telefono, correo, tipo_cliente, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE clientes SET nombre = ?, telefono = ?, correo = ?, tipo_cliente = ?, activo = ? "
                    + "WHERE id_cliente = ?";

    private static final String SQL_DELETE = "DELETE FROM clientes WHERE id_cliente = ?";

    public List<Cliente> listar() throws DAOException {
        return consultar(SQL_LISTAR);
    }

    public List<Cliente> listarActivos() throws DAOException {
        return consultar(SQL_ACTIVOS);
    }

    public void crear(Cliente c) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, c.getIdCliente());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getTelefono());
            ps.setString(4, c.getCorreo());
            ps.setString(5, c.getTipoCliente());
            ps.setBoolean(6, c.isActivo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(Sql.mensajeAmigable(e, "No se pudo crear el cliente"), e);
        }
    }

    public void actualizar(Cliente c) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getTelefono());
            ps.setString(3, c.getCorreo());
            ps.setString(4, c.getTipoCliente());
            ps.setBoolean(5, c.isActivo());
            ps.setString(6, c.getIdCliente());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(Sql.mensajeAmigable(e, "No se pudo actualizar el cliente"), e);
        }
    }

    public void eliminar(String idCliente) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setString(1, idCliente);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(Sql.mensajeAmigable(e, "No se pudo eliminar el cliente"), e);
        }
    }

    private List<Cliente> consultar(String sql) throws DAOException {
        List<Cliente> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Cliente c = new Cliente();
                c.setIdCliente(rs.getString("id_cliente"));
                c.setNombre(rs.getString("nombre"));
                c.setTelefono(rs.getString("telefono"));
                c.setCorreo(rs.getString("correo"));
                c.setTipoCliente(rs.getString("tipo_cliente"));
                c.setActivo(rs.getBoolean("activo"));
                lista.add(c);
            }
        } catch (SQLException e) {
            throw new DAOException("Error al consultar clientes: " + e.getMessage(), e);
        }
        return lista;
    }
}
