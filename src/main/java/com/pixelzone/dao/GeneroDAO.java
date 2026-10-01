package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Genero;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lectura del catalogo {@code generos} (para el combo de productos).
 */
public class GeneroDAO {

    private static final String SQL_LISTAR =
            "SELECT id_genero, nombre, descripcion FROM generos ORDER BY nombre";

    public List<Genero> listar() throws DAOException {
        List<Genero> lista = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Genero g = new Genero();
                g.setIdGenero(rs.getString("id_genero"));
                g.setNombre(rs.getString("nombre"));
                g.setDescripcion(rs.getString("descripcion"));
                lista.add(g);
            }
        } catch (SQLException e) {
            throw new DAOException("Error al listar generos: " + e.getMessage(), e);
        }
        return lista;
    }
}
