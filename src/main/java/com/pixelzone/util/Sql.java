package com.pixelzone.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Traduccion de errores SQL de MySQL a mensajes para el usuario y utilidades de
 * transaccion.
 */
public final class Sql {

    private Sql() {
    }

    public static String mensajeAmigable(SQLException ex, String base) {
        switch (ex.getErrorCode()) {
            case 1048:
                return "Falta un campo obligatorio.";
            case 1062:
                return "Valor duplicado: ya existe un registro con ese dato unico.";
            case 1451:
                return "No se puede eliminar: el registro tiene operaciones relacionadas.";
            case 1452:
                return "Referencia invalida: el registro relacionado no existe.";
            default:
                return base + ": " + ex.getMessage();
        }
    }

    public static void rollback(Connection conn, SQLException original) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            original.addSuppressed(e);
        }
    }
}
