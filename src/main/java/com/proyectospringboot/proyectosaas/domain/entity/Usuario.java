package com.proyectospringboot.proyectosaas.domain.entity;

import com.proyectospringboot.proyectosaas.domain.enums.RolUsuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/* Usuario:
 * Representa a un usuario registrado en la plataforma.
 * Contiene datos básicos y se relaciona con Perfil y Suscripción. */

@Entity
@Table(name = "usuarios")
public class Usuario {

    // =========================================================
    // CAMPOS
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String pais;

    @Column(nullable = false)
    private String password;

    @Column(name = "metodo_pago_preferido", length = 50)
    private String metodoPagoPreferido; // "Tarjeta", "PayPal", "Transferencia"

    @Column(name = "pago_automatico", nullable = false, columnDefinition = "boolean default false")
    private boolean pagoAutomatico = false; // domiciliación activada por el usuario

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    @OneToOne(mappedBy = "usuario")
    private Perfil perfil;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    protected Usuario() {
        // Constructor requerido por JPA
    }

    public Usuario(String email, String pais, String password, RolUsuario rol) {
        this.email = email;
        this.pais = pais;
        this.password = password;
        this.rol = rol;
        this.fechaAlta = LocalDateTime.now();
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPais() {
        return pais;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public String getPassword() {
        return password;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public String getMetodoPagoPreferido() {
        return metodoPagoPreferido;
    }

    public boolean isPagoAutomatico() {
        return pagoAutomatico;
    }

    // =========================================================
    // SETTERS
    // =========================================================

    public void setMetodoPagoPreferido(String metodoPagoPreferido) {
        this.metodoPagoPreferido = metodoPagoPreferido;
    }

    public void setPagoAutomatico(boolean pagoAutomatico) {
        this.pagoAutomatico = pagoAutomatico;
    }
}
