package com.pixelzone.ui;

import com.pixelzone.dao.UsuarioDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.model.Empleado;
import com.pixelzone.model.ItemCombo;
import com.pixelzone.session.UserSession;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.util.List;
import java.util.UUID;

/**
 * Gestion de empleados (solo administrador). Permite crear/editar usuarios y
 * asignarles perfil; no toca {@code perfil_permisos} (solo lectura).
 */
public class PanelUsuarios extends PanelCrudBase {

    private static final String[] COLUMNAS = {
            "Usuario", "Nombre completo", "Correo", "Perfil", "Activo"
    };

    private final UsuarioDAO dao = new UsuarioDAO();

    private final JTextField campoUsuario = new JTextField(16);
    private final JTextField campoNombreCompleto = new JTextField(20);
    private final JTextField campoCorreo = new JTextField(18);
    private final JPasswordField campoContrasena = new JPasswordField(16);
    private final JComboBox<ItemCombo> comboPerfil = new JComboBox<>();
    private final JCheckBox checkActivo = new JCheckBox("Activo", true);

    private List<Empleado> filas = List.of();

    public PanelUsuarios(UserSession sesion, MainFrame frame) {
        super(sesion, frame, COLUMNAS);
        construirFormulario();
        cargarPerfiles();
        refrescar();
    }

    private void construirFormulario() {
        campo("Usuario:", campoUsuario);
        campo("Nombre completo:", campoNombreCompleto);
        campo("Correo:", campoCorreo);
        campo("Contrasena:", campoContrasena);
        campo("Perfil:", comboPerfil);
        campoAnchoCompleto(checkActivo);

        agregarBoton("Guardar", this::guardar);
        agregarBoton("Actualizar", this::actualizar);
        agregarBoton("Limpiar", this::limpiar);
        agregarBoton("Eliminar", this::eliminar);
    }

    private void cargarPerfiles() {
        try {
            comboPerfil.removeAllItems();
            for (ItemCombo item : dao.listarPerfiles()) {
                comboPerfil.addItem(item);
            }
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void refrescar() {
        try {
            filas = dao.listar();
            modelo.setRowCount(0);
            for (Empleado e : filas) {
                modelo.addRow(new Object[]{
                        e.getNombreUsuario(), e.getNombreCompleto(), e.getCorreo(),
                        e.getNombrePerfil(), e.isActivo() ? "Si" : "No"});
            }
            setMensaje(filas.isEmpty() ? "Sin usuarios." : filas.size() + " usuarios.");
        } catch (DAOException ex) {
            aviso(ex.getMessage());
        }
    }

    private void guardar() throws DAOException {
        Empleado e = leerFormulario(null);
        if (e == null) {
            return;
        }
        dao.crear(e);
        frame.setMensaje("Usuario creado: " + e.getNombreUsuario());
        limpiar();
        refrescar();
    }

    private void actualizar() throws DAOException {
        Empleado seleccionado = empleadoSeleccionado();
        if (seleccionado == null) {
            aviso("Selecciona un usuario de la tabla primero.");
            return;
        }
        Empleado e = leerFormulario(seleccionado.getIdUsuario());
        if (e == null) {
            return;
        }
        dao.actualizar(e);
        frame.setMensaje("Usuario actualizado: " + e.getNombreUsuario());
        refrescar();
    }

    private void eliminar() throws DAOException {
        Empleado seleccionado = empleadoSeleccionado();
        if (seleccionado == null) {
            aviso("Selecciona un usuario de la tabla primero.");
            return;
        }
        if (seleccionado.getIdUsuario().equals(sesion.getUsuario().getIdUsuario())) {
            aviso("No puedes eliminar tu propia cuenta en sesion.");
            return;
        }
        if (!confirmar("¿Eliminar el usuario " + seleccionado.getNombreUsuario() + "?")) {
            return;
        }
        dao.eliminar(seleccionado.getIdUsuario());
        frame.setMensaje("Usuario eliminado: " + seleccionado.getNombreUsuario());
        limpiar();
        refrescar();
    }

    private Empleado leerFormulario(String idExistente) {
        String usuario = campoUsuario.getText().trim();
        if (usuario.isEmpty()) {
            aviso("Captura el nombre de usuario.");
            return null;
        }
        String nombreCompleto = campoNombreCompleto.getText().trim();
        if (nombreCompleto.isEmpty()) {
            aviso("Captura el nombre completo.");
            return null;
        }
        String contrasena = new String(campoContrasena.getPassword());
        if (contrasena.isEmpty()) {
            aviso("Captura la contrasena.");
            return null;
        }
        ItemCombo perfil = (ItemCombo) comboPerfil.getSelectedItem();
        if (perfil == null) {
            aviso("No hay perfiles cargados.");
            return null;
        }
        Empleado e = new Empleado();
        e.setIdUsuario(idExistente != null ? idExistente : UUID.randomUUID().toString());
        e.setIdPerfil(perfil.getId());
        e.setNombreUsuario(usuario);
        e.setContrasena(contrasena);
        e.setNombreCompleto(nombreCompleto);
        e.setCorreo(textoONull(campoCorreo.getText()));
        e.setActivo(checkActivo.isSelected());
        return e;
    }

    private Empleado empleadoSeleccionado() {
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
        Empleado e = filas.get(filaModelo);
        campoUsuario.setText(e.getNombreUsuario());
        campoNombreCompleto.setText(e.getNombreCompleto());
        campoCorreo.setText(e.getCorreo() == null ? "" : e.getCorreo());
        campoContrasena.setText(e.getContrasena());
        seleccionarItem(comboPerfil, e.getIdPerfil());
        checkActivo.setSelected(e.isActivo());
    }

    private void limpiar() {
        campoUsuario.setText("");
        campoNombreCompleto.setText("");
        campoCorreo.setText("");
        campoContrasena.setText("");
        if (comboPerfil.getItemCount() > 0) {
            comboPerfil.setSelectedIndex(0);
        }
        checkActivo.setSelected(true);
        tabla.clearSelection();
    }
}
