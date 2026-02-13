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

/* FacturaService:
 * - Semana 2 - Generamos facturas cuando vence el ciclo de una suscripción.
 * - Calculamos el impuesto por país (regla sencilla).
 * - Incluimos un par de utilidades que usamos durante el desarrollo (pagos / migración). */
@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public FacturaService(FacturaRepository facturaRepository,
            SuscripcionRepository suscripcionRepository,
            jakarta.persistence.EntityManager entityManager) {
        this.facturaRepository = facturaRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.entityManager = entityManager;
    }

    // =========================================================
    // CONSULTAS DE FACTURAS (Dashboard)
    // =========================================================

    public List<Factura> buscarFacturasPorEmail(String email) {
        // Hay facturas de 0€ que usamos solo como apoyo para pruebas de pago.
        // Aquí devolvemos solo las facturas "normales".
        return facturaRepository.buscarPorEmail(email).stream()
                .filter(f -> f.getTotal().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    public List<Factura> buscarFacturasConFiltros(String email,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            BigDecimal totalMin,
            BigDecimal totalMax) {
        return facturaRepository.buscarConFiltros(email, fechaInicio, fechaFin, totalMin, totalMax);
    }

    // =========================================================
    // RENOVACIÓN / GENERACIÓN DE FACTURAS
    // =========================================================

    @Transactional
    public RenovacionResultado renovarSiToca(String email) {
        var suscripcionOpt = suscripcionRepository.buscarPorEmail(email);

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

        return new RenovacionResultado(true, "Renovación generada. Queda pendiente de pago.");
    }

    @Transactional
    public int generarFacturasPendientes() {
        List<Suscripcion> suscripcionesVencidas = suscripcionRepository
                .buscarVencidas(EstadoSuscripcion.ACTIVA, LocalDateTime.now());

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

    // =========================================================
    // UTILIDAD (por si hiciera falta)
    // =========================================================

    @Transactional
    public int migrarFacturasAntiguas() {
        // Si alguna factura se creó antes de aplicar impuestos, aquí se corrige.
        // Al ir implementando todo progresivamente tuvimos problemas con ello.
        List<Factura> facturasConCero = facturaRepository.findAll().stream()
                .filter(f -> f.getImpuesto().compareTo(BigDecimal.ZERO) == 0)
                .toList();

        int contador = 0;

        for (Factura factura : facturasConCero) {
            BigDecimal importeBase = factura.getImporte();
            BigDecimal impuesto = calcularImpuesto(factura.getSuscripcion(), importeBase);

            if (impuesto.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal nuevoTotal = importeBase.add(impuesto);

                entityManager.createQuery(
                        "UPDATE Factura f SET f.impuesto = :imp, f.total = :tot WHERE f.id = :id")
                        .setParameter("imp", impuesto)
                        .setParameter("tot", nuevoTotal)
                        .setParameter("id", factura.getId())
                        .executeUpdate();

                contador++;
            }
        }

        return contador;
    }

    // =========================================================
    // IMPUESTOS (Semana 2)
    // =========================================================

    private BigDecimal calcularImpuesto(Suscripcion suscripcion, BigDecimal importeBase) {
        String pais = suscripcion.getUsuario().getPais();

        if (pais != null && (pais.equalsIgnoreCase("ES")
                || pais.equalsIgnoreCase("España")
                || pais.equalsIgnoreCase("Spain"))) {
            return importeBase.multiply(BigDecimal.valueOf(0.21))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    // =========================================================
    // PAGO (solo para probar la herencia)
    // =========================================================

    @Transactional
    public void registrarPagoPrueba(String email, String tipoPago) {
        // Solo para pruebas: creamos un Pago (Tarjeta/PayPal/Transferencia) y lo
        // asociamos a una factura.
        List<Factura> facturas = facturaRepository.buscarPorEmail(email);

        Factura facturaObjetivo;

        if (facturas.isEmpty()) {
            var suscripcion = suscripcionRepository.buscarPorEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            facturaObjetivo = new Factura(
                    suscripcion,
                    BigDecimal.ONE,
                    BigDecimal.ZERO,
                    BigDecimal.ONE,
                    LocalDateTime.now());
            facturaRepository.save(facturaObjetivo);

        } else {
            Factura facturaPendiente = null;

            try {
                String hql = "SELECT f FROM Factura f " +
                        "WHERE f.suscripcion.usuario.email = :email " +
                        "AND NOT EXISTS (SELECT p FROM Pago p WHERE p.factura = f) " +
                        "ORDER BY f.fecha DESC";

                List<Factura> pendientes = entityManager.createQuery(hql, Factura.class)
                        .setParameter("email", email)
                        .setMaxResults(1)
                        .getResultList();

                if (!pendientes.isEmpty()) {
                    facturaPendiente = pendientes.get(0);
                }
            } catch (Exception e) {
                // Si algo falla aquí, no queremos romper el flujo del panel.
            }

            if (facturaPendiente != null) {
                facturaObjetivo = facturaPendiente;
            } else {
                var suscripcion = suscripcionRepository.buscarPorEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                // Factura 0€ solo para poder guardar un pago de prueba.
                facturaObjetivo = new Factura(
                        suscripcion,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        LocalDateTime.now());
                facturaRepository.save(facturaObjetivo);
            }
        }

        com.proyectospringboot.proyectosaas.domain.entity.Pago nuevoPago = null;
        LocalDateTime ahora = LocalDateTime.now();
        BigDecimal importe = facturaObjetivo.getTotal();

        // Datos simulados: lo importante aquí es la herencia de Pago.
        switch (tipoPago.toLowerCase()) {
            case "tarjeta":
                nuevoPago = new com.proyectospringboot.proyectosaas.domain.entity.PagoTarjeta(
                        facturaObjetivo, importe, ahora,
                        "4242", "Usuario Demo");
                break;
            case "paypal":
                nuevoPago = new com.proyectospringboot.proyectosaas.domain.entity.PagoPaypal(
                        facturaObjetivo, importe, ahora,
                        email);
                break;
            case "transferencia":
                nuevoPago = new com.proyectospringboot.proyectosaas.domain.entity.PagoTransferencia(
                        facturaObjetivo, importe, ahora,
                        "ES91 2100 0000 0000 0000 0000",
                        "REF-" + System.currentTimeMillis());
                break;
            default:
                throw new IllegalArgumentException("Tipo de pago no soportado: " + tipoPago);
        }

        entityManager.persist(nuevoPago);
    }

    public boolean estaPagada(Long facturaId) {
        try {
            Long count = entityManager.createQuery(
                    "SELECT count(p) FROM Pago p WHERE p.factura.id = :id", Long.class)
                    .setParameter("id", facturaId)
                    .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String obtenerUltimoMetodoPago(String email) {
        List<Factura> facturas = facturaRepository.buscarPorEmail(email);

        for (Factura factura : facturas) {
            try {
                String hql = "SELECT p FROM Pago p WHERE p.factura.id = :facturaId";
                List<com.proyectospringboot.proyectosaas.domain.entity.Pago> pagos = entityManager
                        .createQuery(hql, com.proyectospringboot.proyectosaas.domain.entity.Pago.class)
                        .setParameter("facturaId", factura.getId())
                        .setMaxResults(1)
                        .getResultList();

                if (!pagos.isEmpty()) {
                    var pago = pagos.get(0);
                    if (pago instanceof com.proyectospringboot.proyectosaas.domain.entity.PagoTarjeta) {
                        return "Tarjeta";
                    } else if (pago instanceof com.proyectospringboot.proyectosaas.domain.entity.PagoPaypal) {
                        return "PayPal";
                    } else if (pago instanceof com.proyectospringboot.proyectosaas.domain.entity.PagoTransferencia) {
                        return "Transferencia";
                    }
                }
            } catch (Exception e) {
                // Seguimos buscando en otras facturas.
            }
        }

        return "Tarjeta";
    }

    public record RenovacionResultado(boolean exito, String mensaje) {
    }
}
