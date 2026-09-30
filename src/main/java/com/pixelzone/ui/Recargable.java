package com.pixelzone.ui;

/**
 * Paneles que deben recargar sus datos (y sus combos) cada vez que el usuario
 * los selecciona en la navegacion, para no mostrar informacion desactualizada.
 */
public interface Recargable {

    void recargar();
}
