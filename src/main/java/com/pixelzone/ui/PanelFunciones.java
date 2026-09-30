package com.pixelzone.ui;

import com.pixelzone.dao.ConsultaDAO;
import com.pixelzone.exception.DAOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Disparadores de las 5 funciones. Cada boton pide el parametro, ejecuta
 * {@code SELECT fn_...(?) AS Resultado} y muestra el escalar en una etiqueta.
 */
public class PanelFunciones extends JPanel {

    private static final String[][] FUNCIONES = {
            {"fn_stock_disponible", "Stock disponible", "VJ-001"},
            {"fn_total_venta_folio", "Total de venta", "VTA-001"},
            {"fn_ganancia_ejemplar", "Ganancia de ejemplar", "SN-SP2-001"},
            {"fn_ingresos_por_metodo", "Ingresos por metodo", "efectivo"},
            {"fn_rentas_activas_cliente", "Rentas activas de cliente", "Fernanda"}
    };

    private final MainFrame frame;
    private final ConsultaDAO dao = new ConsultaDAO();
    private final JLabel resultado = new JLabel(" ");

    public PanelFunciones(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        for (String[] funcion : FUNCIONES) {
            JButton boton = new JButton(funcion[1]);
            boton.setToolTipText("SELECT " + funcion[0] + "(?) AS Resultado");
            boton.addActionListener(e -> ejecutarFuncion(funcion));
            barra.add(boton);
        }

        resultado.setFont(resultado.getFont().deriveFont(Font.BOLD, 22f));
        resultado.setHorizontalAlignment(SwingConstants.CENTER);

        add(barra, BorderLayout.NORTH);
        add(resultado, BorderLayout.CENTER);
    }

    private void ejecutarFuncion(String[] funcion) {
        String parametro = Parametros.pedir(this, "Parametro para " + funcion[0] + ":", funcion[2]);
        if (parametro == null) {
            return;
        }
        try {
            Object valor = dao.funcion(funcion[0], parametro);
            resultado.setText(funcion[0] + "(\"" + parametro + "\") = "
                    + (valor == null ? "(sin resultado)" : valor));
            frame.setMensaje("SELECT " + funcion[0] + " -> " + valor);
        } catch (DAOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
