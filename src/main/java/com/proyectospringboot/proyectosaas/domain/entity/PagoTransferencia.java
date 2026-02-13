package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Pago realizado mediante transferencia bancaria.
 *
 * Guardamos:
 * - IBAN (simulado)
 * - Referencia del pago
 *
 * En un sistema real estos datos deberían tratarse con
 * medidas adicionales de seguridad. */

@Entity
@Table(name = "pagos_transferencia")
public class PagoTransferencia extends Pago {

    // =========================================================
    // DATOS ESPECÍFICOS
    // =========================================================

    @Column(nullable = false)
    private String iban;

    @Column(nullable = false)
    private String referencia;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    protected PagoTransferencia() {
        // Constructor requerido por JPA
    }

    public PagoTransferencia(Factura factura,
                             BigDecimal importe,
                             LocalDateTime fecha,
                             String iban,
                             String referencia) {

        super(factura, importe, fecha);
        this.iban = iban;
        this.referencia = referencia;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public String getIban() {
        return iban;
    }

    public String getReferencia() {
        return referencia;
    }
}
