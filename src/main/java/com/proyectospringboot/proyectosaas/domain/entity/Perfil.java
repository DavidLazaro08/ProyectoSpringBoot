package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.*;

/* Perfil:
 * Información personal adicional del usuario.
 * Lo separamos de Usuario para mantener el modelo más limpio
 * y permitir ampliaciones futuras sin sobrecargar la entidad principal. */

@Entity
@Table(name = "perfiles")
public class Perfil {

    // =========================================================
    // CAMPOS
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellidos;

    @Column
    private String telefono;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    protected Perfil() {
        // Constructor requerido por JPA
    }

    public Perfil(Usuario usuario, String nombre, String apellidos, String telefono) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.telefono = telefono;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
