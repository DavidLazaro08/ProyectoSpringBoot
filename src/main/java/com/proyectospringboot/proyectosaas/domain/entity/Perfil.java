package com.proyectospringboot.proyectosaas.domain.entity;

import jakarta.persistence.*;

/* Entidad que almacena información adicional del usuario.
 * Se mantiene separada para evitar sobrecargar la entidad Usuario
 * y facilitar la extensibilidad del modelo. */

@Entity
@Table(name = "perfiles")
public class Perfil {

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

    protected Perfil() {
    }

    public Perfil(Usuario usuario, String nombre, String apellidos, String telefono) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.telefono = telefono;
    }

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
