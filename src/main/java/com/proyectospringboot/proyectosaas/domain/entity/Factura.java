package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Representa una factura generada a partir de una suscripción.
 * Almacena un snapshot de los datos económicos en el momento
 * de su creación. */

@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    @Column(nullable = false)
    private BigDecimal importe;

    @Column(nullable = false)
    private BigDecimal impuesto;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Factura() {
    }

    public Factura(Suscripcion suscripcion, BigDecimal importe, BigDecimal impuesto, BigDecimal total,
            LocalDateTime fecha) {
        this.suscripcion = suscripcion;
        this.importe = importe;
        this.impuesto = impuesto;
        this.total = total;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public Suscripcion getSuscripcion() {
        return suscripcion;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public BigDecimal getImpuesto() {
        return impuesto;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
