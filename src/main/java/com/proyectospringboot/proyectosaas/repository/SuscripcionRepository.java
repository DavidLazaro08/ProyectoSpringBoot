package com.proyectospringboot.proyectosaas.repository;

import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.enums.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/* SuscripcionRepository:
 * Semana 2 - Consultas básicas de Suscripción.
 * Usamos consultas claras y cortas para evitar nombres demasiado largos. */

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    // =========================
    // BÚSQUEDAS BÁSICAS
    // =========================

    @Query("SELECT s FROM Suscripcion s WHERE s.usuario.id = :usuarioId")
    Optional<Suscripcion> buscarPorUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT s FROM Suscripcion s WHERE s.usuario.email = :email")
    Optional<Suscripcion> buscarPorEmail(@Param("email") String email);

    // =========================
    // RENOVACIONES
    // =========================

    @Query("SELECT s FROM Suscripcion s WHERE s.estado = :estado AND s.fechaFinCiclo < :ahora")
    List<Suscripcion> buscarVencidas(@Param("estado") EstadoSuscripcion estado,
                                     @Param("ahora") LocalDateTime ahora);
}
