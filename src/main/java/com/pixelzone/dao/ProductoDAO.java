package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.ItemCombo;
import com.pixelzone.model.Producto;
import com.pixelzone.util.Sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CRUD de {@code productos} y consultas de los catalogos relacionados
 * ({@code categorias}, {@code plataformas}).
 */
public class ProductoDAO {

    private static final String SELECT_BASE =
            "SELECT p.id_producto, p.id_categoria, p.id_plataforma, p.id_genero, p.codigo_interno, "
                    + "       p.codigo_barras, p.nombre, p.descripcion, c.tipo AS tipo, g.nombre AS genero, "
                    + "       p.clasificacion, p.edicion, p.fecha_lanzamiento, p.precio_nuevo, "
                    + "       p.precio_usado, p.rentable, p.activo, "
                    + "       c.nombre AS nombre_categoria, pl.nombre AS nombre_plataforma "
                    + "FROM productos p "
                    + "INNER JOIN categorias c ON p.id_categoria = c.id_categoria "
                    + "LEFT JOIN plataformas pl ON p.id_plataforma = pl.id_plataforma "
                    + "LEFT JOIN generos g ON g.id_genero = p.id_genero";

    private static final String SQL_LISTAR = SELECT_BASE + " ORDER BY p.nombre";
    private static final String SQL_ACTIVOS = SELECT_BASE + " WHERE p.activo = TRUE ORDER BY p.nombre";

    private static final String SQL_INSERT =
            "INSERT INTO productos (id_producto, id_categoria, id_plataforma, id_genero, codigo_interno, "
                    + "codigo_barras, nombre, descripcion, clasificacion, edicion, "
                    + "fecha_lanzamiento, precio_nuevo, precio_usado, rentable, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE productos SET id_categoria = ?, id_plataforma = ?, id_genero = ?, codigo_interno = ?, "
                    + "codigo_barras = ?, nombre = ?, descripcion = ?, "
                    + "clasificacion = ?, edicion = ?, fecha_lanzamiento = ?, precio_nuevo = ?, "
                    + "precio_usado = ?, rentable = ?, activo = ? WHERE id_producto = ?";

    private static final String SQL_DELETE = "DELETE FROM productos WHERE id_producto = ?";

    private static final String SQL_CATEGORIAS =
            "SELECT id_categoria, nombre FROM categorias ORDER BY nombre";

    private static final String SQL_PLATAFORMAS =
            "SELECT id_plataforma, nombre FROM plataformas ORDER BY nombre";

    private static final String SQL_TIPOS_CATEGORIA =
            "SELECT id_categoria, tipo FROM categorias";

    public List<Producto> listar() throws DAOException {
        return consultar(SQL_LISTAR);
    }

    public List<Producto> listarActivos() throws DAOException {
        return consultar(SQL_ACTIVOS);
    }

    public List<ItemCombo> listarCategorias() throws DAOException {
        return consultarCatalogo(SQL_CATEGORIAS);
    }

    public List<ItemCombo> listarPlataformas() throws DAOException {
        return consultarCatalogo(SQL_PLATAFORMAS);
    }

    /** @return mapa id_categoria -&gt; tipo (videojuego/consola/accesorio/otro). */
    public Map<String, String> tiposCategoria() throws DAOException {
        Map<String, String> tipos = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_TIPOS_CATEGORIA);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tipos.put(rs.getString("id_categoria"), rs.getString("tipo"));
            }
        } catch (SQLException e) {
            throw new DAOException("Error al consultar tipos de categoria: " + e.getMessage(), e);
        }
        return tipos;
    }

    public void crear(Producto p) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, p.getIdProducto());
            ps.setString(2, p.getIdCategoria());
            ps.setString(3, p.getIdPlataforma());
            ps.setString(4, p.getIdGenero());
            ps.setString(5, p.getCodigoInterno());
            ps.setString(6, p.getCodigoBarras());
            ps.setString(7, p.getNombre());
            ps.setString(8, p.getDescripcion());
            ps.setString(9, p.getClasificacion());
            ps.setString(10, p.getEdicion());
            ps.setObject(11, p.getFechaLanzamiento());
            ps.setBigDecimal(12, p.getPrecioNuevo());
            ps.setBigDecimal(13, p.getPrecioUsado());
            ps.setBoolean(14, p.isRentable());
            ps.setBoolean(15, p.isActivo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(Sql.mensajeAmigable(e, "No se pudo crear el producto"), e);
        }
    }

    public void actualizar(Producto p) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, p.getIdCategoria());
            ps.setString(2, p.getIdPlataforma());
            ps.setString(3, p.getIdGenero());
            ps.setString(4, p.getCodigoInterno());
            ps.setString(5, p.getCodigoBarras());
            ps.setString(6, p.getNombre());
            ps.setString(7, p.getDescripcion());
            ps.setString(8, p.getClasificacion());
            ps.setString(9, p.getEdicion());
            ps.setObject(10, p.getFechaLanzamiento());
            ps.setBigDecimal(11, p.getPrecioNuevo());
            ps.setBigDecimal(12, p.getPrecioUsado());
            ps.setBoolean(13, p.isRentable());
            ps.setBoolean(14, p.isActivo());
            ps.setString(15, p.getIdProducto());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(Sql.mensajeAmigable(e, "No se pudo actualizar el producto"), e);
        }
    }

    public void eliminar(String idProducto) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setString(1, idProducto);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(Sql.mensajeAmigable(e, "No se pudo eliminar el producto"), e);
        }
    }

    private List<Producto> consultar(String sql) throws DAOException {
        List<Producto> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto p = new Producto();
                p.setIdProducto(rs.getString("id_producto"));
                p.setIdCategoria(rs.getString("id_categoria"));
                p.setIdPlataforma(rs.getString("id_plataforma"));
                p.setCodigoInterno(rs.getString("codigo_interno"));
                p.setCodigoBarras(rs.getString("codigo_barras"));
                p.setNombre(rs.getString("nombre"));
                p.setDescripcion(rs.getString("descripcion"));
                p.setTipo(rs.getString("tipo"));
                p.setIdGenero(rs.getString("id_genero"));
                p.setGenero(rs.getString("genero"));
                p.setClasificacion(rs.getString("clasificacion"));
                p.setEdicion(rs.getString("edicion"));
                p.setFechaLanzamiento(rs.getObject("fecha_lanzamiento", LocalDate.class));
                p.setPrecioNuevo(rs.getBigDecimal("precio_nuevo"));
                p.setPrecioUsado(rs.getBigDecimal("precio_usado"));
                p.setRentable(rs.getBoolean("rentable"));
                p.setActivo(rs.getBoolean("activo"));
                p.setNombreCategoria(rs.getString("nombre_categoria"));
                p.setNombrePlataforma(rs.getString("nombre_plataforma"));
                lista.add(p);
            }
        } catch (SQLException e) {
            throw new DAOException("Error al consultar productos: " + e.getMessage(), e);
        }
        return lista;
    }

    private List<ItemCombo> consultarCatalogo(String sql) throws DAOException {
        List<ItemCombo> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new ItemCombo(rs.getString(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            throw new DAOException("Error al consultar catalogo: " + e.getMessage(), e);
        }
        return lista;
    }
}
