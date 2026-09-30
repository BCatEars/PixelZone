package com.pixelzone.ui;

import com.pixelzone.dao.ProductoDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.ItemCombo;
import com.pixelzone.model.Producto;
import com.pixelzone.session.UserSession;
import com.pixelzone.util.Numeros;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * CRUD de productos. Catalogos (categoria, plataforma) via combos; el tipo usa
 * los literales del ENUM.
 */
public class PanelProductos extends PanelCrudBase {

    private static final String[] COLUMNAS = {
            "Codigo", "Nombre", "Tipo", "Categoria", "Plataforma",
            "P. nuevo", "P. usado", "Rentable", "Activo"
    };

    private final ProductoDAO dao = new ProductoDAO();

    private final JTextField campoCodigo = new JTextField(12);
    private final JTextField campoCodigoBarras = new JTextField(14);
    private final JTextField campoNombre = new JTextField(22);
    private final JTextField campoDescripcion = new JTextField(22);
    private final JComboBox<String> comboTipo =
            new JComboBox<>(new String[]{"videojuego", "consola", "accesorio", "otro"});
    private final JTextField campoGenero = new JTextField(12);
    private final JTextField campoClasificacion = new JTextField(8);
    private final JTextField campoEdicion = new JTextField(12);
    private final JTextField campoFecha = new JTextField(10);
    private final JComboBox<ItemCombo> comboCategoria = new JComboBox<>();
    private final JComboBox<ItemCombo> comboPlataforma = new JComboBox<>();
    private final JTextField campoPrecioNuevo = new JTextField(10);
    private final JTextField campoPrecioUsado = new JTextField(10);
    private final JCheckBox checkRentable = new JCheckBox("Rentable");
    private final JCheckBox checkActivo = new JCheckBox("Activo", true);

    private List<Producto> filas = List.of();

    public PanelProductos(UserSession sesion, MainFrame frame) {
        super(sesion, frame, COLUMNAS);
        construirFormulario();
        cargarCombos();
        refrescar();
    }

    private void construirFormulario() {
        campo("Codigo interno:", campoCodigo);
        campo("Codigo de barras:", campoCodigoBarras);
        campo("Nombre:", campoNombre);
        campo("Descripcion:", campoDescripcion);
        campo("Tipo:", comboTipo);
        campo("Genero:", campoGenero);
        campo("Clasificacion:", campoClasificacion);
        campo("Edicion:", campoEdicion);
        campo("Fecha lanzamiento:", campoFecha);
        campo("Categoria:", comboCategoria);
        campo("Plataforma:", comboPlataforma);
        campo("Precio nuevo:", campoPrecioNuevo);
        campo("Precio usado:", campoPrecioUsado);
        campoAnchoCompleto(checkRentable);
        campoAnchoCompleto(checkActivo);

        agregarBoton("Guardar", this::guardar);
        agregarBoton("Actualizar", this::actualizar);
        agregarBoton("Limpiar", this::limpiar);
        agregarBoton("Eliminar", this::eliminar);
    }

    private void cargarCombos() {
        try {
            comboCategoria.removeAllItems();
            for (ItemCombo item : dao.listarCategorias()) {
                comboCategoria.addItem(item);
            }
            comboPlataforma.removeAllItems();
            comboPlataforma.addItem(new ItemCombo(null, "(sin plataforma)"));
            for (ItemCombo item : dao.listarPlataformas()) {
                comboPlataforma.addItem(item);
            }
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void refrescar() {
        try {
            filas = dao.listar();
            modelo.setRowCount(0);
            for (Producto p : filas) {
                modelo.addRow(new Object[]{
                        p.getCodigoInterno(), p.getNombre(), p.getTipo(),
                        p.getNombreCategoria(), p.getNombrePlataforma(),
                        p.getPrecioNuevo(), p.getPrecioUsado(),
                        p.isRentable() ? "Si" : "No", p.isActivo() ? "Si" : "No"});
            }
            setMensaje(filas.isEmpty() ? "Sin productos." : filas.size() + " productos.");
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void guardar() throws DAOException {
        Producto p = leerFormulario(null);
        if (p == null) {
            return;
        }
        dao.crear(p);
        frame.setMensaje("Producto creado: " + p.getNombre());
        limpiar();
        refrescar();
    }

    private void actualizar() throws DAOException {
        Producto seleccionado = productoSeleccionado();
        if (seleccionado == null) {
            aviso("Selecciona un producto de la tabla primero.");
            return;
        }
        Producto p = leerFormulario(seleccionado.getIdProducto());
        if (p == null) {
            return;
        }
        dao.actualizar(p);
        frame.setMensaje("Producto actualizado: " + p.getNombre());
        refrescar();
    }

    private void eliminar() throws DAOException {
        Producto seleccionado = productoSeleccionado();
        if (seleccionado == null) {
            aviso("Selecciona un producto de la tabla primero.");
            return;
        }
        if (!confirmar("¿Eliminar el producto " + seleccionado.getNombre() + "?")) {
            return;
        }
        dao.eliminar(seleccionado.getIdProducto());
        frame.setMensaje("Producto eliminado: " + seleccionado.getNombre());
        limpiar();
        refrescar();
    }

    private Producto leerFormulario(String idExistente) {
        String codigo = campoCodigo.getText().trim();
        if (codigo.isEmpty()) {
            aviso("Captura el codigo interno.");
            return null;
        }
        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            aviso("Captura el nombre.");
            return null;
        }
        ItemCombo categoria = (ItemCombo) comboCategoria.getSelectedItem();
        if (categoria == null) {
            aviso("No hay categorias cargadas.");
            return null;
        }
        LocalDate fecha;
        try {
            fecha = campoFecha.getText().trim().isEmpty()
                    ? null : LocalDate.parse(campoFecha.getText().trim());
        } catch (DateTimeParseException ex) {
            aviso("Fecha invalida: usa el formato AAAA-MM-DD.");
            return null;
        }
        BigDecimal precioNuevo = decimalOpcional(campoPrecioNuevo.getText(), "precio nuevo");
        BigDecimal precioUsado = decimalOpcional(campoPrecioUsado.getText(), "precio usado");

        Producto p = new Producto();
        p.setIdProducto(idExistente != null ? idExistente : UUID.randomUUID().toString());
        p.setIdCategoria(categoria.getId());
        ItemCombo plataforma = (ItemCombo) comboPlataforma.getSelectedItem();
        p.setIdPlataforma(plataforma == null ? null : plataforma.getId());
        p.setCodigoInterno(codigo);
        p.setCodigoBarras(textoONull(campoCodigoBarras.getText()));
        p.setNombre(nombre);
        p.setDescripcion(textoONull(campoDescripcion.getText()));
        p.setTipo((String) comboTipo.getSelectedItem());
        p.setGenero(textoONull(campoGenero.getText()));
        p.setClasificacion(textoONull(campoClasificacion.getText()));
        p.setEdicion(textoONull(campoEdicion.getText()));
        p.setFechaLanzamiento(fecha);
        p.setPrecioNuevo(precioNuevo);
        p.setPrecioUsado(precioUsado);
        p.setRentable(checkRentable.isSelected());
        p.setActivo(checkActivo.isSelected());
        return p;
    }

    private Producto productoSeleccionado() {
        int vista = tabla.getSelectedRow();
        if (vista < 0) {
            return null;
        }
        int fila = tabla.convertRowIndexToModel(vista);
        return (fila >= 0 && fila < filas.size()) ? filas.get(fila) : null;
    }

    @Override
    public void recargar() {
        cargarCombos();
        refrescar();
    }

    @Override
    protected void alSeleccionar(int filaModelo) {
        if (filaModelo < 0 || filaModelo >= filas.size()) {
            return;
        }
        Producto p = filas.get(filaModelo);
        campoCodigo.setText(p.getCodigoInterno());
        campoCodigoBarras.setText(p.getCodigoBarras() == null ? "" : p.getCodigoBarras());
        campoNombre.setText(p.getNombre());
        campoDescripcion.setText(p.getDescripcion() == null ? "" : p.getDescripcion());
        comboTipo.setSelectedItem(p.getTipo());
        campoGenero.setText(p.getGenero() == null ? "" : p.getGenero());
        campoClasificacion.setText(p.getClasificacion() == null ? "" : p.getClasificacion());
        campoEdicion.setText(p.getEdicion() == null ? "" : p.getEdicion());
        campoFecha.setText(p.getFechaLanzamiento() == null ? "" : p.getFechaLanzamiento().toString());
        seleccionarItem(comboCategoria, p.getIdCategoria());
        seleccionarItem(comboPlataforma, p.getIdPlataforma());
        campoPrecioNuevo.setText(Numeros.formatear(p.getPrecioNuevo()));
        campoPrecioUsado.setText(Numeros.formatear(p.getPrecioUsado()));
        checkRentable.setSelected(p.isRentable());
        checkActivo.setSelected(p.isActivo());
    }

    private void limpiar() {
        campoCodigo.setText("");
        campoCodigoBarras.setText("");
        campoNombre.setText("");
        campoDescripcion.setText("");
        comboTipo.setSelectedIndex(0);
        campoGenero.setText("");
        campoClasificacion.setText("");
        campoEdicion.setText("");
        campoFecha.setText("");
        if (comboCategoria.getItemCount() > 0) {
            comboCategoria.setSelectedIndex(0);
        }
        if (comboPlataforma.getItemCount() > 0) {
            comboPlataforma.setSelectedIndex(0);
        }
        campoPrecioNuevo.setText("");
        campoPrecioUsado.setText("");
        checkRentable.setSelected(false);
        checkActivo.setSelected(true);
        tabla.clearSelection();
    }
}
