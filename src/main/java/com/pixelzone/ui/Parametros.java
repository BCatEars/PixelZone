package com.pixelzone.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Entrada de un parametro de texto mediante dialogo modal.
 */
public final class Parametros {

    private Parametros() {
    }

    /**
     * @return el texto ingresado (puede ser vacio, util para los centinelas de
     *         los procedimientos) o {@code null} si el usuario cancela.
     */
    public static String pedir(Component padre, String mensaje, String defecto) {
        Object valor = JOptionPane.showInputDialog(padre, mensaje, "Parametro",
                JOptionPane.QUESTION_MESSAGE, null, null, defecto);
        return valor == null ? null : valor.toString();
    }
}
