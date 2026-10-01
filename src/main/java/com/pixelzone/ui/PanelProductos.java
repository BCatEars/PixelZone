package com.pixelzone.ui;

import com.pixelzone.dao.GeneroDAO;
import com.pixelzone.dao.ProductoDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Genero;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD de productos. Categoria y plataforma son catalogos (combos buscables);
 * genero es un catalogo ({@code generos}) y clasificacion es un ENUM ESRB. Solo
 * los videojuegos pueden tener genero/clasificacion, asi que esos campos se
 * deshabilitan y limpian cuando la categoria elegida no es de tipo videojuego.
 */
public class PanelProductos extends PanelCrudBase {

    private static final String SIN_CLASIFICACION = "(ninguna)";

    private static final String[] COLUMNAS = {
            "Codigo", "Nombre", "Categoria", "Plataforma",
            "P. nuevo", "P. usado", "Rentable", "Activo"
    };

    private final ProductoDAO dao = new ProductoDAO();
    private final GeneroDAO generoDAO = new GeneroDAO();

    private final JTextField campoCodigo = new JTextField(12);
    private final JTextField campoCodigoBarras = new JTextField(14);
    private final JTextField campoNombre = new JTextField(22);
    private final JTextField campoDescripcion = new JTextField(22);
    private final ComboBuscable<Genero> comboGenero = new ComboBuscable<>(Object::toString);
    private final JComboBox<String> comboClasificacion = new JComboBox<>(
            new String[]{SIN_CLASIFICACION, "E", "E10+", "T", "M", "AO", "RP"});
    private final JTextField campoEdicion = new JTextField(12);
    private final JTextField campoFecha = new JTextField(10);
    private final ComboBuscable<ItemCombo> comboCategoria = new ComboBuscable<>(Object::toString);
    private final ComboBuscable<ItemCombo> comboPlataforma = new ComboBuscable<>(Object::toString);
    private final JTextField campoPrecioNuevo = new JTextField(10);
    private final JTextField campoPrecioUsado = new JTextField(10);
    private final JCheckBox checkRentable = new JCheckBox("Rentable");
    private final JCheckBox checkActivo = new JCheckBox("Activo", true);

    private List<Producto> filas = List.of();
    private Map<String, String> tipoPorCategoria = Map.of();

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
        campo("Categoria:", comboCategoria);
        campo("Genero:", comboGenero);
        campo("Clasificacion (ESRB):", comboClasificacion);
        campo("Edicion:", campoEdicion);
        campo("Fecha lanzamiento:", campoFecha);
        campo("Plataforma:", comboPlataforma);
        campo("Precio nuevo (referencia):", campoPrecioNuevo);
        campo("Precio usado (referencia):", campoPrecioUsado);
        campoPrecioNuevo.setToolTipText("Precio de lista para una copia nueva. Solo sugiere el precio al crear un ejemplar o comprar stock.");
        campoPrecioUsado.setToolTipText("Precio de lista para una copia usada. Aplica a ejemplares con condicion 'usado'.");
        campoAnchoCompleto(checkRentable);
        campoAnchoCompleto(checkActivo);

        comboCategoria.addActionListener(e -> actualizarHabilitadoPorCategoria());

        agregarBoton("Guardar", this::guardar);
        agregarBoton("Actualizar", this::actualizar);
        agregarBoton("Limpiar", this::limpiar);
        agregarBoton("Eliminar", this::eliminar);
    }

    private void cargarCombos() {
        try {
            tipoPorCategoria = dao.tiposCategoria();

            ItemCombo categoriaPrevia = comboCategoria.getSelectedItem();
            String idCategoria = categoriaPrevia == null ? null : categoriaPrevia.getId();
            comboCategoria.setItems(dao.listarCategorias());
            if (idCategoria != null) {
                comboCategoria.preseleccionar(i -> idCategoria.equals(i.getId()));
            }

            ItemCombo plataformaPrevia = comboPlataforma.getSelectedItem();
            String idPlataforma = plataformaPrevia == null ? null : plataformaPrevia.getId();
            List<ItemCombo> plataformas = new ArrayList<>();
            plataformas.add(new ItemCombo(null, "(sin plataforma)"));
            plataformas.addAll(dao.listarPlataformas());
            comboPlataforma.setItems(plataformas);
            if (idPlataforma != null) {
                comboPlataforma.preseleccionar(i -> idPlataforma.equals(i.getId()));
            }

            Genero generoPrevio = comboGenero.getSelectedItem();
            String idGenero = generoPrevio == null ? null : generoPrevio.getIdGenero();
            comboGenero.setItems(generoDAO.listar());
            if (idGenero != null) {
                comboGenero.preseleccionar(g -> idGenero.equals(g.getIdGenero()));
            }
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
        actualizarHabilitadoPorCategoria();
    }

    private void actualizarHabilitadoPorCategoria() {
        ItemCombo categoria = comboCategoria.getSelectedItem();
        boolean videojuego = categoria != null
                && "videojuego".equals(tipoPorCategoria.get(categoria.getId()));
        comboGenero.setEnabled(videojuego);
        comboClasificacion.setEnabled(videojuego);
        if (!videojuego) {
            comboGenero.setSelectedItem(null);
            comboClasificacion.setSelectedItem(SIN_CLASIFICACION);
        }
    }

    private void refrescar() {
        try {
            filas = dao.listar();
            modelo.setRowCount(0);
            for (Producto p : filas) {
                modelo.addRow(new Object[]{
                        p.getCodigoInterno(), p.getNombre(),
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
        ItemCombo categoria = comboCategoria.getSelectedItem();
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
        ItemCombo plataforma = comboPlataforma.getSelectedItem();
        p.setIdPlataforma(plataforma == null ? null : plataforma.getId());
        Genero genero = comboGenero.isEnabled() ? comboGenero.getSelectedItem() : null;
        p.setIdGenero(genero == null ? null : genero.getIdGenero());
        String clasificacion = comboClasificacion.isEnabled()
                ? (String) comboClasificacion.getSelectedItem() : null;
        p.setClasificacion(clasificacion == null || SIN_CLASIFICACION.equals(clasificacion)
                ? null : clasificacion);
        p.setCodigoInterno(codigo);
        p.setCodigoBarras(textoONull(campoCodigoBarras.getText()));
        p.setNombre(nombre);
        p.setDescripcion(textoONull(campoDescripcion.getText()));
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
        campoEdicion.setText(p.getEdicion() == null ? "" : p.getEdicion());
        campoFecha.setText(p.getFechaLanzamiento() == null ? "" : p.getFechaLanzamiento().toString());
        seleccionarItem(comboCategoria, p.getIdCategoria());
        seleccionarItem(comboPlataforma, p.getIdPlataforma());
        if (p.getIdGenero() != null) {
            String idGenero = p.getIdGenero();
            comboGenero.preseleccionar(g -> idGenero.equals(g.getIdGenero()));
        } else {
            comboGenero.setSelectedItem(null);
        }
        comboClasificacion.setSelectedItem(
                p.getClasificacion() == null ? SIN_CLASIFICACION : p.getClasificacion());
        campoPrecioNuevo.setText(Numeros.formatear(p.getPrecioNuevo()));
        campoPrecioUsado.setText(Numeros.formatear(p.getPrecioUsado()));
        checkRentable.setSelected(p.isRentable());
        checkActivo.setSelected(p.isActivo());
        actualizarHabilitadoPorCategoria();
    }

    private void limpiar() {
        campoCodigo.setText("");
        campoCodigoBarras.setText("");
        campoNombre.setText("");
        campoDescripcion.setText("");
        campoEdicion.setText("");
        campoFecha.setText("");
        if (comboCategoria.getItemCount() > 0) {
            comboCategoria.setSelectedIndex(0);
        }
        if (comboPlataforma.getItemCount() > 0) {
            comboPlataforma.setSelectedIndex(0);
        }
        comboGenero.setSelectedItem(null);
        comboClasificacion.setSelectedItem(SIN_CLASIFICACION);
        campoPrecioNuevo.setText("");
        campoPrecioUsado.setText("");
        checkRentable.setSelected(false);
        checkActivo.setSelected(true);
        tabla.clearSelection();
        actualizarHabilitadoPorCategoria();
    }
}
