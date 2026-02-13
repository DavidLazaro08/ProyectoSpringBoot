package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Entidad Factura
 *
 * Representa una factura generada a partir de una suscripción.
 * Guarda un snapshot económico en el momento de creación:
 * importe base, impuesto aplicado y total final. */

@Entity
@Table(name = "facturas")
public class Factura {

    // =========================================================
    // IDENTIFICADOR
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // RELACIONES
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    // =========================================================
    // DATOS ECONÓMICOS
    // =========================================================

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importe;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal impuesto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    // =========================================================
    // METADATOS
    // =========================================================

    @Column(nullable = false)
    private LocalDateTime fecha;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    protected Factura() {
        // Constructor requerido por JPA
    }

    public Factura(Suscripcion suscripcion,
                   BigDecimal importe,
                   BigDecimal impuesto,
                   BigDecimal total,
                   LocalDateTime fecha) {

        this.suscripcion = suscripcion;
        this.importe = importe;
        this.impuesto = impuesto;
        this.total = total;
        this.fecha = fecha;
    }

    // =========================================================
    // GETTERS
    // =========================================================

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
