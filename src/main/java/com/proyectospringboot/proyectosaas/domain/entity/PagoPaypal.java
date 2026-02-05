package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Implementación del método de pago mediante tarjeta.
 * Contiene los datos específicos asociados al pago con Paypal. */

@Entity
@Table(name = "pagos_paypal")
public class PagoPaypal extends Pago {

    private String emailPaypal;

    protected PagoPaypal() {
    }

    public PagoPaypal(Factura factura, BigDecimal importe, LocalDateTime fecha, String emailPaypal) {
        super(factura, importe, fecha);
        this.emailPaypal = emailPaypal;
    }

    public String getEmailPaypal() {
        return emailPaypal;
    }
}
