package com.pixelzone;

import com.pixelzone.session.UserSession;
import com.pixelzone.ui.DialogoLogin;
import com.pixelzone.ui.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada de PixelZone: login modal y, si es correcto, ventana
 * principal. El cierre de sesion vuelve a lanzar el login.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::mostrarLogin);
    }

    private static void mostrarLogin() {
        UserSession sesion = new DialogoLogin(null).mostrar();
        if (sesion == null) {
            System.exit(0);
            return;
        }
        new MainFrame(sesion, Main::mostrarLogin).setVisible(true);
    }
}
