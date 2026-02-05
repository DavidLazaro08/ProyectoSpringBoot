package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.*;
import com.proyectospringboot.proyectosaas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RegistroService {

        private final UsuarioRepository usuarioRepository;
        private final PerfilRepository perfilRepository;
        private final SuscripcionRepository suscripcionRepository;
        private final PlanRepository planRepository;
        private final FacturaRepository facturaRepository;

        public RegistroService(UsuarioRepository usuarioRepository,
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

        @Transactional
        public Usuario registrar(String email, String pais, String nombre, String apellidos, String telefono,
                        Long planId) {

                Usuario u = new Usuario(email, pais);
                u = usuarioRepository.save(u);

                Perfil p = new Perfil(u, nombre, apellidos, telefono);
                perfilRepository.save(p);

                Plan plan = planRepository.findById(planId)
                                .orElseThrow(() -> new IllegalArgumentException("Plan no existe: " + planId));

                Suscripcion s = new Suscripcion(u, plan);
                s = suscripcionRepository.save(s);

                Factura f = new Factura(s, plan.getPrecioMensual(), LocalDateTime.now());
                facturaRepository.save(f);

                return u;
        }

        @Transactional(readOnly = true)
        public DashboardDTO getDashboard(Long usuarioId) {
                Usuario u = usuarioRepository.findById(usuarioId)
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe: " + usuarioId));

                Perfil perfil = perfilRepository.findByUsuarioId(usuarioId)
                                .orElseThrow(() -> new IllegalArgumentException("Perfil no existe"));

                Suscripcion sus = suscripcionRepository.findByUsuarioId(usuarioId)
                                .orElseThrow(() -> new IllegalArgumentException("Suscripción no existe"));

                Factura ultima = facturaRepository.findTopBySuscripcionIdOrderByFechaDesc(sus.getId())
                                .orElse(null);

                String nombreCompleto = perfil.getNombre() + " " + perfil.getApellidos();

                BigDecimal importeFactura = (ultima != null) ? ultima.getImporte() : null;
                String fechaFactura = (ultima != null) ? ultima.getFecha().toString() : null;

                return new DashboardDTO(
                                nombreCompleto,
                                u.getEmail(),
                                u.getPais(),
                                sus.getPlan().getNombre(),
                                sus.getEstado().name(),
                                importeFactura,
                                fechaFactura);
        }

        public record DashboardDTO(
                        String nombreCompleto,
                        String email,
                        String pais,
                        String planNombre,
                        String estado,
                        BigDecimal importeFactura,
                        String fechaFactura) {
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
