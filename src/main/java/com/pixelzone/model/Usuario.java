package com.pixelzone.model;

/**
 * Usuario autenticado (fila de {@code usuarios} + nombre de su perfil).
 */
public class Usuario {

    private final String idUsuario;
    private final String idPerfil;
    private final String nombreUsuario;
    private final String nombreCompleto;
    private final String nombrePerfil;

    public Usuario(String idUsuario,
                   String idPerfil,
                   String nombreUsuario,
                   String nombreCompleto,
                   String nombrePerfil) {
        this.idUsuario = idUsuario;
        this.idPerfil = idPerfil;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.nombrePerfil = nombrePerfil;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public String getIdPerfil() {
        return idPerfil;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getNombrePerfil() {
        return nombrePerfil;
    }
}
