package com.pixelzone.model;

/**
 * Par (id, nombre) para poblar combos de catalogos (categorias, plataformas,
 * perfiles). El id puede ser {@code null} para representar "ninguno".
 */
public class ItemCombo {

    private final String id;
    private final String nombre;

    public ItemCombo(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
