package com.pixelzone.model;

import java.math.BigDecimal;

/**
 * Datos de una devolucion de renta dentro de un lote: la renta/ejemplar a
 * devolver, la condicion en que regresa y el monto extra a cobrar (recargo por
 * atraso + daño, etc.). Cada fila del lote puede tener valores distintos.
 */
public class DevolucionRenta {

    private String idRenta;
    private String idEjemplar;
    private String condicionRetorno;
    private BigDecimal montoExtra;

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

    public String getCondicionRetorno() {
        return condicionRetorno;
    }

    public void setCondicionRetorno(String condicionRetorno) {
        this.condicionRetorno = condicionRetorno;
    }

    public BigDecimal getMontoExtra() {
        return montoExtra;
    }

    public void setMontoExtra(BigDecimal montoExtra) {
        this.montoExtra = montoExtra;
    }
}
