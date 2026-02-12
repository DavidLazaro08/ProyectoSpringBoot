package com.proyectospringboot.proyectosaas.config;

import com.proyectospringboot.proyectosaas.service.FacturaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class RenovacionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RenovacionScheduler.class);

    private final FacturaService facturaService;

    public RenovacionScheduler(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void ejecutarRenovacionesAutomaticas() {
        log.info("Iniciando renovaciones automáticas...");
        int facturasGeneradas = facturaService.generarFacturasPendientes();
        log.info("Facturas generadas: {}", facturasGeneradas);
    }
}
