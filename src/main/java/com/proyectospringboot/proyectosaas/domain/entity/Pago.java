package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Clase base abstracta para los distintos métodos de pago.
 * Se utiliza herencia JPA para permitir diferentes
 * implementaciones según el tipo de pago. */

@Entity
@Table(name = "pagos")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "factura_id", nullable = false, unique = true)
    private Factura factura;

    @Column(nullable = false)
    private BigDecimal importe;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Pago() {
    }

    protected Pago(Factura factura, BigDecimal importe, LocalDateTime fecha) {
        this.factura = factura;
        this.importe = importe;
        this.fecha = fecha;
    }

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
