package com.pixelzone.ui;

import com.pixelzone.dao.ConfiguracionDAO;
import com.pixelzone.dao.RentaDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.DevolucionRenta;
import com.pixelzone.model.Renta;
import com.pixelzone.session.UserSession;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Devolucion de rentas (individual o en lote del mismo cliente). Muestra solo
 * rentas activas/vencidas con el atraso y el recargo calculado
 * ({@code configuracion.recargo_por_dia}). La condicion y el monto extra se
 * editan por fila en la tabla.
 */
public class PanelDevolucionesRenta extends PanelCrudBase {

    private static final int COL_CONDICION = 8;
    private static final int COL_MONTO_EXTRA = 9;

    private static final String[] COLUMNAS = {
            "Cliente", "Producto", "Serie", "Fecha limite", "Dias atraso",
            "Recargo", "Deposito", "Estado renta", "Condicion", "Monto extra"
    };

    private final RentaDAO rentaDAO = new RentaDAO();
    private final ConfiguracionDAO configuracionDAO = new ConfiguracionDAO();

    private final JLabel etiquetaCliente = new JLabel("-");
    private final JLabel etiquetaSerie = new JLabel("-");
    private final JLabel etiquetaFechaLimite = new JLabel("-");
    private final JLabel etiquetaDiasAtraso = new JLabel("-");
    private final JLabel etiquetaRecargo = new JLabel("-");
    private final JLabel etiquetaDeposito = new JLabel("-");
    private final JComboBox<String> comboMetodoReembolso =
            new JComboBox<>(new String[]{"efectivo", "tarjeta", "transferencia", "otro"});

    private List<Renta> filas = List.of();
    private final List<DevolucionRenta> devoluciones = new ArrayList<>();
    private BigDecimal recargoPorDia = BigDecimal.ZERO;

    public PanelDevolucionesRenta(UserSession sesion, MainFrame frame) {
        super(sesion, frame, COLUMNAS);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabla.setDefaultRenderer(Object.class, rendererSemaforo());
        tabla.getColumnModel().getColumn(COL_CONDICION).setCellEditor(new DefaultCellEditor(
                new JComboBox<>(new String[]{"buena", "con_desgaste", "danado", "incompleto"})));
        modelo.addTableModelListener(this::alEditarCelda);
        construirFormulario();
        recargar();
    }

    private void construirFormulario() {
        campo("Cliente:", etiquetaCliente);
        campo("Serie:", etiquetaSerie);
        campo("Fecha limite:", etiquetaFechaLimite);
        campo("Dias de atraso:", etiquetaDiasAtraso);
        campo("Recargo por atraso:", etiquetaRecargo);
        campo("Deposito (a reembolsar):", etiquetaDeposito);
        campo("Metodo reembolso:", comboMetodoReembolso);

        agregarBoton("Registrar devolucion", this::registrarDevolucion);
        agregarBoton("Limpiar", this::limpiar);
    }

    private DefaultTableCellRenderer rendererSemaforo() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected,
                        hasFocus, row, column);
                if (!isSelected) {
                    int modeloFila = table.convertRowIndexToModel(row);
                    if (modeloFila >= 0 && modeloFila < filas.size()) {
                        long dias = diasAtraso(filas.get(modeloFila));
                        Color color = dias <= 0 ? new Color(0xD9F2D9)
                                : dias <= 3 ? new Color(0xFFF3CC)
                                : new Color(0xFFD9D9);
                        c.setBackground(color);
                    }
                }
                return c;
            }
        };
    }

    private void alEditarCelda(TableModelEvent e) {
        if (e.getType() != TableModelEvent.UPDATE) {
            return;
        }
        int fila = e.getFirstRow();
        int columna = e.getColumn();
        if (fila < 0 || fila >= devoluciones.size() || columna < 0) {
            return;
        }
        DevolucionRenta d = devoluciones.get(fila);
        Object valor = modelo.getValueAt(fila, columna);
        if (columna == COL_CONDICION) {
            d.setCondicionRetorno(valor == null ? "buena" : valor.toString());
        } else if (columna == COL_MONTO_EXTRA) {
            d.setMontoExtra(aDecimal(valor));
        }
    }

    @Override
    protected boolean celdaEditable(int fila, int columna) {
        return columna == COL_CONDICION || columna == COL_MONTO_EXTRA;
    }

    @Override
    public void recargar() {
        try {
            BigDecimal valor = configuracionDAO.obtenerDecimal("recargo_por_dia");
            recargoPorDia = valor == null ? BigDecimal.ZERO : valor;
        } catch (DAOException ex) {
            aviso(ex.getMessage());
            recargoPorDia = BigDecimal.ZERO;
        }
        refrescar();
    }

    private void refrescar() {
        try {
            filas = rentaDAO.listarPendientes();
            devoluciones.clear();
            modelo.setRowCount(0);
            for (Renta r : filas) {
                DevolucionRenta d = new DevolucionRenta();
                d.setIdRenta(r.getIdRenta());
                d.setIdEjemplar(r.getIdEjemplar());
                d.setCondicionRetorno("buena");
                d.setMontoExtra(recargo(r));
                devoluciones.add(d);
                modelo.addRow(new Object[]{
                        r.getNombreCliente(), r.getNombreProducto(), r.getNumeroSerie(),
                        r.getFechaLimite(), diasAtraso(r), recargo(r), r.getDeposito(),
                        r.getEstado(), d.getCondicionRetorno(), d.getMontoExtra()});
            }
            setMensaje(filas.isEmpty()
                    ? "Sin rentas por devolver."
                    : filas.size() + " rentas por devolver. Edita condicion/monto extra por fila.");
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void registrarDevolucion() throws DAOException {
        int[] vistas = tabla.getSelectedRows();
        if (vistas.length == 0) {
            aviso("Selecciona al menos una renta de la tabla.");
            return;
        }
        List<DevolucionRenta> seleccion = new ArrayList<>();
        String idCliente = null;
        StringBuilder series = new StringBuilder();
        BigDecimal totalExtra = BigDecimal.ZERO;
        BigDecimal totalReembolso = BigDecimal.ZERO;
        for (int vista : vistas) {
            int fila = tabla.convertRowIndexToModel(vista);
            if (fila < 0 || fila >= filas.size()) {
                continue;
            }
            Renta r = filas.get(fila);
            if (idCliente == null) {
                idCliente = r.getIdCliente();
            } else if (!idCliente.equals(r.getIdCliente())) {
                aviso("Selecciona rentas del mismo cliente.");
                return;
            }
            DevolucionRenta d = devoluciones.get(fila);
            seleccion.add(d);
            series.append("\n  - ").append(r.getNumeroSerie());
            totalExtra = totalExtra.add(d.getMontoExtra() == null ? BigDecimal.ZERO : d.getMontoExtra());
            totalReembolso = totalReembolso.add(r.getDeposito() == null ? BigDecimal.ZERO : r.getDeposito());
        }
        if (seleccion.isEmpty()) {
            aviso("No hay rentas validas seleccionadas.");
            return;
        }
        if (!confirmar("¿Registrar " + seleccion.size() + " devolucion(es)?" + series
                + "\n\nReembolso de depositos: " + totalReembolso.toPlainString()
                + "\nCobro por recargo/extra: " + totalExtra.toPlainString())) {
            return;
        }
        rentaDAO.devolverLote(seleccion, sesion.getUsuario().getIdUsuario(),
                (String) comboMetodoReembolso.getSelectedItem());
        frame.setMensaje("Devoluciones registradas: " + seleccion.size());
        limpiar();
        recargar();
    }

    private long diasAtraso(Renta r) {
        if (r.getFechaLimite() == null) {
            return 0;
        }
        return Math.max(ChronoUnit.DAYS.between(r.getFechaLimite(), LocalDate.now()), 0);
    }

    private BigDecimal recargo(Renta r) {
        return recargoPorDia.multiply(BigDecimal.valueOf(diasAtraso(r)));
    }

    private BigDecimal aDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valor.toString().trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    protected void alSeleccionar(int filaModelo) {
        if (filaModelo < 0 || filaModelo >= filas.size()) {
            return;
        }
        Renta r = filas.get(filaModelo);
        etiquetaCliente.setText(r.getNombreCliente());
        etiquetaSerie.setText(r.getNumeroSerie());
        etiquetaFechaLimite.setText(String.valueOf(r.getFechaLimite()));
        etiquetaDiasAtraso.setText(String.valueOf(diasAtraso(r)));
        etiquetaRecargo.setText(recargo(r).toPlainString());
        etiquetaDeposito.setText(r.getDeposito() == null ? "0.00" : r.getDeposito().toPlainString());
    }

    private void limpiar() {
        etiquetaCliente.setText("-");
        etiquetaSerie.setText("-");
        etiquetaFechaLimite.setText("-");
        etiquetaDiasAtraso.setText("-");
        etiquetaRecargo.setText("-");
        etiquetaDeposito.setText("-");
        comboMetodoReembolso.setSelectedIndex(0);
        tabla.clearSelection();
    }
}
