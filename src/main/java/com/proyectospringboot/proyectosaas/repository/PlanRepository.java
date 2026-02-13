package com.proyectospringboot.proyectosaas.repository;

import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

/* PlanRepository:
 * Acceso a los planes disponibles (Basic, Premium, etc.).
 * De momento solo usamos las operaciones básicas de JPA. */

public interface PlanRepository extends JpaRepository<Plan, Long> {

}
