package com.pixelzone.security;

/**
 * Claves de permiso tal como estan sembradas en {@code permisos.nombre}.
 * No inventar valores nuevos: el DDL/DML estan congelados.
 */
public final class Permisos {

    public static final String GESTION_VENTAS = "GESTION_VENTAS";
    public static final String GESTION_INVENTARIO = "GESTION_INVENTARIO";
    public static final String GESTION_RENTAS = "GESTION_RENTAS";

    private Permisos() {
    }
}
