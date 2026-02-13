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
    private final jakarta.persistence.EntityManager entityManager;

    public FacturaService(FacturaRepository facturaRepository,
            SuscripcionRepository suscripcionRepository,
            jakarta.persistence.EntityManager entityManager) {
        this.facturaRepository = facturaRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.entityManager = entityManager;
    }

    public List<Factura> buscarFacturasPorEmail(String email) {
        // Filtrar facturas "dummy" de 0€ (usadas solo para guardar métodos de pago)
        return facturaRepository.findBySuscripcionUsuarioEmailOrderByFechaDesc(email).stream()
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
    /* ... skipped parts ... */

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

        // GENERAMOS LA FACTURA PERO NO LA PAGAMOS AUTOMÁTICAMENTE
        // Esto permite al usuario "Guardar/Pagar" después desde el Dashboard
        // simulando un periodo de gracia.

        // Demo: Aplicar automáticamente el pago usando el método "heredado"
        // String metodoPago = obtenerUltimoMetodoPago(email);
        // registrarPagoDemo(email, metodoPago);

        suscripcion.setFechaFinCiclo(suscripcion.getFechaFinCiclo().plusDays(30));
        suscripcionRepository.save(suscripcion);

        return new RenovacionResultado(true,
                "Renovación generada (Pendiente de Pago). Dispones de unos días para regularizarla en el Dashboard.");
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

    // Migrar facturas antiguas que tienen impuesto=0 (creadas antes del cálculo)
    @Transactional
    public int migrarFacturasAntiguas() {
        // En lugar de borrar y crear (lo que rompe FK con Pagos),
        // buscamos las facturas incorrectas y las actualizamos vía UPDATE directo
        // (HQL),
        // ya que la entidad Factura es inmutable (sin setters).

        List<Factura> facturasConCero = facturaRepository.findAll().stream()
                .filter(f -> f.getImpuesto().compareTo(BigDecimal.ZERO) == 0)
                .toList();

        int contador = 0;
        for (Factura factura : facturasConCero) {
            BigDecimal importeBase = factura.getImporte();
            BigDecimal impuesto = calcularImpuesto(factura.getSuscripcion(), importeBase);

            // Solo actualizar si el impuesto calculado es > 0
            if (impuesto.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal nuevoTotal = importeBase.add(impuesto);

                // UPDATE directo para no romper integridad referencial
                entityManager.createQuery("UPDATE Factura f SET f.impuesto = :imp, f.total = :tot WHERE f.id = :id")
                        .setParameter("imp", impuesto)
                        .setParameter("tot", nuevoTotal)
                        .setParameter("id", factura.getId())
                        .executeUpdate();

                contador++;
            }
        }

        return contador;
    }

    // Cálculo de impuestos según país (MVP Semana 2)
    // Por ahora solo España con IVA 21%, resto sin impuestos
    private BigDecimal calcularImpuesto(Suscripcion suscripcion, BigDecimal importeBase) {
        String pais = suscripcion.getUsuario().getPais();

        // Acepta ES, España, spain (case-insensitive)
        if (pais != null && (pais.equalsIgnoreCase("ES") ||
                pais.equalsIgnoreCase("España") ||
                pais.equalsIgnoreCase("Spain"))) {
            return importeBase.multiply(BigDecimal.valueOf(0.21))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    // Demo: Registrar un pago simulado para la última factura disponible
    // Esto demuestra el uso de la herencia en la entidad Pago (Tarjeta, PayPal,
    // Transferencia)
    @Transactional
    public void registrarPagoDemo(String email, String tipoPago) {
        // Buscar la última factura del usuario (sea cual sea su estado, para la demo)
        List<Factura> facturas = facturaRepository.findBySuscripcionUsuarioEmailOrderByFechaDesc(email);

        Factura facturaObjetivo;

        if (facturas.isEmpty()) {
            // Si no hay facturas, creamos una "Factura de Validación" simbólica (ej. 1€)
            // para poder asociar el método de pago.
            var suscripcion = suscripcionRepository.findByUsuarioEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            facturaObjetivo = new Factura(
                    suscripcion,
                    BigDecimal.ONE, // 1.00 base
                    BigDecimal.ZERO, // 0 impuesto
                    BigDecimal.ONE, // 1.00 total
                    LocalDateTime.now());
            facturaRepository.save(facturaObjetivo);
        } else {
            // Buscamos si existe alguna factura PENDIENTE de pago real
            // (Aquella que no tenga registro en la tabla Pagos)
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
                // Error en consulta
            }

            if (facturaPendiente != null) {
                // Si hay deuda pendiente, el pago va para ella
                facturaObjetivo = facturaPendiente;
            } else {
                // Si NO hay deudas (todo pagado), creamos factura de VALIDACIÓN (0€)
                // para que el usuario pueda guardar su nuevo método de pago SIN cobrarle.
                // Esto simula una "Zero Auth" o tokenización.
                var suscripcion = suscripcionRepository.findByUsuarioEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                facturaObjetivo = new Factura(
                        suscripcion,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        LocalDateTime.now());
                facturaRepository.save(facturaObjetivo);
            }
        }

        // Crear el pago según el tipo (Herencia)
        // Usamos datos simulados para la demo
        com.proyectospringboot.proyectosaas.domain.entity.Pago nuevoPago = null;
        LocalDateTime ahora = LocalDateTime.now();
        BigDecimal importe = facturaObjetivo.getTotal();

        switch (tipoPago.toLowerCase()) {
            case "tarjeta":
                nuevoPago = new com.proyectospringboot.proyectosaas.domain.entity.PagoTarjeta(
                        facturaObjetivo, importe, ahora,
                        "4242", "Usuario Demo" // Datos simulados
                );
                break;
            case "paypal":
                nuevoPago = new com.proyectospringboot.proyectosaas.domain.entity.PagoPaypal(
                        facturaObjetivo, importe, ahora,
                        email // Cuenta PayPal = email usuario
                );
                break;
            case "transferencia":
                nuevoPago = new com.proyectospringboot.proyectosaas.domain.entity.PagoTransferencia(
                        facturaObjetivo, importe, ahora,
                        "ES91 2100 0000 0000 0000 0000", // IBAN simulado
                        "REF-" + System.currentTimeMillis() // Referencia única
                );
                break;
            default:
                throw new IllegalArgumentException("Tipo de pago no soportado: " + tipoPago);
        }

        // Guardar el pago usando EntityManager
        entityManager.persist(nuevoPago);
    }

    public boolean estaPagada(Long facturaId) {
        try {
            Long count = entityManager.createQuery("SELECT count(p) FROM Pago p WHERE p.factura.id = :id", Long.class)
                    .setParameter("id", facturaId)
                    .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // Método auxiliar para obtener el tipo de pago de la última factura pagada
    // Se usará para "heredar" el método en las renovaciones automaticas
    public String obtenerUltimoMetodoPago(String email) {
        // Obtenemos las últimas facturas
        List<Factura> facturas = facturaRepository.findBySuscripcionUsuarioEmailOrderByFechaDesc(email);

        for (Factura factura : facturas) {
            try {
                // Buscamos si existe un pago asociado a esta factura
                // Como no hay relación bidireccional en Factura, usamos query directa
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
                // Ignorar errores puntuales y seguir buscando
            }
        }

        return "Tarjeta"; // Default si no se encuentra historial
    }

    public record RenovacionResultado(
            boolean exito,
            String mensaje) {
    }
}
