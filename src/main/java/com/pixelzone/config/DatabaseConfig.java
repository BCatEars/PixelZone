package com.pixelzone.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Punto unico de obtencion de conexiones JDBC.
 *
 * <p>Lee la configuracion desde {@code db.properties} (classpath). No mantiene
 * conexiones abiertas: cada operacion pide una conexion y la cierra con
 * {@code try-with-resources}.</p>
 */
public final class DatabaseConfig {

    private static final String CONFIG_FILE = "db.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "No se encontro " + CONFIG_FILE + " en el classpath.");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Error al leer " + CONFIG_FILE, e);
        }
    }

    private DatabaseConfig() {
    }

    /**
     * Abre una conexion nueva a la base de datos {@code pixel_zone}.
     *
     * @return conexion JDBC lista para usarse (el llamador debe cerrarla).
     * @throws SQLException si el motor rechaza la conexion.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                require("db.url"),
                require("db.user"),
                PROPS.getProperty("db.password", "testing"));
    }

    private static String require(String key) {
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Falta la propiedad '" + key + "' en " + CONFIG_FILE);
        }
        return value;
    }
}
