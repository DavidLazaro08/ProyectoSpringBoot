package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*Implementación del método de pago mediante tarjeta.
 * Contiene los datos específicos asociados al pago con tarjeta. */

@Entity
@Table(name = "pagos_tarjeta")
public class PagoTarjeta extends Pago {

    private String ultimos4;
    private String titular;

    protected PagoTarjeta() {
    }

    public PagoTarjeta(Factura factura, BigDecimal importe, LocalDateTime fecha, String ultimos4, String titular) {
        super(factura, importe, fecha);
        this.ultimos4 = ultimos4;
        this.titular = titular;
    }

    public String getUltimos4() {
        return ultimos4;
    }

    public String getTitular() {
        return titular;
    }
}
