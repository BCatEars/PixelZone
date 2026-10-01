package com.pixelzone.ui;

import com.pixelzone.dao.ClienteDAO;
import com.pixelzone.dao.EjemplarDAO;
import com.pixelzone.dao.VentaPOSDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Cliente;
import com.pixelzone.model.Ejemplar;
import com.pixelzone.session.UserSession;
import com.pixelzone.util.Numeros;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Punto de Venta: selecciona un cliente y uno o varios ejemplares disponibles,
 * calcula el total y delega la transaccion atomica a {@link VentaPOSDAO}.
 */
public class PanelPuntoDeVenta extends PanelCrudBase {

    private static final String[] COLUMNAS = {"Producto", "Serie", "Precio venta"};

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EjemplarDAO ejemplarDAO = new EjemplarDAO();
    private final VentaPOSDAO ventaDAO = new VentaPOSDAO();

    private final ComboBuscable<Cliente> comboCliente = new ComboBuscable<>(Object::toString);
    private final JComboBox<String> comboMetodo =
            new JComboBox<>(new String[]{"efectivo", "tarjeta", "transferencia", "otro"});
    private final JTextField campoDescuento = new JTextField("0", 8);
    private final JTextField campoImpuestos = new JTextField("0", 8);
    private final JLabel etiquetaTotal = new JLabel("0.00");

    private List<Ejemplar> disponibles = List.of();

    public PanelPuntoDeVenta(UserSession sesion, MainFrame frame) {
        super(sesion, frame, COLUMNAS);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        construirFormulario();
        cargarClientes();
        refrescar();
    }

    private void construirFormulario() {
        campo("Cliente:", comboCliente);
        campo("Metodo de pago:", comboMetodo);
        campo("Descuento:", campoDescuento);
        campo("Impuestos:", campoImpuestos);
        campo("Total:", etiquetaTotal);

        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarTotal();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarTotal();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarTotal();
            }
        };
        campoDescuento.getDocument().addDocumentListener(listener);
        campoImpuestos.getDocument().addDocumentListener(listener);

        agregarBoton("Cobrar venta", this::cobrar);
        agregarBoton("Limpiar", this::limpiar);
    }

    private void cargarClientes() {
        try {
            Cliente previo = comboCliente.getSelectedItem();
            String idPrevio = previo == null ? null : previo.getIdCliente();
            comboCliente.setItems(clienteDAO.listarActivos());
            if (idPrevio != null) {
                comboCliente.preseleccionar(c -> idPrevio.equals(c.getIdCliente()));
            } else {
                comboCliente.preseleccionar(c -> "anonimo".equals(c.getTipoCliente()));
            }
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void refrescar() {
        try {
            disponibles = ejemplarDAO.listarDisponibles();
            modelo.setRowCount(0);
            for (Ejemplar e : disponibles) {
                modelo.addRow(new Object[]{
                        e.getNombreProducto(), e.getNumeroSerie(), e.getPrecioVenta()});
            }
            actualizarTotal();
            setMensaje(disponibles.isEmpty()
                    ? "No hay ejemplares disponibles. Crea copias en Inventario ▸ Ejemplares."
                    : disponibles.size() + " ejemplares disponibles.");
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void cobrar() throws DAOException {
        Cliente cliente = (Cliente) comboCliente.getSelectedItem();
        if (cliente == null) {
            aviso("Selecciona un cliente.");
            return;
        }
        if (disponibles.isEmpty()) {
            aviso("No hay ejemplares disponibles para vender.\n"
                    + "Crea copias en Inventario ▸ Ejemplares con estado 'disponible'.");
            return;
        }
        List<Ejemplar> seleccionados = seleccionados();
        if (seleccionados.isEmpty()) {
            aviso("Selecciona al menos un ejemplar de la tabla.");
            return;
        }
        if (!confirmar("¿Cobrar " + etiquetaTotal.getText() + " a " + cliente.getNombre() + "?")) {
            return;
        }
        List<String> ids = new ArrayList<>();
        for (Ejemplar e : seleccionados) {
            ids.add(e.getIdEjemplar());
        }
        String folio = ventaDAO.registrarVenta(cliente.getIdCliente(),
                sesion.getUsuario().getIdUsuario(), ids,
                opcional(campoDescuento.getText()), opcional(campoImpuestos.getText()),
                (String) comboMetodo.getSelectedItem());
        frame.setMensaje("Venta registrada, folio " + folio);
        limpiar();
        refrescar();
    }

    private List<Ejemplar> seleccionados() {
        List<Ejemplar> lista = new ArrayList<>();
        for (int vista : tabla.getSelectedRows()) {
            int fila = tabla.convertRowIndexToModel(vista);
            if (fila >= 0 && fila < disponibles.size()) {
                lista.add(disponibles.get(fila));
            }
        }
        return lista;
    }

    private void actualizarTotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Ejemplar e : seleccionados()) {
            subtotal = subtotal.add(Objects.requireNonNullElse(e.getPrecioVenta(), BigDecimal.ZERO));
        }
        BigDecimal total = subtotal.subtract(opcional(campoDescuento.getText()))
                .add(opcional(campoImpuestos.getText()));
        etiquetaTotal.setText(total.toPlainString());
    }

    private BigDecimal opcional(String texto) {
        try {
            BigDecimal valor = Numeros.parseDecimal(texto);
            return valor == null ? BigDecimal.ZERO : valor;
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void recargar() {
        cargarClientes();
        refrescar();
    }

    @Override
    protected void alSeleccionar(int filaModelo) {
        actualizarTotal();
    }

    private void limpiar() {
        tabla.clearSelection();
        campoDescuento.setText("0");
        campoImpuestos.setText("0");
        etiquetaTotal.setText("0.00");
    }
}
