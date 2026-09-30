package com.pixelzone.model;

import java.util.List;

/**
 * Resultado tabular generico de una consulta (columnas + filas), para no
 * arrastrar un {@link java.sql.ResultSet} fuera de su conexion.
 */
public class Tabla {

    private final List<String> columnas;
    private final List<List<Object>> filas;

    public Tabla(List<String> columnas, List<List<Object>> filas) {
        this.columnas = columnas;
        this.filas = filas;
    }

    public List<String> getColumnas() {
        return columnas;
    }

    public List<List<Object>> getFilas() {
        return filas;
    }
}
