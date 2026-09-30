package com.pixelzone.ui;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Panel de relleno para las pestanias aun no implementadas. Se sustituye por
 * el panel real conforme avanzan las fases.
 */
public class PanelPlaceholder extends JPanel {

    public PanelPlaceholder(String titulo, String detalle) {
        super(new GridBagLayout());
        JLabel etiqueta = new JLabel(
                "<html><div style='text-align:center'><b>" + titulo + "</b><br>"
                        + detalle + "</div></html>");
        etiqueta.setHorizontalAlignment(SwingConstants.CENTER);
        add(etiqueta);
    }
}
