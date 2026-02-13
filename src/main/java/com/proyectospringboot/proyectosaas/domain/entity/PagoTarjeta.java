package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Pago realizado mediante tarjeta.
 *
 * Se almacena únicamente información básica:
 * - Últimos 4 dígitos
 * - Nombre del titular
 *
 * (No guardamos datos sensibles reales, solo simulación para el modelo.) */

@Entity
@Table(name = "pagos_tarjeta")
public class PagoTarjeta extends Pago {

    // =========================================================
    // DATOS ESPECÍFICOS
    // =========================================================

    @Column(nullable = false)
    private String ultimos4;

    @Column(nullable = false)
    private String titular;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    protected PagoTarjeta() {
        // Constructor requerido por JPA
    }

    public PagoTarjeta(Factura factura,
                       BigDecimal importe,
                       LocalDateTime fecha,
                       String ultimos4,
                       String titular) {

        super(factura, importe, fecha);
        this.ultimos4 = ultimos4;
        this.titular = titular;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public String getUltimos4() {
        return ultimos4;
    }

    public String getTitular() {
        return titular;
    }
}
