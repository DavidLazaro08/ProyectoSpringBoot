package com.proyectospringboot.proyectosaas.repository;

import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {
    Optional<Suscripcion> findByUsuarioId(Long usuarioId);
    Optional<Suscripcion> findTopByOrderByIdDesc(); // opcional, por si quieres dashboard "último"
}

