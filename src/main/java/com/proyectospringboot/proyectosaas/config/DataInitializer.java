package com.proyectospringboot.proyectosaas.config;

import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import com.proyectospringboot.proyectosaas.domain.enums.RolUsuario;
import com.proyectospringboot.proyectosaas.repository.PlanRepository;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import com.proyectospringboot.proyectosaas.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

/* DataInitializer:
 * Carga datos mínimos para poder arrancar el proyecto sin meter nada a mano.
 * Se usa en desarrollo / pruebas, no es lógica de negocio. 
 * 
 * IMPORTANTE: Crea usuario ADMIN automáticamente para que el profesor pueda
 * probar el proyecto sin configuración adicional. */

@Configuration
public class DataInitializer {

        private static final String EMAIL_PRUEBA = "demo@saas.local";
        private static final String EMAIL_ADMIN = "admin@saas.com";
        private static final String PASSWORD_DEFAULT = "password123";

        @Bean
        CommandLineRunner initData(
                        PlanRepository planRepository,
                        UsuarioRepository usuarioRepository,
                        SuscripcionRepository suscripcionRepository,
                        PasswordEncoder passwordEncoder,
                        TransactionTemplate tx,
                        org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {

                return args -> {
                        // PARCHE DE MIGRACIÓN: Asegurar que la columna existe antes de cualquier
                        // consulta
                        try {
                                jdbcTemplate.execute(
                                                "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS pago_automatico BOOLEAN NOT NULL DEFAULT FALSE");
                                System.out.println(
                                                ">>> MIGRACIÓN: Columna 'pago_automatico' verificada correctamente.");
                        } catch (Exception e) {
                                System.out.println(">>> MIGRACIÓN: Aviso (probablemente ya existe): " + e.getMessage());
                        }

                        tx.execute(status -> {

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

                                // Hashear password usando el PasswordEncoder configurado
                                String hashedPassword = passwordEncoder.encode(PASSWORD_DEFAULT);

                                // =========================================================
                                // USUARIO ADMIN (para pruebas y corrección del proyecto)
                                // =========================================================

                                Usuario admin = usuarioRepository.buscarPorEmail(EMAIL_ADMIN)
                                                .orElseGet(() -> usuarioRepository.save(
                                                                new Usuario(EMAIL_ADMIN, "ES", hashedPassword,
                                                                                RolUsuario.ADMIN)));

                                // =========================================================
                                // USUARIO DEMO + SUSCRIPCIÓN (para tener algo que ver en pantalla)
                                // =========================================================

                                Usuario usuario = usuarioRepository.buscarPorEmail(EMAIL_PRUEBA)
                                                .orElseGet(() -> usuarioRepository.save(
                                                                new Usuario(EMAIL_PRUEBA, "ES", hashedPassword,
                                                                                RolUsuario.USER)));

                                Suscripcion suscripcion = suscripcionRepository.buscarPorUsuarioId(usuario.getId())
                                                .orElseGet(() -> suscripcionRepository
                                                                .save(new Suscripcion(usuario, basic)));

                                // Cambio de plan para generar alguna revisión en auditoría
                                if (!"PREMIUM".equalsIgnoreCase(suscripcion.getPlan().getNombre())) {
                                        suscripcion.setPlan(premium);
                                        suscripcionRepository.save(suscripcion);
                                }

                                return null;
                        });
                };
        }
}
