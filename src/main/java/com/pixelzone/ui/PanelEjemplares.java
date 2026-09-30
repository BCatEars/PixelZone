package com.pixelzone.ui;

import com.pixelzone.dao.ClienteDAO;
import com.pixelzone.dao.EjemplarDAO;
import com.pixelzone.dao.ProductoDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Cliente;
import com.pixelzone.model.Ejemplar;
import com.pixelzone.model.Producto;
import com.pixelzone.session.UserSession;
import com.pixelzone.util.Numeros;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * CRUD de {@code ejemplares}. Panel de referencia del patron
 * formulario + tabla, con kardex sincronizado.
 */
public class PanelEjemplares extends PanelCrudBase {

    private static final String[] COLUMNAS = {
            "Producto", "Serie", "Condicion", "Estado", "Costo", "Precio venta",
            "Caja", "Manual", "Observaciones"
    };

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EjemplarDAO ejemplarDAO = new EjemplarDAO();

    private final JComboBox<Producto> comboProducto = new JComboBox<>();
    private final JTextField campoSerie = new JTextField(16);
    private final JComboBox<String> comboCondicion = new JComboBox<>(new String[]{"nuevo", "usado"});
    private final JComboBox<String> comboEstado = new JComboBox<>(
            new String[]{"disponible", "rentado", "vendido", "apartado", "reservado", "baja"});
    private final JTextField campoCosto = new JTextField(12);
    private final JTextField campoPrecio = new JTextField(12);
    private final JCheckBox checkCaja = new JCheckBox("Tiene caja", true);
    private final JCheckBox checkManual = new JCheckBox("Tiene manual", true);
    private final JTextField campoObservaciones = new JTextField(16);
    private final JComboBox<Cliente> comboClienteOrigen = new JComboBox<>();

    private JLabel etiquetaClienteOrigen;
    private List<Ejemplar> filas = List.of();

    public PanelEjemplares(UserSession sesion, MainFrame frame) {
        super(sesion, frame, COLUMNAS);
        construirFormulario();
        cargarCombos();
        refrescar();
    }

    private void construirFormulario() {
        campo("Producto:", comboProducto);
        campo("Numero de serie:", campoSerie);
        campo("Condicion:", comboCondicion);
        campo("Estado:", comboEstado);
        campo("Costo:", campoCosto);
        campo("Precio de venta:", campoPrecio);
        campoAnchoCompleto(checkCaja);
        campoAnchoCompleto(checkManual);
        campo("Observaciones:", campoObservaciones);
        etiquetaClienteOrigen = campo("Cliente origen:", comboClienteOrigen);

        agregarBoton("Guardar", () -> guardar());
        agregarBoton("Actualizar", () -> actualizar());
        agregarBoton("Limpiar", this::limpiar);
        agregarBoton("Dar de baja", () -> darDeBaja());

        comboCondicion.addActionListener(e -> actualizarHabilitadoClienteOrigen());
        comboProducto.addActionListener(e -> sugerirPrecio());
        actualizarHabilitadoClienteOrigen();
    }

    private void cargarCombos() {
        try {
            comboProducto.removeAllItems();
            for (Producto p : productoDAO.listarActivos()) {
                comboProducto.addItem(p);
            }
            comboClienteOrigen.removeAllItems();
            for (Cliente c : clienteDAO.listarActivos()) {
                comboClienteOrigen.addItem(c);
            }
            comboClienteOrigen.setSelectedIndex(-1);
        } catch (DAOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error al cargar catalogos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refrescar() {
        try {
            filas = ejemplarDAO.listar();
            modelo.setRowCount(0);
            for (Ejemplar e : filas) {
                modelo.addRow(new Object[]{
                        e.getNombreProducto(), e.getNumeroSerie(), e.getCondicion(), e.getEstado(),
                        e.getCosto(), e.getPrecioVenta(),
                        e.isTieneCaja() ? "Si" : "No", e.isTieneManual() ? "Si" : "No",
                        e.getObservaciones()});
            }
            setMensaje(filas.isEmpty() ? "Sin ejemplares." : filas.size() + " ejemplares.");
        } catch (DAOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error al cargar ejemplares", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardar() throws DAOException {
        Ejemplar e = leerFormulario(null);
        if (e == null) {
            return;
        }
        ejemplarDAO.crear(e, sesion.getUsuario().getIdUsuario());
        frame.setMensaje("Ejemplar creado: " + e.getNumeroSerie());
        limpiar();
        refrescar();
    }

    private void actualizar() throws DAOException {
        Ejemplar seleccionado = ejemplarSeleccionado();
        if (seleccionado == null) {
            aviso("Selecciona un ejemplar de la tabla primero.");
            return;
        }
        Ejemplar e = leerFormulario(seleccionado.getIdEjemplar());
        if (e == null) {
            return;
        }
        ejemplarDAO.actualizar(e, sesion.getUsuario().getIdUsuario());
        frame.setMensaje("Ejemplar actualizado: " + e.getNumeroSerie());
        refrescar();
    }

    private void darDeBaja() throws DAOException {
        Ejemplar seleccionado = ejemplarSeleccionado();
        if (seleccionado == null) {
            aviso("Selecciona un ejemplar de la tabla primero.");
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Dar de baja la serie " + seleccionado.getNumeroSerie() + "?",
                "Confirmar baja", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        ejemplarDAO.darDeBaja(seleccionado.getIdEjemplar(), sesion.getUsuario().getIdUsuario());
        frame.setMensaje("Ejemplar dado de baja: " + seleccionado.getNumeroSerie());
        limpiar();
        refrescar();
    }

    private Ejemplar leerFormulario(String idExistente) {
        Producto producto = (Producto) comboProducto.getSelectedItem();
        if (producto == null) {
            aviso("No hay productos cargados.");
            return null;
        }
        String serie = campoSerie.getText().trim();
        if (serie.isEmpty()) {
            aviso("Captura el numero de serie.");
            return null;
        }
        BigDecimal costo = parseDecimal(campoCosto.getText(), "costo");
        if (costo == null) {
            return null;
        }
        BigDecimal precio = parseDecimal(campoPrecio.getText(), "precio de venta");
        if (precio == null) {
            return null;
        }
        if (costo.signum() < 0 || precio.signum() < 0) {
            aviso("Costo y precio no pueden ser negativos.");
            return null;
        }

        Ejemplar e = new Ejemplar();
        e.setIdEjemplar(idExistente != null ? idExistente : UUID.randomUUID().toString());
        e.setIdProducto(producto.getIdProducto());
        e.setNumeroSerie(serie);
        e.setCondicion((String) comboCondicion.getSelectedItem());
        e.setEstado((String) comboEstado.getSelectedItem());
        e.setCosto(costo);
        e.setPrecioVenta(precio);
        e.setTieneCaja(checkCaja.isSelected());
        e.setTieneManual(checkManual.isSelected());
        String observaciones = campoObservaciones.getText().trim();
        e.setObservaciones(observaciones.isEmpty() ? null : observaciones);
        Cliente origen = (Cliente) comboClienteOrigen.getSelectedItem();
        e.setIdClienteOrigen("usado".equals(e.getCondicion()) && origen != null
                ? origen.getIdCliente() : null);
        return e;
    }

    private BigDecimal parseDecimal(String texto, String nombreCampo) {
        try {
            BigDecimal valor = Numeros.parseDecimal(texto);
            if (valor == null) {
                aviso("Captura el " + nombreCampo + ".");
                return null;
            }
            return valor.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            aviso("El " + nombreCampo + " no es un numero valido.");
            return null;
        }
    }

    private Ejemplar ejemplarSeleccionado() {
        int vista = tabla.getSelectedRow();
        if (vista < 0) {
            return null;
        }
        int fila = tabla.convertRowIndexToModel(vista);
        return (fila >= 0 && fila < filas.size()) ? filas.get(fila) : null;
    }

    @Override
    protected void alSeleccionar(int filaModelo) {
        if (filaModelo < 0 || filaModelo >= filas.size()) {
            return;
        }
        Ejemplar e = filas.get(filaModelo);
        seleccionarProducto(e.getIdProducto());
        campoSerie.setText(e.getNumeroSerie());
        comboCondicion.setSelectedItem(e.getCondicion());
        comboEstado.setSelectedItem(e.getEstado());
        campoCosto.setText(Numeros.formatear(e.getCosto()));
        campoPrecio.setText(Numeros.formatear(e.getPrecioVenta()));
        checkCaja.setSelected(e.isTieneCaja());
        checkManual.setSelected(e.isTieneManual());
        campoObservaciones.setText(e.getObservaciones() == null ? "" : e.getObservaciones());
        seleccionarCliente(e.getIdClienteOrigen());
        actualizarHabilitadoClienteOrigen();
    }

    private void limpiar() {
        campoSerie.setText("");
        comboCondicion.setSelectedIndex(0);
        comboEstado.setSelectedItem("disponible");
        campoCosto.setText("");
        campoPrecio.setText("");
        checkCaja.setSelected(true);
        checkManual.setSelected(true);
        campoObservaciones.setText("");
        if (comboClienteOrigen.getItemCount() > 0) {
            comboClienteOrigen.setSelectedIndex(-1);
        }
        tabla.clearSelection();
        actualizarHabilitadoClienteOrigen();
    }

    private void sugerirPrecio() {
        Producto producto = (Producto) comboProducto.getSelectedItem();
        if (producto == null) {
            return;
        }
        BigDecimal sugerido = "usado".equals(comboCondicion.getSelectedItem())
                ? producto.getPrecioUsado() : producto.getPrecioNuevo();
        if (sugerido != null) {
            campoPrecio.setText(Numeros.formatear(sugerido));
        }
    }

    private void actualizarHabilitadoClienteOrigen() {
        boolean usado = "usado".equals(comboCondicion.getSelectedItem());
        comboClienteOrigen.setEnabled(usado);
        etiquetaClienteOrigen.setEnabled(usado);
        if (!usado && comboClienteOrigen.getItemCount() > 0) {
            comboClienteOrigen.setSelectedIndex(-1);
        }
    }

    private void seleccionarProducto(String idProducto) {
        for (int i = 0; i < comboProducto.getItemCount(); i++) {
            if (comboProducto.getItemAt(i).getIdProducto().equals(idProducto)) {
                comboProducto.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarCliente(String idCliente) {
        if (idCliente == null) {
            if (comboClienteOrigen.getItemCount() > 0) {
                comboClienteOrigen.setSelectedIndex(-1);
            }
            return;
        }
        for (int i = 0; i < comboClienteOrigen.getItemCount(); i++) {
            if (comboClienteOrigen.getItemAt(i).getIdCliente().equals(idCliente)) {
                comboClienteOrigen.setSelectedIndex(i);
                return;
            }
        }
    }
}
