package com.proyectospringboot.proyectosaas.repository;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
    Optional<Factura> findTopBySuscripcionIdOrderByFechaDesc(Long suscripcionId);

    List<Factura> findBySuscripcionUsuarioEmailOrderByFechaDesc(String email);
}
