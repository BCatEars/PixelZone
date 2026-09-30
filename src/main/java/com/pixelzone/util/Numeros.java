package com.pixelzone.util;

import java.math.BigDecimal;

/**
 * Utilidades de parseo/formato numerico. Acepta coma o punto como separador
 * decimal (entorno es-MX) y nunca usa {@code double} para dinero.
 */
public final class Numeros {

    private Numeros() {
    }

    /**
     * @return el valor parseado, o {@code null} si el texto esta vacio.
     * @throws NumberFormatException si el texto no es un decimal valido.
     */
    public static BigDecimal parseDecimal(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replace(',', '.');
        if (limpio.isEmpty()) {
            return null;
        }
        return new BigDecimal(limpio);
    }

    public static String formatear(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString();
    }
}
