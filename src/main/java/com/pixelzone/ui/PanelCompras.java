package com.pixelzone.ui;

import com.pixelzone.dao.CompraDAO;
import com.pixelzone.dao.ProductoDAO;
import com.pixelzone.dao.ProveedorDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.LineaCompra;
import com.pixelzone.model.Producto;
import com.pixelzone.model.Proveedor;
import com.pixelzone.session.UserSession;
import com.pixelzone.util.Numeros;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Compras a proveedor: captura lineas (producto, cantidad, costo y precio de
 * venta) y registra la compra completa (compras + detalle + ejemplares + enlace
 * + kardex) en una transaccion de {@link CompraDAO}.
 */
public class PanelCompras extends JPanel implements Recargable {

    private static final String[] COLUMNAS = {
            "Producto", "Cantidad", "Costo unitario", "Precio venta", "Subtotal"
    };

    private final UserSession sesion;
    private final MainFrame frame;
    private final CompraDAO compraDAO = new CompraDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    private final ComboBuscable<Proveedor> comboProveedor = new ComboBuscable<>(Object::toString);
    private final ComboBuscable<Producto> comboProducto = new ComboBuscable<>(Object::toString);
    private final JTextField campoCantidad = new JTextField("1", 5);
    private final JTextField campoCosto = new JTextField(8);
    private final JTextField campoPrecioVenta = new JTextField(8);
    private final JLabel etiquetaTotal = new JLabel("0.00");
    private final JLabel etiquetaEstado = new JLabel(" ");
    private final DefaultTableModel modeloLineas = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaLineas = new JTable(modeloLineas);
    private final List<LineaCompra> lineas = new ArrayList<>();

    public PanelCompras(UserSession sesion, MainFrame frame) {
        this.sesion = sesion;
        this.frame = frame;
        construir();
        recargar();
    }

    private void construir() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        norte.add(cabecera());
        norte.add(lineaCaptura());

        JPanel sur = new JPanel(new BorderLayout());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton registrar = new JButton("Registrar compra");
        registrar.addActionListener(e -> registrar());
        JButton limpiar = new JButton("Limpiar");
        limpiar.addActionListener(e -> limpiar());
        botones.add(registrar);
        botones.add(limpiar);
        botones.add(new JLabel("Costo total:"));
        botones.add(etiquetaTotal);
        sur.add(botones, BorderLayout.NORTH);
        sur.add(etiquetaEstado, BorderLayout.SOUTH);

        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tablaLineas), BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
    }

    private JPanel cabecera() {
        JPanel cabecera = new JPanel(new GridBagLayout());
        cabecera.setBorder(BorderFactory.createTitledBorder("Compra"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        cabecera.add(new JLabel("Proveedor:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        cabecera.add(comboProveedor, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        cabecera.add(new JLabel("Registra:"), gbc);
        gbc.gridx = 1;
        cabecera.add(new JLabel(sesion.getUsuario().getNombreCompleto()), gbc);
        return cabecera;
    }

    private JPanel lineaCaptura() {
        JPanel linea = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        linea.setBorder(BorderFactory.createTitledBorder("Agregar linea"));
        linea.add(new JLabel("Producto:"));
        linea.add(comboProducto);
        linea.add(new JLabel("Cantidad:"));
        linea.add(campoCantidad);
        linea.add(new JLabel("Costo unitario:"));
        linea.add(campoCosto);
        linea.add(new JLabel("Precio venta:"));
        linea.add(campoPrecioVenta);
        JButton agregar = new JButton("Agregar");
        agregar.addActionListener(e -> agregarLinea());
        linea.add(agregar);
        JButton quitar = new JButton("Quitar seleccionada");
        quitar.addActionListener(e -> quitarLinea());
        linea.add(quitar);

        comboProducto.addActionListener(e -> sugerirPrecioVenta());
        return linea;
    }

    @Override
    public void recargar() {
        cargarProveedores();
        cargarProductos();
        refrescarLineas();
    }

    private void cargarProveedores() {
        try {
            Proveedor previo = comboProveedor.getSelectedItem();
            String idPrevio = previo == null ? null : previo.getIdProveedor();
            comboProveedor.setItems(proveedorDAO.listarActivos());
            if (idPrevio != null) {
                comboProveedor.preseleccionar(p -> idPrevio.equals(p.getIdProveedor()));
            }
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void cargarProductos() {
        try {
            Producto previo = comboProducto.getSelectedItem();
            String idPrevio = previo == null ? null : previo.getIdProducto();
            comboProducto.setItems(productoDAO.listarActivos());
            if (idPrevio != null) {
                comboProducto.preseleccionar(p -> idPrevio.equals(p.getIdProducto()));
            }
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void sugerirPrecioVenta() {
        Producto producto = (Producto) comboProducto.getSelectedItem();
        if (producto != null && producto.getPrecioNuevo() != null
                && campoPrecioVenta.getText().trim().isEmpty()) {
            campoPrecioVenta.setText(Numeros.formatear(producto.getPrecioNuevo()));
        }
    }

    private void agregarLinea() {
        Producto producto = (Producto) comboProducto.getSelectedItem();
        if (producto == null) {
            aviso("Selecciona un producto.");
            return;
        }
        int cantidad;
        try {
            cantidad = Integer.parseInt(campoCantidad.getText().trim());
        } catch (NumberFormatException ex) {
            aviso("La cantidad debe ser un numero entero.");
            return;
        }
        if (cantidad <= 0) {
            aviso("La cantidad debe ser mayor que 0.");
            return;
        }
        BigDecimal costo = parse(campoCosto.getText(), "costo unitario");
        if (costo == null) {
            return;
        }
        BigDecimal precio = parse(campoPrecioVenta.getText(), "precio de venta");
        if (precio == null) {
            return;
        }
        LineaCompra l = new LineaCompra();
        l.setIdProducto(producto.getIdProducto());
        l.setNombreProducto(producto.getNombre());
        l.setCantidad(cantidad);
        l.setCostoUnitario(costo);
        l.setPrecioVenta(precio);
        lineas.add(l);

        campoCantidad.setText("1");
        campoCosto.setText("");
        campoPrecioVenta.setText("");
        refrescarLineas();
    }

    private void quitarLinea() {
        int vista = tablaLineas.getSelectedRow();
        if (vista < 0) {
            aviso("Selecciona una linea de la tabla.");
            return;
        }
        int fila = tablaLineas.convertRowIndexToModel(vista);
        if (fila >= 0 && fila < lineas.size()) {
            lineas.remove(fila);
            refrescarLineas();
        }
    }

    private void registrar() {
        Proveedor proveedor = (Proveedor) comboProveedor.getSelectedItem();
        if (proveedor == null) {
            aviso("Selecciona el proveedor de la compra.");
            return;
        }
        if (lineas.isEmpty()) {
            aviso("Agrega al menos una linea a la compra.");
            return;
        }
        if (!confirmar("¿Registrar la compra a " + proveedor.getNombre()
                + " por " + etiquetaTotal.getText() + "?")) {
            return;
        }
        try {
            compraDAO.registrar(proveedor.getIdProveedor(),
                    sesion.getUsuario().getIdUsuario(), new ArrayList<>(lineas));
            frame.setMensaje("Compra registrada a " + proveedor.getNombre());
            limpiar();
            recargar();
        } catch (DAOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refrescarLineas() {
        modeloLineas.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (LineaCompra l : lineas) {
            modeloLineas.addRow(new Object[]{
                    l.getNombreProducto(), l.getCantidad(),
                    l.getCostoUnitario(), l.getPrecioVenta(), l.getSubtotal()});
            total = total.add(l.getSubtotal());
        }
        etiquetaTotal.setText(total.toPlainString());
        etiquetaEstado.setText(lineas.isEmpty()
                ? "Sin lineas. Selecciona producto, cantidad, costo y precio."
                : lineas.size() + " lineas en la compra.");
    }

    private void limpiar() {
        lineas.clear();
        campoCantidad.setText("1");
        campoCosto.setText("");
        campoPrecioVenta.setText("");
        tablaLineas.clearSelection();
        refrescarLineas();
    }

    private BigDecimal parse(String texto, String nombre) {
        try {
            BigDecimal valor = Numeros.parseDecimal(texto);
            if (valor == null) {
                aviso("Captura el " + nombre + ".");
                return null;
            }
            if (valor.signum() < 0) {
                aviso("El " + nombre + " no puede ser negativo.");
                return null;
            }
            return valor.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            aviso("El " + nombre + " no es un numero valido.");
            return null;
        }
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private boolean confirmar(String mensaje) {
        return JOptionPane.showConfirmDialog(this, mensaje, "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
