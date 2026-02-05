package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/* Representa un plan de suscripción disponible en la plataforma
 * (Basic, Premium, Enterprise), con su precio y características.
 * Se trata de un catálogo estable. */

@Entity
@Table(name = "planes")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private BigDecimal precioMensual;

    public Plan() {
    }

    public Plan(String nombre, BigDecimal precioMensual) {
        this.nombre = nombre;
        this.precioMensual = precioMensual;
    }

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
