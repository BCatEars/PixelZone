package com.pixelzone.dao;

import com.pixelzone.config.DatabaseConfig;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Tabla;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Ejecuta las 5 vistas, los 5 procedimientos y las 5 funciones del esquema.
 * Los nombres se validan contra listas blancas antes de interpolarse en el SQL.
 */
public class ConsultaDAO {

    private static final Set<String> VISTAS = Set.of(
            "vw_inventario_ejemplares",
            "vw_historial_ventas",
            "vw_rentas_activas",
            "vw_pedidos_ecommerce",
            "vw_kardex_movimientos");

    private static final Set<String> PROCEDIMIENTOS = Set.of(
            "sp_productos_por_plataforma",
            "sp_ejemplares_por_estado",
            "sp_pagos_por_metodo",
            "sp_compras_proveedor",
            "sp_estado_apartados");

    private static final Set<String> FUNCIONES = Set.of(
            "fn_stock_disponible",
            "fn_total_venta_folio",
            "fn_ganancia_ejemplar",
            "fn_ingresos_por_metodo",
            "fn_rentas_activas_cliente");

    public Tabla vista(String nombre) throws DAOException {
        validar(VISTAS, nombre);
        return ejecutar("SELECT * FROM " + nombre);
    }

    public Tabla procedimiento(String nombre, String parametro) throws DAOException {
        validar(PROCEDIMIENTOS, nombre);
        return ejecutar("CALL " + nombre + "(?)", parametro);
    }

    public Object funcion(String nombre, String parametro) throws DAOException {
        validar(FUNCIONES, nombre);
        Tabla t = ejecutar("SELECT " + nombre + "(?) AS Resultado", parametro);
        if (t.getFilas().isEmpty() || t.getFilas().get(0).isEmpty()) {
            return null;
        }
        return t.getFilas().get(0).get(0);
    }

    private Tabla ejecutar(String sql, String... parametros) throws DAOException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setString(i + 1, parametros[i]);
            }
            boolean hayResultado = ps.execute();
            if (!hayResultado) {
                return new Tabla(List.of(), List.of());
            }
            try (ResultSet rs = ps.getResultSet()) {
                ResultSetMetaData md = rs.getMetaData();
                int columnas = md.getColumnCount();
                List<String> nombres = new ArrayList<>();
                for (int i = 1; i <= columnas; i++) {
                    nombres.add(md.getColumnLabel(i));
                }
                List<List<Object>> filas = new ArrayList<>();
                while (rs.next()) {
                    List<Object> fila = new ArrayList<>();
                    for (int i = 1; i <= columnas; i++) {
                        fila.add(rs.getObject(i));
                    }
                    filas.add(fila);
                }
                return new Tabla(nombres, filas);
            }
        } catch (SQLException e) {
            throw new DAOException("Error al ejecutar la consulta: " + e.getMessage(), e);
        }
    }

    private static void validar(Set<String> permitidos, String nombre) {
        if (!permitidos.contains(nombre)) {
            throw new IllegalArgumentException("Objeto SQL no permitido: " + nombre);
        }
    }
}
