package com.pixelzone.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Inserta un movimiento del kardex dentro de una transaccion ya abierta.
 */
final class Kardex {

    private static final String SQL =
            "INSERT INTO movimientos_inventario (id_movimiento, id_ejemplar, id_usuario, "
                    + "tipo_movimiento, motivo, cantidad, observaciones) VALUES (?, ?, ?, ?, ?, 1, ?)";

    private Kardex() {
    }

    static void registrar(Connection conn, String idEjemplar, String idUsuario,
                          String tipo, String motivo, String observaciones) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, idEjemplar);
            ps.setString(3, idUsuario);
            ps.setString(4, tipo);
            ps.setString(5, motivo);
            ps.setString(6, observaciones);
            ps.executeUpdate();
        }
    }
}
