package com.pixelzone.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Apartado para la tabla de seguimiento.
 */
public class Apartado {

    private String idApartado;
    private String idEjemplar;
    private String folio;
    private String nombreCliente;
    private String nombreProducto;
    private String numeroSerie;
    private LocalDate fechaLimite;
    private BigDecimal importeAnticipo;
    private String estado;

    public String getIdApartado() {
        return idApartado;
    }

    public void setIdApartado(String idApartado) {
        this.idApartado = idApartado;
    }

    public String getIdEjemplar() {
        return idEjemplar;
    }

    public void setIdEjemplar(String idEjemplar) {
        this.idEjemplar = idEjemplar;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
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

    public BigDecimal getImporteAnticipo() {
        return importeAnticipo;
    }

    public void setImporteAnticipo(BigDecimal importeAnticipo) {
        this.importeAnticipo = importeAnticipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
