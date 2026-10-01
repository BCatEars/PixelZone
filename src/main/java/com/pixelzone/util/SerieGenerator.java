package com.pixelzone.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Genera numeros de serie unicos con el patron {@code SN-<codigo_interno>-NNN}
 * a partir del codigo interno del producto. Cuenta las series existentes con el
 * mismo prefijo y salta colisiones.
 *
 * <p>Debe invocarse dentro de la misma conexion/transaccion que inserta el
 * ejemplar, para que el conteo vea las filas ya insertadas en el mismo lote.</p>
 */
public final class SerieGenerator {

    private static final String SQL_CODIGO =
            "SELECT codigo_interno FROM productos WHERE id_producto = ?";
    private static final String SQL_CONTAR =
            "SELECT COUNT(*) FROM ejemplares WHERE numero_serie LIKE ?";
    private static final String SQL_EXISTE =
            "SELECT 1 FROM ejemplares WHERE numero_serie = ?";

    private SerieGenerator() {
    }

    /**
     * @return una serie unica para un ejemplar del producto indicado.
     */
    public static String generar(Connection conn, String idProducto) throws SQLException {
        String codigo = "PZ";
        try (PreparedStatement ps = conn.prepareStatement(SQL_CODIGO)) {
            ps.setString(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    codigo = rs.getString(1);
                }
            }
        }
        String base = "SN-" + codigo + "-";
        int siguiente = 1;
        try (PreparedStatement ps = conn.prepareStatement(SQL_CONTAR)) {
            ps.setString(1, base + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    siguiente = rs.getInt(1) + 1;
                }
            }
        }
        String serie;
        do {
            serie = base + String.format("%03d", siguiente++);
        } while (existe(conn, serie));
        return serie;
    }

    private static boolean existe(Connection conn, String serie) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_EXISTE)) {
            ps.setString(1, serie);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
