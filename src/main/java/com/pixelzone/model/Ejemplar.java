package com.pixelzone.model;

import java.math.BigDecimal;

/**
 * Copia fisica de {@code ejemplares}. Incluye campos de presentacion
 * ({@code nombreProducto}, {@code codigoProducto}) que provienen del JOIN.
 */
public class Ejemplar {

    private String idEjemplar;
    private String idProducto;
    private String codigoProducto;
    private String nombreProducto;
    private String numeroSerie;
    private String condicion;
    private BigDecimal costo;
    private BigDecimal precioVenta;
    private String estado;
    private boolean tieneCaja;
    private boolean tieneManual;
    private String observaciones;
    private String idClienteOrigen;

    public String getIdEjemplar() {
        return idEjemplar;
    }

    public void setIdEjemplar(String idEjemplar) {
        this.idEjemplar = idEjemplar;
    }

    public String getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(String idProducto) {
        this.idProducto = idProducto;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public String getCondicion() {
        return condicion;
    }

    public void setCondicion(String condicion) {
        this.condicion = condicion;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isTieneCaja() {
        return tieneCaja;
    }

    public void setTieneCaja(boolean tieneCaja) {
        this.tieneCaja = tieneCaja;
    }

    public boolean isTieneManual() {
        return tieneManual;
    }

    public void setTieneManual(boolean tieneManual) {
        this.tieneManual = tieneManual;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getIdClienteOrigen() {
        return idClienteOrigen;
    }

    public void setIdClienteOrigen(String idClienteOrigen) {
        this.idClienteOrigen = idClienteOrigen;
    }

    @Override
    public String toString() {
        return (nombreProducto == null ? "" : nombreProducto + " - ") + numeroSerie;
    }
}
