package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Pago mediante PayPal.
 *
 * Extiende de Pago y añade el email asociado
 * a la cuenta con la que se realizó la operación. */

@Entity
@Table(name = "pagos_paypal")
public class PagoPaypal extends Pago {

    // =========================================================
    // DATOS ESPECÍFICOS
    // =========================================================

    @Column(nullable = false)
    private String emailPaypal;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    protected PagoPaypal() {
        // Constructor requerido por JPA
    }

    public PagoPaypal(Factura factura,
                      BigDecimal importe,
                      LocalDateTime fecha,
                      String emailPaypal) {

        super(factura, importe, fecha);
        this.emailPaypal = emailPaypal;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public String getEmailPaypal() {
        return emailPaypal;
    }
}
