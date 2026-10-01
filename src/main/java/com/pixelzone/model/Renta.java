package com.pixelzone.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Renta para la tabla de seguimiento (incluye nombres por JOIN y el UUID del
 * ejemplar para poder devolverlo).
 */
public class Renta {

    private String idRenta;
    private String idEjemplar;
    private String idCliente;
    private String nombreCliente;
    private String nombreProducto;
    private String numeroSerie;
    private LocalDate fechaLimite;
    private BigDecimal montoRenta;
    private BigDecimal deposito;
    private String estado;

    public String getIdRenta() {
        return idRenta;
    }

    public void setIdRenta(String idRenta) {
        this.idRenta = idRenta;
    }

    public String getIdEjemplar() {
        return idEjemplar;
    }

    public void setIdEjemplar(String idEjemplar) {
        this.idEjemplar = idEjemplar;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
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

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public BigDecimal getMontoRenta() {
        return montoRenta;
    }

    public void setMontoRenta(BigDecimal montoRenta) {
        this.montoRenta = montoRenta;
    }

    public BigDecimal getDeposito() {
        return deposito;
    }

    public void setDeposito(BigDecimal deposito) {
        this.deposito = deposito;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
