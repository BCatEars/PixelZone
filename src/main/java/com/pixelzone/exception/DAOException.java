package com.pixelzone.exception;

/**
 * Excepcion de la capa de acceso a datos.
 *
 * <p>Envuelve los {@link java.sql.SQLException} para que la interfaz no dependa
 * de detalles de JDBC.</p>
 */
public class DAOException extends Exception {

    private static final long serialVersionUID = 1L;

    public DAOException(String message) {
        super(message);
    }

    public DAOException(String message, Throwable cause) {
        super(message, cause);
    }
}
