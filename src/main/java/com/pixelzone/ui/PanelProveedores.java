package com.pixelzone.ui;

import com.pixelzone.dao.ProveedorDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Proveedor;
import com.pixelzone.session.UserSession;

import javax.swing.JCheckBox;
import javax.swing.JTextField;
import java.util.List;
import java.util.UUID;

/**
 * CRUD de proveedores.
 */
public class PanelProveedores extends PanelCrudBase {

    private static final String[] COLUMNAS = {
            "Nombre", "Contacto", "Telefono", "Correo", "Direccion", "Activo"
    };

    private final ProveedorDAO dao = new ProveedorDAO();

    private final JTextField campoNombre = new JTextField(20);
    private final JTextField campoContacto = new JTextField(18);
    private final JTextField campoTelefono = new JTextField(14);
    private final JTextField campoCorreo = new JTextField(18);
    private final JTextField campoDireccion = new JTextField(22);
    private final JCheckBox checkActivo = new JCheckBox("Activo", true);

    private List<Proveedor> filas = List.of();

    public PanelProveedores(UserSession sesion, MainFrame frame) {
        super(sesion, frame, COLUMNAS);
        construirFormulario();
        refrescar();
    }

    private void construirFormulario() {
        campo("Nombre:", campoNombre);
        campo("Contacto:", campoContacto);
        campo("Telefono:", campoTelefono);
        campo("Correo:", campoCorreo);
        campo("Direccion:", campoDireccion);
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
            for (Proveedor p : filas) {
                modelo.addRow(new Object[]{
                        p.getNombre(), p.getContacto(), p.getTelefono(),
                        p.getCorreo(), p.getDireccion(), p.isActivo() ? "Si" : "No"});
            }
            setMensaje(filas.isEmpty() ? "Sin proveedores." : filas.size() + " proveedores.");
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void guardar() throws DAOException {
        Proveedor p = leerFormulario(null);
        if (p == null) {
            return;
        }
        dao.crear(p);
        frame.setMensaje("Proveedor creado: " + p.getNombre());
        limpiar();
        refrescar();
    }

    private void actualizar() throws DAOException {
        Proveedor seleccionado = proveedorSeleccionado();
        if (seleccionado == null) {
            aviso("Selecciona un proveedor de la tabla primero.");
            return;
        }
        Proveedor p = leerFormulario(seleccionado.getIdProveedor());
        if (p == null) {
            return;
        }
        dao.actualizar(p);
        frame.setMensaje("Proveedor actualizado: " + p.getNombre());
        refrescar();
    }

    private void eliminar() throws DAOException {
        Proveedor seleccionado = proveedorSeleccionado();
        if (seleccionado == null) {
            aviso("Selecciona un proveedor de la tabla primero.");
            return;
        }
        if (!confirmar("¿Eliminar el proveedor " + seleccionado.getNombre() + "?")) {
            return;
        }
        dao.eliminar(seleccionado.getIdProveedor());
        frame.setMensaje("Proveedor eliminado: " + seleccionado.getNombre());
        limpiar();
        refrescar();
    }

    private Proveedor leerFormulario(String idExistente) {
        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            aviso("Captura el nombre.");
            return null;
        }
        Proveedor p = new Proveedor();
        p.setIdProveedor(idExistente != null ? idExistente : UUID.randomUUID().toString());
        p.setNombre(nombre);
        p.setContacto(textoONull(campoContacto.getText()));
        p.setTelefono(textoONull(campoTelefono.getText()));
        p.setCorreo(textoONull(campoCorreo.getText()));
        p.setDireccion(textoONull(campoDireccion.getText()));
        p.setActivo(checkActivo.isSelected());
        return p;
    }

    private Proveedor proveedorSeleccionado() {
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
        Proveedor p = filas.get(filaModelo);
        campoNombre.setText(p.getNombre());
        campoContacto.setText(p.getContacto() == null ? "" : p.getContacto());
        campoTelefono.setText(p.getTelefono() == null ? "" : p.getTelefono());
        campoCorreo.setText(p.getCorreo() == null ? "" : p.getCorreo());
        campoDireccion.setText(p.getDireccion() == null ? "" : p.getDireccion());
        checkActivo.setSelected(p.isActivo());
    }

    private void limpiar() {
        campoNombre.setText("");
        campoContacto.setText("");
        campoTelefono.setText("");
        campoCorreo.setText("");
        campoDireccion.setText("");
        checkActivo.setSelected(true);
        tabla.clearSelection();
    }
}
