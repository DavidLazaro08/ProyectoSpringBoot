package com.proyectospringboot.proyectosaas.domain.entity;

import com.proyectospringboot.proyectosaas.domain.enums.EstadoSuscripcion;
import jakarta.persistence.*;

import java.time.LocalDateTime;

import org.hibernate.envers.Audited;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

/* Suscripcion:
 * Relaciona un Usuario con un Plan.
 * Guarda estado, fechas y permite auditar cambios con Envers. */

@Audited
@Entity
@Table(name = "suscripciones")
public class Suscripcion {

    // =========================================================
    // CAMPOS
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    @Audited(targetAuditMode = NOT_AUDITED)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    @Audited(targetAuditMode = NOT_AUDITED)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSuscripcion estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin_ciclo", nullable = false)
    private LocalDateTime fechaFinCiclo;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    protected Suscripcion() {
        // Constructor requerido por JPA
    }

    public Suscripcion(Usuario usuario, Plan plan) {
        this.usuario = usuario;
        this.plan = plan;
        this.estado = EstadoSuscripcion.ACTIVA;
        this.fechaInicio = LocalDateTime.now();
        this.fechaFinCiclo = this.fechaInicio.plusDays(30); // Simplificación mensual
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Plan getPlan() {
        return plan;
    }

    public EstadoSuscripcion getEstado() {
        return estado;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFinCiclo() {
        return fechaFinCiclo;
    }

    public LocalDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    // =========================================================
    // MODIFICADORES DE ESTADO
    // =========================================================

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public void setEstado(EstadoSuscripcion estado) {
        this.estado = estado;
    }

    public void setFechaFinCiclo(LocalDateTime fechaFinCiclo) {
        this.fechaFinCiclo = fechaFinCiclo;
    }

    public void cancelar() {
        this.estado = EstadoSuscripcion.CANCELADA;
        this.fechaCancelacion = LocalDateTime.now();
    }
}
