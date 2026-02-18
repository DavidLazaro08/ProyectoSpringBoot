package com.proyectospringboot.proyectosaas.config;

import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.enums.EstadoSuscripcion;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import com.proyectospringboot.proyectosaas.service.FacturaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/*
 * Scheduler para tareas automáticas nocturnas.
 * Se encarga de:
 * 1. Renovar y cobrar suscripciones con "pago automático" activado.
 * 2. Cancelar suscripciones que llevan días impagadas (sin pago automático).
 */
@Component
public class RenovacionScheduler {

    private final SuscripcionRepository suscripcionRepository;
    private final FacturaService facturaService;

    public RenovacionScheduler(SuscripcionRepository suscripcionRepository, FacturaService facturaService) {
        this.suscripcionRepository = suscripcionRepository;
        this.facturaService = facturaService;
    }

    // Se ejecuta todos los días a las 2:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void procesarSuscripciones() {
        System.out.println("Inicio del proceso nocturno de suscripciones...");

        // 1. Renovación Automática (Domiciliación)
        List<Suscripcion> paraRenovar = suscripcionRepository.buscarVencidas(EstadoSuscripcion.ACTIVA,
                LocalDateTime.now());
        int renovadas = 0;

        for (Suscripcion s : paraRenovar) {
            if (s.getUsuario().isPagoAutomatico()) {
                try {
                    facturaService.renovarYPagarAuto(s);
                    renovadas++;
                    System.out.println("Renovada automáticamente: " + s.getUsuario().getEmail());
                } catch (Exception e) {
                    System.err.println("Error al renovar auto " + s.getUsuario().getEmail() + ": " + e.getMessage());
                }
            }
        }

        // 2. Cancelación por impago (si no tiene pago auto y pasaron 3 días)
        int canceladas = facturaService.cancelarExpiradas();

        System.out
                .println("Proceso finalizado. Renovadas auto: " + renovadas + ". Canceladas por impago: " + canceladas);
    }
}
