package com.pixelzone.ui;

import com.pixelzone.dao.UsuarioDAO;
import com.pixelzone.exception.DAOException;
import com.pixelzone.session.UserSession;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Dialogo modal de autenticacion. {@link #mostrar()} bloquea hasta que el
 * usuario entra o cancela, y devuelve la sesion creada (o {@code null}).
 */
public class DialogoLogin extends JDialog {

    private final JTextField campoUsuario = new JTextField(18);
    private final JPasswordField campoContrasena = new JPasswordField(18);
    private UserSession sesion;

    public DialogoLogin(Frame owner) {
        super(owner, "PIXEL ZONE - Iniciar sesion", true);
        construir();
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void construir() {
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formulario.add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 1;
        formulario.add(campoUsuario, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formulario.add(new JLabel("Contrasena:"), gbc);
        gbc.gridx = 1;
        formulario.add(campoContrasena, gbc);

        JButton botonEntrar = new JButton("Iniciar sesion");
        JButton botonCancelar = new JButton("Cancelar");
        botonEntrar.addActionListener(e -> autenticar());
        botonCancelar.addActionListener(e -> {
            sesion = null;
            dispose();
        });
        getRootPane().setDefaultButton(botonEntrar);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.add(botonCancelar);
        botones.add(botonEntrar);

        setLayout(new BorderLayout());
        add(formulario, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
    }

    private void autenticar() {
        String usuario = campoUsuario.getText().trim();
        String contrasena = new String(campoContrasena.getPassword());

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Captura usuario y contrasena.",
                    "Campos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            UserSession resultado = new UsuarioDAO().autenticar(usuario, contrasena);
            if (resultado == null) {
                JOptionPane.showMessageDialog(this,
                        "Usuario o contrasena incorrectos (o usuario inactivo).",
                        "Acceso denegado", JOptionPane.ERROR_MESSAGE);
                campoContrasena.setText("");
                campoContrasena.requestFocusInWindow();
                return;
            }
            this.sesion = resultado;
            dispose();
        } catch (DAOException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar con la base de datos:\n" + ex.getMessage(),
                    "Error de conexion", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Muestra el dialogo de forma modal.
     *
     * @return la {@link UserSession} autenticada, o {@code null} si se cancela.
     */
    public UserSession mostrar() {
        setVisible(true);
        return sesion;
    }
}
