package com.pixelzone.ui;

import com.pixelzone.dao.ApartadoDAO;
import com.pixelzone.dao.ClienteDAO;
import com.pixelzone.dao.EjemplarDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Apartado;
import com.pixelzone.model.Cliente;
import com.pixelzone.model.Ejemplar;
import com.pixelzone.session.UserSession;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Apartados: alta con anticipo (+ pago), liquidacion y cancelacion.
 */
public class PanelApartados extends PanelCrudBase {

    private static final String[] COLUMNAS = {
            "Folio", "Cliente", "Producto", "Serie", "Fecha limite", "Anticipo", "Estado"
    };

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EjemplarDAO ejemplarDAO = new EjemplarDAO();
    private final ApartadoDAO apartadoDAO = new ApartadoDAO();

    private final ComboBuscable<Cliente> comboCliente = new ComboBuscable<>(Object::toString);
    private final ComboBuscable<Ejemplar> comboEjemplar = new ComboBuscable<>(Object::toString);
    private final JTextField campoFechaLimite = new JTextField(LocalDate.now().plusDays(15).toString(), 10);
    private final JTextField campoAnticipo = new JTextField("0", 10);
    private final JComboBox<String> comboMetodo =
            new JComboBox<>(new String[]{"efectivo", "tarjeta", "transferencia", "otro"});

    private List<Apartado> filas = List.of();

    public PanelApartados(UserSession sesion, MainFrame frame) {
        super(sesion, frame, COLUMNAS);
        construirFormulario();
        cargarCombos();
        refrescar();
    }

    private void construirFormulario() {
        campo("Cliente:", comboCliente);
        campo("Ejemplar disponible:", comboEjemplar);
        campo("Fecha limite:", campoFechaLimite);
        campo("Anticipo:", campoAnticipo);
        campo("Metodo de pago:", comboMetodo);

        agregarBoton("Registrar apartado", this::registrar);
        agregarBoton("Liquidar", this::liquidar);
        agregarBoton("Cancelar", this::cancelar);
        agregarBoton("Limpiar", this::limpiar);
    }

    private void cargarCombos() {
        try {
            comboCliente.setItems(clienteDAO.listarActivos());
            comboEjemplar.setItems(ejemplarDAO.listarDisponibles());
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void refrescar() {
        try {
            filas = apartadoDAO.listar();
            modelo.setRowCount(0);
            for (Apartado a : filas) {
                modelo.addRow(new Object[]{
                        a.getFolio(), a.getNombreCliente(), a.getNombreProducto(),
                        a.getNumeroSerie(), a.getFechaLimite(), a.getImporteAnticipo(), a.getEstado()});
            }
            setMensaje(filas.size() + " apartados.");
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void registrar() throws DAOException {
        Cliente cliente = (Cliente) comboCliente.getSelectedItem();
        if (cliente == null) {
            aviso("Selecciona un cliente.");
            return;
        }
        Ejemplar ejemplar = (Ejemplar) comboEjemplar.getSelectedItem();
        if (ejemplar == null) {
            aviso("No hay ejemplares disponibles.\n"
                    + "Crea copias en Inventario ▸ Ejemplares con estado 'disponible'.");
            return;
        }
        LocalDate fecha = parseFecha(campoFechaLimite.getText());
        if (fecha == null) {
            return;
        }
        apartadoDAO.registrar(cliente.getIdCliente(), ejemplar.getIdEjemplar(), fecha,
                decimalOpcional(campoAnticipo.getText(), "anticipo"),
                (String) comboMetodo.getSelectedItem(), sesion.getUsuario().getIdUsuario());
        frame.setMensaje("Apartado registrado para " + cliente.getNombre());
        cargarCombos();
        refrescar();
    }

    private void liquidar() throws DAOException {
        Apartado apartado = apartadoSeleccionado();
        if (apartado == null) {
            aviso("Selecciona un apartado de la tabla primero.");
            return;
        }
        if (!"activo".equals(apartado.getEstado())) {
            aviso("Solo se pueden liquidar apartados activos.");
            return;
        }
        apartadoDAO.liquidar(apartado.getIdApartado(), apartado.getIdEjemplar(),
                (String) comboMetodo.getSelectedItem(), sesion.getUsuario().getIdUsuario());
        frame.setMensaje("Apartado liquidado: " + apartado.getFolio());
        cargarCombos();
        refrescar();
    }

    private void cancelar() throws DAOException {
        Apartado apartado = apartadoSeleccionado();
        if (apartado == null) {
            aviso("Selecciona un apartado de la tabla primero.");
            return;
        }
        if (!"activo".equals(apartado.getEstado())) {
            aviso("Solo se pueden cancelar apartados activos.");
            return;
        }
        if (!confirmar("¿Cancelar el apartado " + apartado.getFolio() + "?")) {
            return;
        }
        apartadoDAO.cancelar(apartado.getIdApartado(), apartado.getIdEjemplar(),
                sesion.getUsuario().getIdUsuario());
        frame.setMensaje("Apartado cancelado: " + apartado.getFolio());
        cargarCombos();
        refrescar();
    }

    private Apartado apartadoSeleccionado() {
        int vista = tabla.getSelectedRow();
        if (vista < 0) {
            return null;
        }
        int fila = tabla.convertRowIndexToModel(vista);
        return (fila >= 0 && fila < filas.size()) ? filas.get(fila) : null;
    }

    private LocalDate parseFecha(String texto) {
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException ex) {
            aviso("Fecha invalida: usa el formato AAAA-MM-DD.");
            return null;
        }
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
        campoFechaLimite.setText(LocalDate.now().plusDays(15).toString());
        campoAnticipo.setText("0");
        comboMetodo.setSelectedIndex(0);
        tabla.clearSelection();
    }
}
