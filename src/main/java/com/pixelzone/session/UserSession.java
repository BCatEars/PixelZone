package com.pixelzone.session;

import com.pixelzone.model.Usuario;
import com.pixelzone.security.Permisos;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Sesion activa: usuario autenticado + permisos cargados desde
 * {@code perfil_permisos}/{@code permisos}.
 */
public class UserSession {

    private final Usuario usuario;
    private final Set<String> permisos;

    public UserSession(Usuario usuario, Set<String> permisos) {
        this.usuario = usuario;
        this.permisos = Collections.unmodifiableSet(new TreeSet<>(permisos));
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Set<String> getPermisos() {
        return permisos;
    }

    public boolean tienePermiso(String permiso) {
        return permisos.contains(permiso);
    }

    /**
     * El rol administrador se infiere cuando el perfil posee los tres permisos
     * sembrados (no existe columna {@code es_admin} en el esquema).
     */
    public boolean esAdmin() {
        return permisos.contains(Permisos.GESTION_VENTAS)
                && permisos.contains(Permisos.GESTION_INVENTARIO)
                && permisos.contains(Permisos.GESTION_RENTAS);
    }

    public String resumenPermisos() {
        return permisos.isEmpty() ? "(sin permisos)" : String.join(", ", permisos);
    }
}
