package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Ejemplar;
import com.pixelzone.util.Sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Acceso a {@code ejemplares} y al kardex {@code movimientos_inventario}.
 *
 * <p>Toda escritura es transaccional: el cambio en {@code ejemplares} y su
 * movimiento de inventario se confirman juntos o no se confirman.</p>
 */
public class EjemplarDAO {

    private static final String SELECT_BASE =
            "SELECT e.id_ejemplar, e.id_producto, p.codigo_interno, p.nombre AS nombre_producto, "
                    + "       e.numero_serie, e.condicion, e.costo, e.precio_venta, e.estado, "
                    + "       e.tiene_caja, e.tiene_manual, e.observaciones, e.id_cliente_origen "
                    + "FROM ejemplares e "
                    + "INNER JOIN productos p ON e.id_producto = p.id_producto";

    private static final String SQL_LISTAR = SELECT_BASE + " ORDER BY p.nombre, e.numero_serie";
    private static final String SQL_DISPONIBLES =
            SELECT_BASE + " WHERE e.estado = 'disponible' ORDER BY p.nombre, e.numero_serie";
    private static final String SQL_RENTABLES =
            SELECT_BASE + " WHERE e.estado = 'disponible' AND p.rentable = TRUE "
                    + "ORDER BY p.nombre, e.numero_serie";

    private static final String SQL_INSERT =
            "INSERT INTO ejemplares (id_ejemplar, id_producto, numero_serie, condicion, costo, "
                    + "precio_venta, estado, tiene_caja, tiene_manual, observaciones, id_cliente_origen) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE ejemplares SET id_producto = ?, numero_serie = ?, condicion = ?, costo = ?, "
                    + "precio_venta = ?, estado = ?, tiene_caja = ?, tiene_manual = ?, "
                    + "observaciones = ?, id_cliente_origen = ? WHERE id_ejemplar = ?";

    private static final String SQL_ESTADO =
            "SELECT estado FROM ejemplares WHERE id_ejemplar = ? FOR UPDATE";

    private static final String SQL_BAJA =
            "UPDATE ejemplares SET estado = 'baja' WHERE id_ejemplar = ?";

    public List<Ejemplar> listar() throws DAOException {
        return consultar(SQL_LISTAR);
    }

    public List<Ejemplar> listarDisponibles() throws DAOException {
        return consultar(SQL_DISPONIBLES);
    }

    public List<Ejemplar> listarRentablesDisponibles() throws DAOException {
        return consultar(SQL_RENTABLES);
    }

    public void crear(Ejemplar e, String idUsuario) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                insertar(conn, e);
                Kardex.registrar(conn, e.getIdEjemplar(), idUsuario, "entrada", "ajuste_manual",
                        "Alta manual de ejemplar");
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(mensaje(ex, "No se pudo crear el ejemplar"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }

    public void actualizar(Ejemplar e, String idUsuario) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String estadoAnterior = estadoActual(conn, e.getIdEjemplar());
                actualizar(conn, e);
                if (estadoAnterior != null && !estadoAnterior.equals(e.getEstado())) {
                    Kardex.registrar(conn, e.getIdEjemplar(), idUsuario, tipoPara(e.getEstado()),
                            "ajuste_manual",
                            "Cambio de estado: " + estadoAnterior + " -> " + e.getEstado());
                }
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(mensaje(ex, "No se pudo actualizar el ejemplar"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }

    public void darDeBaja(String idEjemplar, String idUsuario) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String estadoAnterior = estadoActual(conn, idEjemplar);
                if (estadoAnterior == null) {
                    throw new SQLException("El ejemplar no existe.");
                }
                if (!"baja".equals(estadoAnterior)) {
                    try (PreparedStatement ps = conn.prepareStatement(SQL_BAJA)) {
                        ps.setString(1, idEjemplar);
                        ps.executeUpdate();
                    }
                    Kardex.registrar(conn, idEjemplar, idUsuario, "baja", "ajuste_manual",
                            "Baja manual de ejemplar (estado previo: " + estadoAnterior + ")");
                }
                conn.commit();
            } catch (SQLException ex) {
                Sql.rollback(conn, ex);
                throw new DAOException(mensaje(ex, "No se pudo dar de baja el ejemplar"), ex);
            }
        } catch (SQLException ex) {
            throw new DAOException("Error de conexion: " + ex.getMessage(), ex);
        }
    }

    private void insertar(Connection conn, Ejemplar e) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, e.getIdEjemplar());
            ps.setString(2, e.getIdProducto());
            ps.setString(3, e.getNumeroSerie());
            ps.setString(4, e.getCondicion());
            ps.setBigDecimal(5, e.getCosto());
            ps.setBigDecimal(6, e.getPrecioVenta());
            ps.setString(7, e.getEstado());
            ps.setBoolean(8, e.isTieneCaja());
            ps.setBoolean(9, e.isTieneManual());
            ps.setString(10, e.getObservaciones());
            ps.setString(11, e.getIdClienteOrigen());
            ps.executeUpdate();
        }
    }

    private void actualizar(Connection conn, Ejemplar e) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, e.getIdProducto());
            ps.setString(2, e.getNumeroSerie());
            ps.setString(3, e.getCondicion());
            ps.setBigDecimal(4, e.getCosto());
            ps.setBigDecimal(5, e.getPrecioVenta());
            ps.setString(6, e.getEstado());
            ps.setBoolean(7, e.isTieneCaja());
            ps.setBoolean(8, e.isTieneManual());
            ps.setString(9, e.getObservaciones());
            ps.setString(10, e.getIdClienteOrigen());
            ps.setString(11, e.getIdEjemplar());
            ps.executeUpdate();
        }
    }

    private String estadoActual(Connection conn, String idEjemplar) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_ESTADO)) {
            ps.setString(1, idEjemplar);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("estado") : null;
            }
        }
    }

    private List<Ejemplar> consultar(String sql) throws DAOException {
        List<Ejemplar> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DAOException("Error al listar ejemplares: " + e.getMessage(), e);
        }
        return lista;
    }

    private Ejemplar mapear(ResultSet rs) throws SQLException {
        Ejemplar e = new Ejemplar();
        e.setIdEjemplar(rs.getString("id_ejemplar"));
        e.setIdProducto(rs.getString("id_producto"));
        e.setCodigoProducto(rs.getString("codigo_interno"));
        e.setNombreProducto(rs.getString("nombre_producto"));
        e.setNumeroSerie(rs.getString("numero_serie"));
        e.setCondicion(rs.getString("condicion"));
        e.setCosto(rs.getBigDecimal("costo"));
        e.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        e.setEstado(rs.getString("estado"));
        e.setTieneCaja(rs.getBoolean("tiene_caja"));
        e.setTieneManual(rs.getBoolean("tiene_manual"));
        e.setObservaciones(rs.getString("observaciones"));
        e.setIdClienteOrigen(rs.getString("id_cliente_origen"));
        return e;
    }

    private static String tipoPara(String estadoNuevo) {
        return "baja".equals(estadoNuevo) ? "baja" : "ajuste";
    }

    private static String mensaje(SQLException ex, String base) {
        if (ex.getErrorCode() == 1062) {
            return "Numero de serie duplicado: ya existe un ejemplar con esa serie.";
        }
        return base + ": " + ex.getMessage();
    }
}
