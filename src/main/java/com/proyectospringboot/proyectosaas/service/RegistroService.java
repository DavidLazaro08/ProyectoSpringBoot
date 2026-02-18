package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.domain.entity.Perfil;
import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import com.proyectospringboot.proyectosaas.domain.enums.RolUsuario;
import com.proyectospringboot.proyectosaas.repository.FacturaRepository;
import com.proyectospringboot.proyectosaas.repository.PerfilRepository;
import com.proyectospringboot.proyectosaas.repository.PlanRepository;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import com.proyectospringboot.proyectosaas.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * - Primera factura (snapshot)
 *
 * Además, construye los datos básicos necesarios para el dashboard. */
@Service
public class RegistroService {

        private final UsuarioRepository usuarioRepository;
        private final PerfilRepository perfilRepository;
        private final SuscripcionRepository suscripcionRepository;
        private final PlanRepository planRepository;
        private final FacturaRepository facturaRepository;
        private final FacturaService facturaService;
        private final PasswordEncoder passwordEncoder;

        public RegistroService(UsuarioRepository usuarioRepository,
                        PerfilRepository perfilRepository,
                        SuscripcionRepository suscripcionRepository,
                        PlanRepository planRepository,
                        FacturaRepository facturaRepository,
                        FacturaService facturaService,
                        PasswordEncoder passwordEncoder) {

                this.usuarioRepository = usuarioRepository;
                this.perfilRepository = perfilRepository;
                this.suscripcionRepository = suscripcionRepository;
                this.planRepository = planRepository;
                this.facturaRepository = facturaRepository;
                this.facturaService = facturaService;
                this.passwordEncoder = passwordEncoder;
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
                        Long planId,
                        String password) {

                // Validación básica: no permitir emails duplicados.
                if (usuarioRepository.buscarPorEmail(email).isPresent()) {
                        throw new IllegalArgumentException("Ya existe un usuario con ese email.");
                }

                // Hashear contraseña con BCrypt antes de guardar
                String hashedPassword = passwordEncoder.encode(password);

                // Crear usuario con rol USER por defecto
                Usuario usuario = new Usuario(email, pais, hashedPassword, RolUsuario.USER);
                usuario = usuarioRepository.save(usuario);

                Perfil perfil = new Perfil(usuario, nombre, apellidos, telefono);
                perfilRepository.save(perfil);

                Plan plan = planRepository.findById(planId)
                                .orElseThrow(() -> new IllegalArgumentException("Plan no existe: " + planId));

                Suscripcion suscripcion = new Suscripcion(usuario, plan);
                suscripcion = suscripcionRepository.save(suscripcion);

                // Primera factura (snapshot económico de alta)
                BigDecimal importeBase = plan.getPrecioMensual();
                BigDecimal impuesto = facturaService.calcularImpuesto(pais, importeBase);
                BigDecimal total = importeBase.add(impuesto);

                Factura factura = new Factura(
                                suscripcion,
                                LocalDateTime.now(),
                                importeBase,
                                impuesto,
                                total,
                                "Alta de Suscripción");

                facturaRepository.save(factura);

                return usuario;
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

                // Para la vista nos vale así. (lo formateamos más bonito después).
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
         * Lo dejamos como record para no crear otra clase aparte.
         */
        public record DashboardDTO(
                        String nombreCompleto,
                        String email,
                        String pais,
                        String planNombre,
                        String estado,
                        BigDecimal importeFactura,
                        String fechaFactura) {
                // Getters (por si alguna vista/plantilla los necesita explícitos)
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
