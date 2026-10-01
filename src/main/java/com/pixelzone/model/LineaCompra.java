package com.pixelzone.model;

import java.math.BigDecimal;

/**
 * Linea capturada en el panel de compras a proveedor. Es una estructura de
 * transporte hacia {@code CompraDAO}; no se persiste por si misma (lo que se
 * guarda es {@code detalle_compras} + los ejemplares generados).
 */
public class LineaCompra {

    private String idProducto;
    private String nombreProducto;
    private int cantidad;
    private BigDecimal costoUnitario;
    private BigDecimal precioVenta;

    public String getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(String idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public BigDecimal getSubtotal() {
        if (costoUnitario == null) {
            return BigDecimal.ZERO;
        }
        return costoUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    @Override
    public String toString() {
        return nombreProducto + " x" + cantidad;
    }
}
