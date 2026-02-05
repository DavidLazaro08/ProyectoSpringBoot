package com.proyectospringboot.proyectosaas.repository;

import com.proyectospringboot.proyectosaas.domain.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Optional<Perfil> findByUsuarioId(Long usuarioId);
}
