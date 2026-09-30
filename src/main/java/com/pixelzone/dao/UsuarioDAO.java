package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Empleado;
import com.pixelzone.model.ItemCombo;
import com.pixelzone.model.Usuario;
import com.pixelzone.session.UserSession;
import com.pixelzone.util.Sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Acceso a {@code usuarios}, {@code perfiles}, {@code permisos} y
 * {@code perfil_permisos}: autenticacion y gestion de empleados.
 */
public class UsuarioDAO {

    private static final String SQL_AUTENTICAR =
            "SELECT u.id_usuario, u.id_perfil, u.nombre_usuario, u.nombre_completo, "
                    + "       u.contrasena, p.nombre AS nombre_perfil "
                    + "FROM usuarios u "
                    + "INNER JOIN perfiles p ON u.id_perfil = p.id_perfil "
                    + "WHERE u.nombre_usuario = ? AND u.activo = TRUE";

    private static final String SQL_PERMISOS =
            "SELECT p.nombre "
                    + "FROM permisos p "
                    + "INNER JOIN perfil_permisos pp ON p.id_permiso = pp.id_permiso "
                    + "WHERE pp.id_perfil = ?";

    private static final String SQL_LISTAR =
            "SELECT u.id_usuario, u.id_perfil, u.nombre_usuario, u.contrasena, "
                    + "       u.nombre_completo, u.correo, u.activo, p.nombre AS nombre_perfil "
                    + "FROM usuarios u "
                    + "INNER JOIN perfiles p ON u.id_perfil = p.id_perfil "
                    + "ORDER BY u.nombre_usuario";

    private static final String SQL_PERFILES =
            "SELECT id_perfil, nombre FROM perfiles ORDER BY nombre";

    private static final String SQL_INSERT =
            "INSERT INTO usuarios (id_usuario, id_perfil, nombre_usuario, contrasena, "
                    + "nombre_completo, correo, activo) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE usuarios SET id_perfil = ?, nombre_usuario = ?, contrasena = ?, "
                    + "nombre_completo = ?, correo = ?, activo = ? WHERE id_usuario = ?";

    private static final String SQL_DELETE = "DELETE FROM usuarios WHERE id_usuario = ?";

    /**
     * Valida credenciales y carga los permisos del perfil.
     *
     * @return la sesion si las credenciales son correctas y el usuario esta
     *         activo; {@code null} en caso contrario.
     */
    public UserSession autenticar(String nombreUsuario, String contrasena) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_AUTENTICAR)) {

            ps.setString(1, nombreUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                // Demo academico: contrasenas almacenadas en texto plano.
                if (!rs.getString("contrasena").equals(contrasena)) {
                    return null;
                }

                Usuario usuario = new Usuario(
                        rs.getString("id_usuario"),
                        rs.getString("id_perfil"),
                        rs.getString("nombre_usuario"),
                        rs.getString("nombre_completo"),
                        rs.getString("nombre_perfil"));

                Set<String> permisos = cargarPermisos(conn, usuario.getIdPerfil());
                return new UserSession(usuario, permisos);
            }
        } catch (SQLException e) {
            throw new DAOException("Error al autenticar al usuario: " + e.getMessage(), e);
        }
    }

    public List<Empleado> listar() throws DAOException {
        List<Empleado> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Empleado e = new Empleado();
                e.setIdUsuario(rs.getString("id_usuario"));
                e.setIdPerfil(rs.getString("id_perfil"));
                e.setNombrePerfil(rs.getString("nombre_perfil"));
                e.setNombreUsuario(rs.getString("nombre_usuario"));
                e.setContrasena(rs.getString("contrasena"));
                e.setNombreCompleto(rs.getString("nombre_completo"));
                e.setCorreo(rs.getString("correo"));
                e.setActivo(rs.getBoolean("activo"));
                lista.add(e);
            }
        } catch (SQLException e) {
            throw new DAOException("Error al listar usuarios: " + e.getMessage(), e);
        }
        return lista;
    }

    public List<ItemCombo> listarPerfiles() throws DAOException {
        List<ItemCombo> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_PERFILES);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new ItemCombo(rs.getString("id_perfil"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new DAOException("Error al listar perfiles: " + e.getMessage(), e);
        }
        return lista;
    }

    public void crear(Empleado e) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, e.getIdUsuario());
            ps.setString(2, e.getIdPerfil());
            ps.setString(3, e.getNombreUsuario());
            ps.setString(4, e.getContrasena());
            ps.setString(5, e.getNombreCompleto());
            ps.setString(6, e.getCorreo());
            ps.setBoolean(7, e.isActivo());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo crear el usuario"), ex);
        }
    }

    public void actualizar(Empleado e) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, e.getIdPerfil());
            ps.setString(2, e.getNombreUsuario());
            ps.setString(3, e.getContrasena());
            ps.setString(4, e.getNombreCompleto());
            ps.setString(5, e.getCorreo());
            ps.setBoolean(6, e.isActivo());
            ps.setString(7, e.getIdUsuario());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo actualizar el usuario"), ex);
        }
    }

    public void eliminar(String idUsuario) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setString(1, idUsuario);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DAOException(Sql.mensajeAmigable(ex, "No se pudo eliminar el usuario"), ex);
        }
    }

    private Set<String> cargarPermisos(Connection conn, String idPerfil) throws SQLException {
        Set<String> permisos = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_PERMISOS)) {
            ps.setString(1, idPerfil);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permisos.add(rs.getString("nombre"));
                }
            }
        }
        return permisos;
    }
}
