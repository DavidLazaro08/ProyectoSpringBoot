package com.proyectospringboot.proyectosaas.repository;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
    Optional<Factura> findTopBySuscripcionIdOrderByFechaDesc(Long suscripcionId);

    List<Factura> findBySuscripcionUsuarioEmailOrderByFechaDesc(String email);

    @Query("SELECT f FROM Factura f JOIN f.suscripcion s JOIN s.usuario u " +
            "WHERE u.email = :email " +
            "AND (:fechaInicio IS NULL OR f.fecha >= :fechaInicio) " +
            "AND (:fechaFin IS NULL OR f.fecha <= :fechaFin) " +
            "AND (:totalMin IS NULL OR f.total >= :totalMin) " +
            "AND (:totalMax IS NULL OR f.total <= :totalMax) " +
            "ORDER BY f.fecha DESC")
    List<Factura> buscarConFiltros(
            @Param("email") String email,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("totalMin") BigDecimal totalMin,
            @Param("totalMax") BigDecimal totalMax);
}
