package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.enums.EstadoSuscripcion;
import com.proyectospringboot.proyectosaas.repository.FacturaRepository;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final SuscripcionRepository suscripcionRepository;

    public FacturaService(FacturaRepository facturaRepository,
            SuscripcionRepository suscripcionRepository) {
        this.facturaRepository = facturaRepository;
        this.suscripcionRepository = suscripcionRepository;
    }

    public List<Factura> buscarFacturasPorEmail(String email) {
        return facturaRepository.findBySuscripcionUsuarioEmailOrderByFechaDesc(email);
    }

    @Transactional
    public RenovacionResultado renovarSiToca(String email) {
        var suscripcionOpt = suscripcionRepository.findByUsuarioEmail(email);

        if (suscripcionOpt.isEmpty()) {
            return new RenovacionResultado(false, "No se encontró suscripción para este email");
        }

        Suscripcion suscripcion = suscripcionOpt.get();

        if (suscripcion.getEstado() != EstadoSuscripcion.ACTIVA) {
            return new RenovacionResultado(false, "La suscripción no está activa");
        }

        if (suscripcion.getFechaFinCiclo().isAfter(LocalDateTime.now())) {
            return new RenovacionResultado(false,
                    "Aún no toca renovar. Próxima renovación: " + suscripcion.getFechaFinCiclo());
        }

        BigDecimal importeBase = suscripcion.getPlan().getPrecioMensual();
        BigDecimal impuesto = calcularImpuesto(suscripcion, importeBase);
        BigDecimal total = importeBase.add(impuesto);

        Factura nuevaFactura = new Factura(
                suscripcion,
                importeBase,
                impuesto,
                total,
                LocalDateTime.now());
        facturaRepository.save(nuevaFactura);

        suscripcion.setFechaFinCiclo(suscripcion.getFechaFinCiclo().plusDays(30));
        suscripcionRepository.save(suscripcion);

        return new RenovacionResultado(true, "Renovación exitosa. Nueva factura generada.");
    }

    @Transactional
    public int generarFacturasPendientes() {
        List<Suscripcion> suscripcionesVencidas = suscripcionRepository
                .findByEstadoAndFechaFinCicloBefore(EstadoSuscripcion.ACTIVA, LocalDateTime.now());

        int contador = 0;
        for (Suscripcion suscripcion : suscripcionesVencidas) {
            BigDecimal importeBase = suscripcion.getPlan().getPrecioMensual();
            BigDecimal impuesto = calcularImpuesto(suscripcion, importeBase);
            BigDecimal total = importeBase.add(impuesto);

            Factura nuevaFactura = new Factura(
                    suscripcion,
                    importeBase,
                    impuesto,
                    total,
                    LocalDateTime.now());
            facturaRepository.save(nuevaFactura);

            suscripcion.setFechaFinCiclo(suscripcion.getFechaFinCiclo().plusDays(30));
            suscripcionRepository.save(suscripcion);

            contador++;
        }

        return contador;
    }

    private BigDecimal calcularImpuesto(Suscripcion suscripcion, BigDecimal importeBase) {
        String pais = suscripcion.getUsuario().getPais();

        if ("ES".equalsIgnoreCase(pais)) {
            return importeBase.multiply(BigDecimal.valueOf(0.21))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    public record RenovacionResultado(
            boolean exito,
            String mensaje) {
    }
}
