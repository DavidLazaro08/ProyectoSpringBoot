package com.proyectospringboot.proyectosaas.repository;

import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.enums.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {
    Optional<Suscripcion> findByUsuarioId(Long usuarioId);

    Optional<Suscripcion> findByUsuarioEmail(String email);

    List<Suscripcion> findByEstadoAndFechaFinCicloBefore(EstadoSuscripcion estado, LocalDateTime fecha);
}
