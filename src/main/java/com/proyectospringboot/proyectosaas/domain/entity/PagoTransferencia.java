package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Implementación del método de pago mediante tarjeta.
 * Contiene los datos específicos asociados al pago por transferencia. */

@Entity
@Table(name = "pagos_transferencia")
public class PagoTransferencia extends Pago {

    private String iban;
    private String referencia;

    protected PagoTransferencia() {
    }

    public PagoTransferencia(Factura factura, BigDecimal importe, LocalDateTime fecha, String iban, String referencia) {
        super(factura, importe, fecha);
        this.iban = iban;
        this.referencia = referencia;
    }

    public String getIban() {
        return iban;
    }

    public String getReferencia() {
        return referencia;
    }
}
