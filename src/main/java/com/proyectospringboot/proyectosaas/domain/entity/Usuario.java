package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/* Entidad que representa a un usuario del sistema.
 * Contiene los datos básicos de identificación y se asocia
 * a un perfil y a una suscripción activa. */

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String pais;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    @OneToOne(mappedBy = "usuario")
    private Perfil perfil;

    protected Usuario() {
    }

    public Usuario(String email, String pais) {
        this.email = email;
        this.pais = pais;
        this.fechaAlta = LocalDateTime.now();
    }

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
}
