package com.pixelzone.ui;

import com.pixelzone.dao.ClienteDAO;
import com.pixelzone.dao.CompraUsadoDAO;
import com.pixelzone.dao.ProductoDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Cliente;
import com.pixelzone.model.CompraUsado;
import com.pixelzone.model.Producto;
import com.pixelzone.session.UserSession;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.util.List;

/**
 * Compra de ejemplares usados a clientes: crea el ejemplar, registra
 * {@code compras_usado} y el kardex en una transaccion.
 */
public class PanelCompraUsado extends PanelCrudBase {

    private static final String[] COLUMNAS = {
            "Producto", "Serie", "Cliente origen", "Precio compra"
    };

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CompraUsadoDAO compraDAO = new CompraUsadoDAO();

    private final JComboBox<Cliente> comboCliente = new JComboBox<>();
    private final JComboBox<Producto> comboProducto = new JComboBox<>();
    private final JTextField campoSerie = new JTextField(16);
    private final JTextField campoCosto = new JTextField(10);
    private final JTextField campoPrecioVenta = new JTextField(10);
    private final JTextField campoObservaciones = new JTextField(18);

    private List<CompraUsado> filas = List.of();

    public PanelCompraUsado(UserSession sesion, MainFrame frame) {
        super(sesion, frame, COLUMNAS);
        construirFormulario();
        cargarCombos();
        refrescar();
    }

    private void construirFormulario() {
        campo("Cliente origen:", comboCliente);
        campo("Producto:", comboProducto);
        campo("Numero de serie:", campoSerie);
        campo("Precio de compra:", campoCosto);
        campo("Precio de venta:", campoPrecioVenta);
        campo("Observaciones:", campoObservaciones);

        agregarBoton("Registrar compra", this::registrar);
        agregarBoton("Limpiar", this::limpiar);
    }

    private void cargarCombos() {
        try {
            comboCliente.removeAllItems();
            for (Cliente c : clienteDAO.listarActivos()) {
                comboCliente.addItem(c);
            }
            comboProducto.removeAllItems();
            for (Producto p : productoDAO.listarActivos()) {
                comboProducto.addItem(p);
            }
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void refrescar() {
        try {
            filas = compraDAO.listar();
            modelo.setRowCount(0);
            for (CompraUsado c : filas) {
                modelo.addRow(new Object[]{
                        c.getNombreProducto(), c.getNumeroSerie(),
                        c.getNombreCliente(), c.getPrecioCompra()});
            }
            setMensaje(filas.size() + " compras de usados.");
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void registrar() throws DAOException {
        Cliente cliente = (Cliente) comboCliente.getSelectedItem();
        if (cliente == null) {
            aviso("Selecciona el cliente que vende el ejemplar.");
            return;
        }
        Producto producto = (Producto) comboProducto.getSelectedItem();
        if (producto == null) {
            aviso("Selecciona el producto.");
            return;
        }
        String serie = campoSerie.getText().trim();
        if (serie.isEmpty()) {
            aviso("Captura el numero de serie.");
            return;
        }
        BigDecimal costo = decimalRequerido(campoCosto.getText(), "precio de compra");
        if (costo == null) {
            return;
        }
        BigDecimal precioVenta = decimalRequerido(campoPrecioVenta.getText(), "precio de venta");
        if (precioVenta == null) {
            return;
        }
        compraDAO.registrar(producto.getIdProducto(), serie, costo, precioVenta,
                cliente.getIdCliente(), sesion.getUsuario().getIdUsuario(),
                textoONull(campoObservaciones.getText()));
        frame.setMensaje("Compra de usado registrada: " + serie);
        limpiar();
        refrescar();
    }

    @Override
    public void recargar() {
        cargarCombos();
        refrescar();
    }

    @Override
    protected void alSeleccionar(int filaModelo) {
    }

    private void limpiar() {
        campoSerie.setText("");
        campoCosto.setText("");
        campoPrecioVenta.setText("");
        campoObservaciones.setText("");
        tabla.clearSelection();
    }
}
