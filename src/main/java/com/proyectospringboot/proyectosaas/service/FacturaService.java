package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import com.proyectospringboot.proyectosaas.domain.enums.EstadoSuscripcion;
import com.proyectospringboot.proyectosaas.repository.FacturaRepository;
import com.proyectospringboot.proyectosaas.repository.PlanRepository;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FacturaService {

    private static final String CONCEPTO_RENOVACION = "Renovación Mensual";

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
                LocalDateTime.now(),
                importeBase,
                impuesto,
                total,
                CONCEPTO_RENOVACION);
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
                    LocalDateTime.now(),
                    importeBase,
                    impuesto,
                    total,
                    CONCEPTO_RENOVACION);
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

    public BigDecimal calcularImpuesto(Suscripcion suscripcion, BigDecimal importeBase) {
        return calcularImpuesto(suscripcion.getUsuario().getPais(), importeBase);
    }

    public BigDecimal calcularImpuesto(String pais, BigDecimal importeBase) {
        if (pais != null && (pais.equalsIgnoreCase("ES")
                || pais.equalsIgnoreCase("España")
                || pais.equalsIgnoreCase("Spain"))) {
            return importeBase.multiply(BigDecimal.valueOf(0.21))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    // =========================================================
    // VERIFICAR FACTURA PENDIENTE
    // =========================================================

    public boolean tieneFacturaPendiente(String email) {
        try {
            String hql = "SELECT COUNT(f) FROM Factura f " +
                    "WHERE f.suscripcion.usuario.email = :email " +
                    "AND NOT EXISTS (SELECT p FROM Pago p WHERE p.factura = f)";

            Long count = entityManager.createQuery(hql, Long.class)
                    .setParameter("email", email)
                    .getSingleResult();

            return count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // REGISTRAR PAGO CON DATOS ESPECÍFICOS
    // =========================================================

    @Transactional
    public void registrarPagoConDatos(String email, String tipoPago,
            String ultimos4, String titular,
            String emailPaypal,
            String iban, String referencia) {

        // Validar datos según el tipo de pago
        switch (tipoPago.toLowerCase()) {
            case "tarjeta":
                if (ultimos4 == null || !ultimos4.matches("\\d{4}")) {
                    throw new IllegalArgumentException(
                            "Los últimos 4 dígitos de la tarjeta deben ser exactamente 4 números");
                }
                if (titular == null || titular.trim().length() < 3) {
                    throw new IllegalArgumentException("El nombre del titular debe tener al menos 3 caracteres");
                }
                break;
            case "paypal":
                if (emailPaypal == null || !emailPaypal.contains("@")) {
                    throw new IllegalArgumentException("Debe proporcionar un email válido de PayPal");
                }
                break;
            case "transferencia":
                if (iban == null || iban.replace(" ", "").length() < 20) {
                    throw new IllegalArgumentException("Debe proporcionar un IBAN válido");
                }
                // Generar referencia automática si no se proporciona
                if (referencia == null || referencia.trim().isEmpty()) {
                    referencia = "REF-" + System.currentTimeMillis();
                }
                break;
            default:
                throw new IllegalArgumentException("Tipo de pago no soportado: " + tipoPago);
        }

        // Buscar o crear factura
        List<Factura> facturas = facturaRepository.buscarPorEmail(email);
        Factura facturaObjetivo;

        if (facturas.isEmpty()) {
            var suscripcion = suscripcionRepository.buscarPorEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            facturaObjetivo = new Factura(
                    suscripcion,
                    LocalDateTime.now(),
                    BigDecimal.ONE,
                    BigDecimal.ZERO,
                    BigDecimal.ONE,
                    "Factura de Prueba");
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
                // Si algo falla, continuamos
            }

            if (facturaPendiente != null) {
                facturaObjetivo = facturaPendiente;
            } else {
                var suscripcion = suscripcionRepository.buscarPorEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                facturaObjetivo = new Factura(
                        suscripcion,
                        LocalDateTime.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "Factura de Prueba (0€)");
                facturaRepository.save(facturaObjetivo);
            }
        }

        // Crear el pago específico con los datos proporcionados
        com.proyectospringboot.proyectosaas.domain.entity.Pago nuevoPago = null;
        LocalDateTime ahora = LocalDateTime.now();
        BigDecimal importe = facturaObjetivo.getTotal();

        switch (tipoPago.toLowerCase()) {
            case "tarjeta":
                nuevoPago = new com.proyectospringboot.proyectosaas.domain.entity.PagoTarjeta(
                        facturaObjetivo, importe, ahora,
                        ultimos4, titular);
                break;
            case "paypal":
                nuevoPago = new com.proyectospringboot.proyectosaas.domain.entity.PagoPaypal(
                        facturaObjetivo, importe, ahora,
                        emailPaypal);
                break;
            case "transferencia":
                nuevoPago = new com.proyectospringboot.proyectosaas.domain.entity.PagoTransferencia(
                        facturaObjetivo, importe, ahora,
                        iban.replace(" ", ""),
                        referencia);
                break;
        }

        entityManager.persist(nuevoPago);

        // Guardar el método de pago como preferencia del usuario
        var suscripcion = suscripcionRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Usuario usuario = suscripcion.getUsuario();
        usuario.setMetodoPagoPreferido(tipoPago);
        entityManager.merge(usuario);
    }

    // =========================================================
    // MÉTODO ANTIGUO (DEPRECATED) - Mantener por compatibilidad
    // =========================================================

    @Deprecated
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
                    LocalDateTime.now(),
                    BigDecimal.ONE,
                    BigDecimal.ZERO,
                    BigDecimal.ONE,
                    "Factura de Prueba");
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
                        LocalDateTime.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "Factura de Prueba (0€)");
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

    // =========================================================
    // RENOVACIÓN + PAGO AUTOMÁTICO (para el scheduler)
    // =========================================================

    @Transactional
    public void renovarYPagarAuto(Suscripcion suscripcion) {
        String email = suscripcion.getUsuario().getEmail();

        BigDecimal base = suscripcion.getPlan().getPrecioMensual();
        BigDecimal impuesto = calcularImpuesto(suscripcion, base);
        BigDecimal total = base.add(impuesto);

        Factura factura = new Factura(suscripcion, LocalDateTime.now(), base, impuesto, total, CONCEPTO_RENOVACION);
        facturaRepository.save(factura);

        suscripcion.setFechaFinCiclo(suscripcion.getFechaFinCiclo().plusDays(30));
        suscripcionRepository.save(suscripcion);

        // Pago automático con el método preferido guardado
        String metodo = suscripcion.getUsuario().getMetodoPagoPreferido();
        if (metodo == null)
            metodo = "tarjeta";

        com.proyectospringboot.proyectosaas.domain.entity.Pago pago = switch (metodo.toLowerCase()) {
            case "paypal" -> new com.proyectospringboot.proyectosaas.domain.entity.PagoPaypal(
                    factura, total, LocalDateTime.now(), email);
            case "transferencia" -> new com.proyectospringboot.proyectosaas.domain.entity.PagoTransferencia(
                    factura, total, LocalDateTime.now(), "ES00AUTO", "REF-AUTO-" + System.currentTimeMillis());
            default -> new com.proyectospringboot.proyectosaas.domain.entity.PagoTarjeta(
                    factura, total, LocalDateTime.now(), "AUTO", "Pago Automático");
        };

        entityManager.persist(pago);
    }

    // Cancela suscripciones que llevan más de 3 días vencidas sin pagar
    @Transactional
    public int cancelarExpiradas() {
        LocalDateTime limite = LocalDateTime.now().minusDays(3); // 3 días de margen

        List<Suscripcion> candidatas = suscripcionRepository
                .buscarVencidas(EstadoSuscripcion.ACTIVA, limite);

        int canceladas = 0;

        for (Suscripcion s : candidatas) {
            // Solo cancelamos si tiene factura pendiente de pago (no ha pagado)
            boolean tienePendiente = tieneFacturaPendiente(s.getUsuario().getEmail());
            if (tienePendiente) {
                s.cancelar();
                suscripcionRepository.save(s);
                canceladas++;
            }
        }

        return canceladas;
    }
}
