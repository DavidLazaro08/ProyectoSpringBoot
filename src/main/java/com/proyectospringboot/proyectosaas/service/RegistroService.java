package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.*;
import com.proyectospringboot.proyectosaas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/* RegistroService
 *
 * Se encarga del alta completa de un usuario:
 * - Usuario
 * - Perfil
 * - Suscripción
 * - Primera factura
 *
 * También construye los datos necesarios para el dashboard. */

@Service
public class RegistroService {

        private final UsuarioRepository usuarioRepository;
        private final PerfilRepository perfilRepository;
        private final SuscripcionRepository suscripcionRepository;
        private final PlanRepository planRepository;
        private final FacturaRepository facturaRepository;

        public RegistroService(
                        UsuarioRepository usuarioRepository,
                        PerfilRepository perfilRepository,
                        SuscripcionRepository suscripcionRepository,
                        PlanRepository planRepository,
                        FacturaRepository facturaRepository) {

                this.usuarioRepository = usuarioRepository;
                this.perfilRepository = perfilRepository;
                this.suscripcionRepository = suscripcionRepository;
                this.planRepository = planRepository;
                this.facturaRepository = facturaRepository;
        }

        // =========================================================
        // REGISTRO
        // =========================================================

        @Transactional
        public Usuario registrar(String email,
                        String pais,
                        String nombre,
                        String apellidos,
                        String telefono,
                        Long planId) {

                Usuario usuario = new Usuario(email, pais);
                usuario = usuarioRepository.save(usuario);

                Perfil perfil = new Perfil(usuario, nombre, apellidos, telefono);
                perfilRepository.save(perfil);

                Plan plan = planRepository.findById(planId)
                                .orElseThrow(() -> new IllegalArgumentException("Plan no existe: " + planId));

                Suscripcion suscripcion = new Suscripcion(usuario, plan);
                suscripcion = suscripcionRepository.save(suscripcion);

                BigDecimal importeBase = plan.getPrecioMensual();
                BigDecimal impuesto = calcularImpuesto(pais, importeBase);
                BigDecimal total = importeBase.add(impuesto);

                Factura factura = new Factura(
                                suscripcion,
                                importeBase,
                                impuesto,
                                total,
                                LocalDateTime.now());

                facturaRepository.save(factura);

                return usuario;
        }

        // Cálculo sencillo de IVA (21% si el usuario es de España)
        private BigDecimal calcularImpuesto(String pais, BigDecimal importeBase) {

                if (pais != null &&
                                (pais.equalsIgnoreCase("ES")
                                                || pais.equalsIgnoreCase("España")
                                                || pais.equalsIgnoreCase("Spain"))) {

                        return importeBase.multiply(BigDecimal.valueOf(0.21))
                                        .setScale(2, java.math.RoundingMode.HALF_UP);
                }

                return BigDecimal.ZERO;
        }

        // =========================================================
        // DASHBOARD
        // =========================================================

        @Transactional(readOnly = true)
        public DashboardDTO getDashboard(Long usuarioId) {

                Usuario usuario = usuarioRepository.findById(usuarioId)
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe: " + usuarioId));

                Perfil perfil = perfilRepository.buscarPorUsuarioId(usuarioId)
                                .orElseThrow(() -> new IllegalArgumentException("Perfil no existe"));

                Suscripcion suscripcion = suscripcionRepository.buscarPorUsuarioId(usuarioId)
                                .orElseThrow(() -> new IllegalArgumentException("Suscripción no existe"));

                List<Factura> facturas = facturaRepository.buscarPorSuscripcionOrdenadas(suscripcion.getId());

                Factura ultima = facturas.isEmpty() ? null : facturas.get(0);

                String nombreCompleto = perfil.getNombre() + " " + perfil.getApellidos();

                BigDecimal importeFactura = (ultima != null) ? ultima.getImporte() : null;

                String fechaFactura = (ultima != null) ? ultima.getFecha().toString() : null;

                return new DashboardDTO(
                                nombreCompleto,
                                usuario.getEmail(),
                                usuario.getPais(),
                                suscripcion.getPlan().getNombre(),
                                suscripcion.getEstado().name(),
                                importeFactura,
                                fechaFactura);
        }

        /*
         * DTO simple para devolver los datos del panel.
         * Lo dejamos como record para no crear una clase aparte.
         */

        public record DashboardDTO(
                        String nombreCompleto,
                        String email,
                        String pais,
                        String planNombre,
                        String estado,
                        BigDecimal importeFactura,
                        String fechaFactura) {

                // Getters clásicos para compatibilidad con vistas
                public String getNombreCompleto() {
                        return nombreCompleto;
                }

                public String getEmail() {
                        return email;
                }

                public String getPais() {
                        return pais;
                }

                public String getPlanNombre() {
                        return planNombre;
                }

                public String getEstado() {
                        return estado;
                }

                public BigDecimal getImporteFactura() {
                        return importeFactura;
                }

                public String getFechaFactura() {
                        return fechaFactura;
                }
        }
}
