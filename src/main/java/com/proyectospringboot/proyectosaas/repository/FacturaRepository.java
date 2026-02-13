package com.proyectospringboot.proyectosaas.repository;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/* FacturaRepository:
 * Consultas de facturas para el Dashboard.
 * Usamos JPQL corto para evitar nombres kilométricos en los métodos. */

public interface FacturaRepository extends JpaRepository<Factura, Long> {

        // Devuelve las facturas de una suscripción ya ordenadas (la última será la primera de la lista).
        @Query("SELECT f FROM Factura f WHERE f.suscripcion.id = :suscripcionId ORDER BY f.fecha DESC")
        List<Factura> buscarPorSuscripcionOrdenadas(@Param("suscripcionId") Long suscripcionId);

        @Query("SELECT f FROM Factura f WHERE f.suscripcion.usuario.email = :email ORDER BY f.fecha DESC")
        List<Factura> buscarPorEmail(@Param("email") String email);

        @Query("SELECT f FROM Factura f JOIN f.suscripcion s JOIN s.usuario u " +
                "WHERE u.email = :email " +
                "AND f.fecha >= COALESCE(:fechaInicio, f.fecha) " +
                "AND f.fecha <= COALESCE(:fechaFin, f.fecha) " +
                "AND f.total >= COALESCE(:totalMin, f.total) " +
                "AND f.total <= COALESCE(:totalMax, f.total) " +
                "ORDER BY f.fecha DESC")
        List<Factura> buscarConFiltros(
                @Param("email") String email,
                @Param("fechaInicio") LocalDateTime fechaInicio,
                @Param("fechaFin") LocalDateTime fechaFin,
                @Param("totalMin") BigDecimal totalMin,
                @Param("totalMax") BigDecimal totalMax
        );
}
