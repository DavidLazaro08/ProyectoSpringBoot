package com.proyectospringboot.proyectosaas.repository;

import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/* UsuarioRepository:
 * Acceso a los usuarios del sistema.
 * Solo añadimos la búsqueda por email porque lo usamos en login / registro. */

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT u FROM Usuario u WHERE u.email = :email")
    Optional<Usuario> buscarPorEmail(@org.springframework.data.repository.query.Param("email") String email);

}
