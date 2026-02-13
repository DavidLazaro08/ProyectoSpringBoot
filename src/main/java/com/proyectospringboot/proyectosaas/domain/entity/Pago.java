package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Entidad abstracta Pago
 *
 * Representa el pago asociado a una factura.
 * Se usa herencia JPA (JOINED) para permitir
 * distintos tipos: tarjeta, PayPal, transferencia, etc. */

@Entity
@Table(name = "pagos")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Pago {

    // =========================================================
    // IDENTIFICADOR
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // RELACIÓN
    // =========================================================

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false, unique = true)
    private Factura factura;

    // =========================================================
    // DATOS
    // =========================================================

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importe;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    protected Pago() {
        // Constructor requerido por JPA
    }

    protected Pago(Factura factura, BigDecimal importe, LocalDateTime fecha) {
        this.factura = factura;
        this.importe = importe;
        this.fecha = fecha;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public Long getId() {
        return id;
    }

    public Factura getFactura() {
        return factura;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
