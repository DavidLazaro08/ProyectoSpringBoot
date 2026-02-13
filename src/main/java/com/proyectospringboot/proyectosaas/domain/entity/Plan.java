package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/*
 * Plan:
 * Catálogo de planes disponibles en la plataforma
 * (Basic, Premium, Enterprise).
 *
 * Es una entidad estable que define precio y nombre.
 */

@Entity
@Table(name = "planes")
public class Plan {

    // =========================================================
    // CAMPOS
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private BigDecimal precioMensual;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    protected Plan() {
        // Constructor requerido por JPA
    }

    public Plan(String nombre, BigDecimal precioMensual) {
        this.nombre = nombre;
        this.precioMensual = precioMensual;
    }

    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecioMensual() {
        return precioMensual;
    }

    public void setPrecioMensual(BigDecimal precioMensual) {
        this.precioMensual = precioMensual;
    }
}
