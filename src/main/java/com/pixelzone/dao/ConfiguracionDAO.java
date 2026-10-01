package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Lectura de parametros de negocio de la tabla {@code configuracion}
 * (clave/valor). Ej.: {@code recargo_por_dia} de rentas.
 */
public class ConfiguracionDAO {

    private static final String SQL_VALOR =
            "SELECT valor FROM configuracion WHERE clave = ?";

    /**
     * @return el valor numerico de la clave, o {@code null} si no existe.
     * @throws DAOException si el valor no es numerico o falla la consulta.
     */
    public BigDecimal obtenerDecimal(String clave) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_VALOR)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                try {
                    return new BigDecimal(rs.getString("valor"));
                } catch (NumberFormatException ex) {
                    throw new DAOException("La configuracion '" + clave + "' no es numerica.");
                }
            }
        } catch (SQLException ex) {
            throw new DAOException("Error al leer configuracion: " + ex.getMessage(), ex);
        }
    }
}
