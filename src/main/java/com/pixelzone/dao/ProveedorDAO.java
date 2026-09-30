package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Proveedor;
import com.pixelzone.util.Sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de {@code proveedores}.
 */
public class ProveedorDAO {

    private static final String SELECT_BASE =
            "SELECT id_proveedor, nombre, contacto, telefono, correo, direccion, activo "
                    + "FROM proveedores";

    private static final String SQL_LISTAR = SELECT_BASE + " ORDER BY nombre";
    private static final String SQL_ACTIVOS = SELECT_BASE + " WHERE activo = TRUE ORDER BY nombre";

    private static final String SQL_INSERT =
            "INSERT INTO proveedores (id_proveedor, nombre, contacto, telefono, correo, "
                    + "direccion, activo) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE proveedores SET nombre = ?, contacto = ?, telefono = ?, correo = ?, "
                    + "direccion = ?, activo = ? WHERE id_proveedor = ?";

    private static final String SQL_DELETE = "DELETE FROM proveedores WHERE id_proveedor = ?";

    public List<Proveedor> listar() throws DAOException {
        return consultar(SQL_LISTAR);
    }

    public List<Proveedor> listarActivos() throws DAOException {
        return consultar(SQL_ACTIVOS);
    }

    public void crear(Proveedor p) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, p.getIdProveedor());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getContacto());
            ps.setString(4, p.getTelefono());
            ps.setString(5, p.getCorreo());
            ps.setString(6, p.getDireccion());
            ps.setBoolean(7, p.isActivo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(Sql.mensajeAmigable(e, "No se pudo crear el proveedor"), e);
        }
    }

    public void actualizar(Proveedor p) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getContacto());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getCorreo());
            ps.setString(5, p.getDireccion());
            ps.setBoolean(6, p.isActivo());
            ps.setString(7, p.getIdProveedor());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(Sql.mensajeAmigable(e, "No se pudo actualizar el proveedor"), e);
        }
    }

    public void eliminar(String idProveedor) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setString(1, idProveedor);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(Sql.mensajeAmigable(e, "No se pudo eliminar el proveedor"), e);
        }
    }

    private List<Proveedor> consultar(String sql) throws DAOException {
        List<Proveedor> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Proveedor p = new Proveedor();
                p.setIdProveedor(rs.getString("id_proveedor"));
                p.setNombre(rs.getString("nombre"));
                p.setContacto(rs.getString("contacto"));
                p.setTelefono(rs.getString("telefono"));
                p.setCorreo(rs.getString("correo"));
                p.setDireccion(rs.getString("direccion"));
                p.setActivo(rs.getBoolean("activo"));
                lista.add(p);
            }
        } catch (SQLException e) {
            throw new DAOException("Error al consultar proveedores: " + e.getMessage(), e);
        }
        return lista;
    }
}
