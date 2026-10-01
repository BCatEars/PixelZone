package com.pixelzone.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila de {@code productos}. Incluye campos de presentacion obtenidos por JOIN:
 * {@code nombreCategoria}, {@code nombrePlataforma} y {@code tipo} (que vive en
 * {@code categorias.tipo}). El {@code tipo} es de solo lectura: se puebla al
 * consultar y no se captura ni se persiste desde aqui.
 */
public class Producto {

    private String idProducto;
    private String idCategoria;
    private String idPlataforma;
    private String idGenero;
    private String codigoInterno;
    private String codigoBarras;
    private String nombre;
    private String descripcion;
    private String tipo;
    private String genero;
    private String clasificacion;
    private String edicion;
    private LocalDate fechaLanzamiento;
    private BigDecimal precioNuevo;
    private BigDecimal precioUsado;
    private boolean rentable;
    private boolean activo;
    private String nombreCategoria;
    private String nombrePlataforma;

    public String getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(String idProducto) {
        this.idProducto = idProducto;
    }

    public String getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(String idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getIdPlataforma() {
        return idPlataforma;
    }

    public void setIdPlataforma(String idPlataforma) {
        this.idPlataforma = idPlataforma;
    }

    public String getIdGenero() {
        return idGenero;
    }

    public void setIdGenero(String idGenero) {
        this.idGenero = idGenero;
    }

    public String getCodigoInterno() {
        return codigoInterno;
    }

    public void setCodigoInterno(String codigoInterno) {
        this.codigoInterno = codigoInterno;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getClasificacion() {
        return clasificacion;
    }

    public void setClasificacion(String clasificacion) {
        this.clasificacion = clasificacion;
    }

    public String getEdicion() {
        return edicion;
    }

    public void setEdicion(String edicion) {
        this.edicion = edicion;
    }

    public LocalDate getFechaLanzamiento() {
        return fechaLanzamiento;
    }

    public void setFechaLanzamiento(LocalDate fechaLanzamiento) {
        this.fechaLanzamiento = fechaLanzamiento;
    }

    public BigDecimal getPrecioNuevo() {
        return precioNuevo;
    }

    public void setPrecioNuevo(BigDecimal precioNuevo) {
        this.precioNuevo = precioNuevo;
    }

    public BigDecimal getPrecioUsado() {
        return precioUsado;
    }

    public void setPrecioUsado(BigDecimal precioUsado) {
        this.precioUsado = precioUsado;
    }

    public boolean isRentable() {
        return rentable;
    }

    public void setRentable(boolean rentable) {
        this.rentable = rentable;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public String getNombrePlataforma() {
        return nombrePlataforma;
    }

    public void setNombrePlataforma(String nombrePlataforma) {
        this.nombrePlataforma = nombrePlataforma;
    }

    @Override
    public String toString() {
        return codigoInterno + " - " + nombre;
    }
}
