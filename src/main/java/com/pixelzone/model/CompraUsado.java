package com.pixelzone.model;

import java.math.BigDecimal;

/**
 * Compra de un ejemplar usado a un cliente, para la tabla de seguimiento.
 */
public class CompraUsado {

    private String idCompraUsado;
    private String nombreProducto;
    private String numeroSerie;
    private String nombreCliente;
    private BigDecimal precioCompra;

    public String getIdCompraUsado() {
        return idCompraUsado;
    }

    public void setIdCompraUsado(String idCompraUsado) {
        this.idCompraUsado = idCompraUsado;
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

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }
}
