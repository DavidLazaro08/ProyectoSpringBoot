package com.proyectospringboot.proyectosaas.config;

import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import com.proyectospringboot.proyectosaas.repository.PlanRepository;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import com.proyectospringboot.proyectosaas.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

/* DataInitializer:
 * Carga datos mínimos para poder arrancar el proyecto sin meter nada a mano.
 * Se usa en desarrollo / pruebas, no es lógica de negocio. */

@Configuration
public class DataInitializer {

        private static final String EMAIL_PRUEBA = "demo@saas.local";

        @Bean
        CommandLineRunner initData(
                PlanRepository planRepository,
                UsuarioRepository usuarioRepository,
                SuscripcionRepository suscripcionRepository,
                TransactionTemplate tx) {

                return args -> tx.execute(status -> {

                        // =========================================================
                        // PLANES (catálogo)
                        // =========================================================

                        if (planRepository.count() == 0) {
                                planRepository.save(new Plan("BASIC", BigDecimal.valueOf(9.99)));
                                planRepository.save(new Plan("PREMIUM", BigDecimal.valueOf(19.99)));
                                planRepository.save(new Plan("ENTERPRISE", BigDecimal.valueOf(49.99)));
                        }

                        Plan basic = planRepository.findAll().stream()
                                .filter(p -> "BASIC".equalsIgnoreCase(p.getNombre()))
                                .findFirst()
                                .orElseThrow();

                        Plan premium = planRepository.findAll().stream()
                                .filter(p -> "PREMIUM".equalsIgnoreCase(p.getNombre()))
                                .findFirst()
                                .orElseThrow();

                        // =========================================================
                        // USUARIO + SUSCRIPCIÓN (para tener algo que ver en pantalla)
                        // =========================================================

                        Usuario usuario = usuarioRepository.buscarPorEmail(EMAIL_PRUEBA)
                                .orElseGet(() -> usuarioRepository.save(new Usuario(EMAIL_PRUEBA, "ES")));

                        Suscripcion suscripcion = suscripcionRepository.buscarPorUsuarioId(usuario.getId())
                                .orElseGet(() -> suscripcionRepository.save(new Suscripcion(usuario, basic)));

                        // Cambio de plan para generar alguna revisión en auditoría
                        if (!"PREMIUM".equalsIgnoreCase(suscripcion.getPlan().getNombre())) {
                                suscripcion.setPlan(premium);
                                suscripcionRepository.save(suscripcion);
                        }

                        return null;
                });
        }
}
