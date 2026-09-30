package com.pixelzone.ui;

import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Tabla;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Base de los paneles de reporte: barra de botones arriba y tabla dinamica
 * (columnas tomadas del {@link java.sql.ResultSetMetaData}).
 */
public abstract class PanelReporteBase extends JPanel {

    protected final MainFrame frame;
    protected final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    protected final JTable tabla = new JTable(modelo);
    protected final JPanel barraSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));

    private final JLabel etiquetaEstado = new JLabel(" ");

    protected PanelReporteBase(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tabla.setAutoCreateRowSorter(true);
        add(barraSuperior, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(etiquetaEstado, BorderLayout.SOUTH);
    }

    protected void boton(String texto, String tooltip, Runnable accion) {
        JButton boton = new JButton(texto);
        if (tooltip != null) {
            boton.setToolTipText(tooltip);
        }
        boton.addActionListener(e -> accion.run());
        barraSuperior.add(boton);
    }

    protected void mostrar(Tabla resultado) {
        modelo.setColumnCount(0);
        modelo.setRowCount(0);
        for (String columna : resultado.getColumnas()) {
            modelo.addColumn(columna);
        }
        for (List<Object> fila : resultado.getFilas()) {
            modelo.addRow(fila.toArray());
        }
        setMensaje(resultado.getFilas().size() + " filas.");
    }

    protected void ejecutar(String descripcion, ProveedorTabla proveedor) {
        try {
            Tabla resultado = proveedor.obtener();
            mostrar(resultado);
            frame.setMensaje(descripcion + " -> " + resultado.getFilas().size() + " filas.");
        } catch (DAOException ex) {
            setMensaje("Error.");
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void setMensaje(String mensaje) {
        etiquetaEstado.setText(mensaje);
    }

    @FunctionalInterface
    protected interface ProveedorTabla {
        Tabla obtener() throws DAOException;
    }
}
