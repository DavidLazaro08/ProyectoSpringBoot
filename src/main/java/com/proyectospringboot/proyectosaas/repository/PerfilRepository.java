package com.proyectospringboot.proyectosaas.repository;

import com.proyectospringboot.proyectosaas.domain.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/* PerfilRepository:
 * Acceso a datos básicos del perfil.
 * Solo necesitamos buscar el perfil asociado a un usuario. */

public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Perfil p WHERE p.usuario.id = :usuarioId")
    Optional<Perfil> buscarPorUsuarioId(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);

}
