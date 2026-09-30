package com.pixelzone.ui;

import com.pixelzone.dao.ConsultaDAO;

/**
 * Disparadores de las 5 vistas: cada boton ejecuta
 * {@code SELECT * FROM vw_...} y vuelca el resultado en la tabla.
 */
public class PanelVistas extends PanelReporteBase {

    private static final String[][] VISTAS = {
            {"vw_inventario_ejemplares", "Inventario de ejemplares"},
            {"vw_historial_ventas", "Historial de ventas"},
            {"vw_rentas_activas", "Rentas activas / vencidas"},
            {"vw_pedidos_ecommerce", "Pedidos e-commerce"},
            {"vw_kardex_movimientos", "Kardex de movimientos"}
    };

    private final ConsultaDAO dao = new ConsultaDAO();

    public PanelVistas(MainFrame frame) {
        super(frame);
        for (String[] vista : VISTAS) {
            boton(vista[1], "SELECT * FROM " + vista[0],
                    () -> ejecutar("Vista " + vista[0], () -> dao.vista(vista[0])));
        }
    }
}
