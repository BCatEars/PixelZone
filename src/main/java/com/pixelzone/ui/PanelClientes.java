package com.pixelzone.ui;

import com.pixelzone.dao.ClienteDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Cliente;
import com.pixelzone.session.UserSession;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.util.List;
import java.util.UUID;

/**
 * CRUD de clientes. El combo de tipo usa los literales exactos del ENUM del DDL.
 */
public class PanelClientes extends PanelCrudBase {

    private static final String[] COLUMNAS = {"Nombre", "Telefono", "Correo", "Tipo", "Activo"};

    private final ClienteDAO dao = new ClienteDAO();

    private final JTextField campoNombre = new JTextField(18);
    private final JTextField campoTelefono = new JTextField(14);
    private final JTextField campoCorreo = new JTextField(18);
    private final JComboBox<String> comboTipo =
            new JComboBox<>(new String[]{"registrado", "mostrador", "anonimo"});
    private final JCheckBox checkActivo = new JCheckBox("Activo", true);

    private List<Cliente> filas = List.of();

    public PanelClientes(UserSession sesion, MainFrame frame) {
        super(sesion, frame, COLUMNAS);
        construirFormulario();
        refrescar();
    }

    private void construirFormulario() {
        campo("Nombre:", campoNombre);
        campo("Telefono:", campoTelefono);
        campo("Correo:", campoCorreo);
        campo("Tipo:", comboTipo);
        campoAnchoCompleto(checkActivo);

        agregarBoton("Guardar", this::guardar);
        agregarBoton("Actualizar", this::actualizar);
        agregarBoton("Limpiar", this::limpiar);
        agregarBoton("Eliminar", this::eliminar);
    }

    private void refrescar() {
        try {
            filas = dao.listar();
            modelo.setRowCount(0);
            for (Cliente c : filas) {
                modelo.addRow(new Object[]{
                        c.getNombre(), c.getTelefono(), c.getCorreo(),
                        c.getTipoCliente(), c.isActivo() ? "Si" : "No"});
            }
            setMensaje(filas.isEmpty() ? "Sin clientes." : filas.size() + " clientes.");
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void guardar() throws DAOException {
        Cliente c = leerFormulario(null);
        if (c == null) {
            return;
        }
        dao.crear(c);
        frame.setMensaje("Cliente creado: " + c.getNombre());
        limpiar();
        refrescar();
    }

    private void actualizar() throws DAOException {
        Cliente seleccionado = clienteSeleccionado();
        if (seleccionado == null) {
            aviso("Selecciona un cliente de la tabla primero.");
            return;
        }
        Cliente c = leerFormulario(seleccionado.getIdCliente());
        if (c == null) {
            return;
        }
        dao.actualizar(c);
        frame.setMensaje("Cliente actualizado: " + c.getNombre());
        refrescar();
    }

    private void eliminar() throws DAOException {
        Cliente seleccionado = clienteSeleccionado();
        if (seleccionado == null) {
            aviso("Selecciona un cliente de la tabla primero.");
            return;
        }
        if (!confirmar("¿Eliminar el cliente " + seleccionado.getNombre() + "?")) {
            return;
        }
        dao.eliminar(seleccionado.getIdCliente());
        frame.setMensaje("Cliente eliminado: " + seleccionado.getNombre());
        limpiar();
        refrescar();
    }

    private Cliente leerFormulario(String idExistente) {
        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            aviso("Captura el nombre.");
            return null;
        }
        Cliente c = new Cliente();
        c.setIdCliente(idExistente != null ? idExistente : UUID.randomUUID().toString());
        c.setNombre(nombre);
        c.setTelefono(textoONull(campoTelefono.getText()));
        c.setCorreo(textoONull(campoCorreo.getText()));
        c.setTipoCliente((String) comboTipo.getSelectedItem());
        c.setActivo(checkActivo.isSelected());
        return c;
    }

    private Cliente clienteSeleccionado() {
        int vista = tabla.getSelectedRow();
        if (vista < 0) {
            return null;
        }
        int fila = tabla.convertRowIndexToModel(vista);
        return (fila >= 0 && fila < filas.size()) ? filas.get(fila) : null;
    }

    @Override
    public void recargar() {
        refrescar();
    }

    @Override
    protected void alSeleccionar(int filaModelo) {
        if (filaModelo < 0 || filaModelo >= filas.size()) {
            return;
        }
        Cliente c = filas.get(filaModelo);
        campoNombre.setText(c.getNombre());
        campoTelefono.setText(c.getTelefono() == null ? "" : c.getTelefono());
        campoCorreo.setText(c.getCorreo() == null ? "" : c.getCorreo());
        comboTipo.setSelectedItem(c.getTipoCliente());
        checkActivo.setSelected(c.isActivo());
    }

    private void limpiar() {
        campoNombre.setText("");
        campoTelefono.setText("");
        campoCorreo.setText("");
        comboTipo.setSelectedIndex(0);
        checkActivo.setSelected(true);
        tabla.clearSelection();
    }
}
