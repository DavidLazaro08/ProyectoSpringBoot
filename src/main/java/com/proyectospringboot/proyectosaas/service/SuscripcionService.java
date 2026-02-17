package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.enums.EstadoSuscripcion;
import com.proyectospringboot.proyectosaas.repository.FacturaRepository;
import com.proyectospringboot.proyectosaas.repository.PlanRepository;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final PlanRepository planRepository;
    private final FacturaRepository facturaRepository;
    private final FacturaService facturaService;

    public SuscripcionService(SuscripcionRepository suscripcionRepository,
            PlanRepository planRepository,
            FacturaRepository facturaRepository,
            FacturaService facturaService) {
        this.suscripcionRepository = suscripcionRepository;
        this.planRepository = planRepository;
        this.facturaRepository = facturaRepository;
        this.facturaService = facturaService;
    }

    /*
     * Cambia el plan de un usuario.
     * Si es un UPGRADE (precio nuevo > actual), cobra la diferencia prorrateada.
     * Si es un DOWNGRADE o IGUAL, no cobra nada (MVP).
     */
    @Transactional
    public void cambiarPlan(Long usuarioId, Long nuevoPlanId) {
        // 1. Obtener Suscripción (Activa o no, la traemos por usuario)
        Suscripcion suscripcion = suscripcionRepository.buscarPorUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no tiene suscripción."));

        if (suscripcion.getEstado() != EstadoSuscripcion.ACTIVA) {
            throw new IllegalArgumentException("La suscripción no está activa.");
        }

        // 2. Obtener Nuevo Plan
        Plan nuevoPlan = planRepository.findById(nuevoPlanId)
                .orElseThrow(() -> new IllegalArgumentException("El plan destino no existe."));

        Plan planActual = suscripcion.getPlan();

        // 3. Validar que no sea el mismo plan
        if (planActual.getId().equals(nuevoPlan.getId())) {
            throw new IllegalArgumentException("El usuario ya tiene el plan " + nuevoPlan.getNombre());
        }

        // 4. Calcular Prorrateo e Impuestos si corresponde
        gestionProrrateo(suscripcion, planActual, nuevoPlan);

        // 5. Actualizar Plan (Esto dispara la auditoría de Envers automáticamente)
        suscripcion.setPlan(nuevoPlan);
        suscripcionRepository.save(suscripcion);
    }

    private void gestionProrrateo(Suscripcion suscripcion, Plan planActual, Plan nuevoPlan) {
        BigDecimal precioActual = planActual.getPrecioMensual();
        BigDecimal precioNuevo = nuevoPlan.getPrecioMensual();

        // Solo cobramos si es un Upgrade
        if (precioNuevo.compareTo(precioActual) > 0) {
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime finCiclo = suscripcion.getFechaFinCiclo();

            long diasRestantes = ChronoUnit.DAYS.between(ahora, finCiclo);

            // Si quedan días, calculamos la diferencia
            if (diasRestantes > 0) {
                BigDecimal diferenciaPrecio = precioNuevo.subtract(precioActual);

                // Fórmula: diferencia * (diasRestantes / 30)
                BigDecimal factorTiempo = BigDecimal.valueOf(diasRestantes)
                        .divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);

                BigDecimal importeProrrateado = diferenciaPrecio.multiply(factorTiempo)
                        .setScale(2, RoundingMode.HALF_UP);

                if (importeProrrateado.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal impuesto = facturaService.calcularImpuesto(suscripcion.getUsuario().getPais(),
                            importeProrrateado);
                    BigDecimal total = importeProrrateado.add(impuesto);

                    Factura factura = new Factura(
                            suscripcion,
                            ahora,
                            importeProrrateado,
                            impuesto,
                            total,
                            "Cambio de plan a " + nuevoPlan.getNombre() + " (Prorrateo " + diasRestantes + " días)");

                    facturaRepository.save(factura);
                }
            }
        }
    }
}
