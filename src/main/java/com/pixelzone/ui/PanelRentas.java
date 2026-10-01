package com.pixelzone.ui;

import com.pixelzone.dao.ClienteDAO;
import com.pixelzone.dao.EjemplarDAO;
import com.pixelzone.dao.RentaDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Cliente;
import com.pixelzone.model.Ejemplar;
import com.pixelzone.model.Renta;
import com.pixelzone.session.UserSession;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Rentas: solo el alta (el ejemplar pasa a 'rentado' y se registra el cobro de
 * renta + depósito). La devolución vive en {@link PanelDevolucionesRenta}.
 */
public class PanelRentas extends PanelCrudBase {

    private static final String[] COLUMNAS = {
            "Cliente", "Producto", "Serie", "Fecha limite", "Monto", "Deposito", "Estado renta"
    };

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EjemplarDAO ejemplarDAO = new EjemplarDAO();
    private final RentaDAO rentaDAO = new RentaDAO();

    private final ComboBuscable<Cliente> comboCliente = new ComboBuscable<>(Object::toString);
    private final ComboBuscable<Ejemplar> comboEjemplar = new ComboBuscable<>(Object::toString);
    private final JTextField campoFechaLimite = new JTextField(LocalDate.now().plusDays(7).toString(), 10);
    private final JTextField campoMonto = new JTextField(10);
    private final JTextField campoDeposito = new JTextField("0", 10);
    private final JComboBox<String> comboMetodoPago =
            new JComboBox<>(new String[]{"efectivo", "tarjeta", "transferencia", "otro"});

    private List<Renta> filas = List.of();

    public PanelRentas(UserSession sesion, MainFrame frame) {
        super(sesion, frame, COLUMNAS);
        construirFormulario();
        cargarCombos();
        refrescar();
    }

    private void construirFormulario() {
        campo("Cliente:", comboCliente);
        campo("Ejemplar rentable:", comboEjemplar);
        campo("Fecha limite:", campoFechaLimite);
        campo("Monto renta:", campoMonto);
        campo("Deposito (garantia):", campoDeposito);
        campo("Metodo de pago:", comboMetodoPago);

        agregarBoton("Registrar renta", this::registrar);
        agregarBoton("Limpiar", this::limpiar);
    }

    private void cargarCombos() {
        try {
            comboCliente.setItems(clienteDAO.listarActivos());
            comboEjemplar.setItems(ejemplarDAO.listarRentablesDisponibles());
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void refrescar() {
        try {
            filas = rentaDAO.listar();
            modelo.setRowCount(0);
            for (Renta r : filas) {
                modelo.addRow(new Object[]{
                        r.getNombreCliente(), r.getNombreProducto(), r.getNumeroSerie(),
                        r.getFechaLimite(), r.getMontoRenta(), r.getDeposito(), r.getEstado()});
            }
            setMensaje(filas.size() + " rentas."
                    + (comboEjemplar.getItemCount() == 0
                    ? "  (Sin ejemplares rentables disponibles)" : ""));
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
            aviso("No hay ejemplares rentables disponibles.\n"
                    + "Verifica que existan ejemplares con estado 'disponible' "
                    + "y que el producto sea 'Rentable'.");
            return;
        }
        LocalDate fecha = parseFecha(campoFechaLimite.getText());
        if (fecha == null) {
            return;
        }
        BigDecimal monto = decimalRequerido(campoMonto.getText(), "monto de renta");
        if (monto == null) {
            return;
        }
        rentaDAO.registrar(cliente.getIdCliente(), ejemplar.getIdEjemplar(), fecha, monto,
                decimalOpcional(campoDeposito.getText(), "deposito"), sesion.getUsuario().getIdUsuario(),
                (String) comboMetodoPago.getSelectedItem());
        frame.setMensaje("Renta registrada para " + cliente.getNombre());
        cargarCombos();
        refrescar();
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
        campoFechaLimite.setText(LocalDate.now().plusDays(7).toString());
        campoMonto.setText("");
        campoDeposito.setText("0");
        comboMetodoPago.setSelectedIndex(0);
        tabla.clearSelection();
    }
}
