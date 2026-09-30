package com.pixelzone.ui;

import com.pixelzone.dao.ConsultaDAO;

/**
 * Disparadores de los 5 procedimientos almacenados. Cada boton pide el
 * parametro (se puede dejar vacio cuando el SP lo usa como centinela) y
 * ejecuta {@code CALL sp_...(?)}.
 */
public class PanelProcedimientos extends PanelReporteBase {

    private static final String[][] PROCEDIMIENTOS = {
            {"sp_productos_por_plataforma", "Productos por plataforma", "PlayStation"},
            {"sp_ejemplares_por_estado", "Ejemplares por estado", "disponible"},
            {"sp_pagos_por_metodo", "Pagos por metodo", "efectivo"},
            {"sp_compras_proveedor", "Compras por proveedor", "Gamer"},
            {"sp_estado_apartados", "Estado de apartados", "APT-001"}
    };

    private final ConsultaDAO dao = new ConsultaDAO();

    public PanelProcedimientos(MainFrame frame) {
        super(frame);
        for (String[] sp : PROCEDIMIENTOS) {
            boton(sp[1], "CALL " + sp[0] + "(?)", () -> {
                String parametro = Parametros.pedir(this,
                        "Parametro para " + sp[0] + "\n(vacio = todos, si el SP lo permite):", sp[2]);
                if (parametro == null) {
                    return;
                }
                ejecutar("CALL " + sp[0], () -> dao.procedimiento(sp[0], parametro));
            });
        }
    }
}
