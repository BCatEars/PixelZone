package com.pixelzone.ui;

import com.pixelzone.exception.DAOException;
import com.pixelzone.model.ItemCombo;
import com.pixelzone.session.UserSession;
import com.pixelzone.util.Numeros;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Esqueleto comun de los paneles CRUD: formulario a la izquierda (dentro de un
 * {@link JSplitPane}) y tabla de resultados a la derecha.
 *
 * <p>Las subclases agregan campos con {@link #campo}/{@link #campoAnchoCompleto},
 * botones con {@link #agregarBoton} y reaccionan a la seleccion en
 * {@link #alSeleccionar(int)}.</p>
 */
public abstract class PanelCrudBase extends JPanel implements Recargable {

    protected final UserSession sesion;
    protected final MainFrame frame;
    protected final DefaultTableModel modelo;
    protected final JTable tabla;
    protected final JPanel formulario = new JPanel(new GridBagLayout());

    private final JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final JLabel etiquetaEstado = new JLabel(" ");
    private int filaFormulario = 0;

    protected PanelCrudBase(UserSession sesion, MainFrame frame, String[] columnas) {
        this.sesion = sesion;
        this.frame = frame;
        this.modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tabla = new JTable(modelo);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) {
                alSeleccionar(tabla.convertRowIndexToModel(tabla.getSelectedRow()));
            }
        });

        formulario.setBorder(BorderFactory.createTitledBorder("Formulario"));
        JScrollPane scrollFormulario = new JScrollPane(formulario);
        scrollFormulario.setBorder(null);

        JPanel izquierda = new JPanel(new BorderLayout());
        izquierda.add(scrollFormulario, BorderLayout.CENTER);
        izquierda.add(botones, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                izquierda, new JScrollPane(tabla));
        split.setDividerLocation(430);
        split.setResizeWeight(0.0);
        split.setOneTouchExpandable(true);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(split, BorderLayout.CENTER);
        add(etiquetaEstado, BorderLayout.SOUTH);
    }

    /**
     * Invocado cuando el usuario selecciona una fila (indice del modelo).
     */
    protected abstract void alSeleccionar(int filaModelo);

    /**
     * Recarga datos y combos al mostrar el panel. Las subclases deben delegar
     * en su metodo de refresco (y recargar catalogos cuando aplique).
     */
    @Override
    public abstract void recargar();

    protected JLabel campo(String etiqueta, JComponent componente) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = filaFormulario;
        JLabel label = new JLabel(etiqueta);
        formulario.add(label, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        formulario.add(componente, gbc);
        filaFormulario++;
        return label;
    }

    protected void campoAnchoCompleto(JComponent componente) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.gridx = 0;
        gbc.gridy = filaFormulario;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        formulario.add(componente, gbc);
        filaFormulario++;
    }

    protected void agregarBoton(String texto, Accion accion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> ejecutar(accion));
        botones.add(boton);
    }

    protected void ejecutar(Accion accion) {
        try {
            accion.ejecutar();
        } catch (DAOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    protected void setMensaje(String mensaje) {
        etiquetaEstado.setText(mensaje);
    }

    protected String textoONull(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    protected boolean confirmar(String mensaje) {
        return JOptionPane.showConfirmDialog(this, mensaje, "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /**
     * @return el decimal capturado, o {@code null} si falta o es invalido
     *         (en cuyo caso ya se mostro el aviso correspondiente).
     */
    protected BigDecimal decimalRequerido(String texto, String nombreCampo) {
        BigDecimal valor = parseDecimal(texto, nombreCampo);
        if (valor == null) {
            aviso("Captura el " + nombreCampo + ".");
        }
        return valor;
    }

    protected BigDecimal decimalOpcional(String texto, String nombreCampo) {
        return parseDecimal(texto, nombreCampo);
    }

    private BigDecimal parseDecimal(String texto, String nombreCampo) {
        try {
            BigDecimal valor = Numeros.parseDecimal(texto);
            return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            aviso("El " + nombreCampo + " no es un numero valido.");
            return null;
        }
    }

    protected void seleccionarItem(JComboBox<ItemCombo> combo, String id) {
        if (id == null) {
            if (combo.getItemCount() > 0) {
                combo.setSelectedIndex(0);
            }
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (id.equals(combo.getItemAt(i).getId())) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    @FunctionalInterface
    protected interface Accion {
        void ejecutar() throws DAOException;
    }
}
