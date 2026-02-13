package com.proyectospringboot.proyectosaas.config;

import com.proyectospringboot.proyectosaas.service.FacturaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/* RenovacionScheduler:
 * Tarea programada que revisa diariamente las suscripciones vencidas
 * y genera automáticamente las facturas correspondientes.
 *
 * Se ejecuta cada día a las 02:00. */

@Configuration
@EnableScheduling
public class RenovacionScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(RenovacionScheduler.class);

    private final FacturaService facturaService;

    public RenovacionScheduler(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    // =========================================================
    // TAREA PROGRAMADA DE RENOVACIÓN
    // =========================================================

    @Scheduled(cron = "0 0 2 * * *")
    public void ejecutarRenovacionesAutomaticas() {

        log.info("Iniciando renovaciones automáticas...");

        int facturasGeneradas =
                facturaService.generarFacturasPendientes();

        log.info("Facturas generadas: {}", facturasGeneradas);
    }
}
