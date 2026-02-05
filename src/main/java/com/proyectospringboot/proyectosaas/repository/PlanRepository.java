package com.proyectospringboot.proyectosaas.repository;

import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

/* Repositorio JPA para la entidad Plan.
 * Permite operaciones CRUD sin implementar código SQL. */

public interface PlanRepository extends JpaRepository<Plan, Long> {

}
