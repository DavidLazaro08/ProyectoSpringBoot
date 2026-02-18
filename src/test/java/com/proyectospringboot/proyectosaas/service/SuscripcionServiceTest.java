package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import com.proyectospringboot.proyectosaas.domain.enums.EstadoSuscripcion;
import com.proyectospringboot.proyectosaas.domain.enums.RolUsuario;
import com.proyectospringboot.proyectosaas.repository.FacturaRepository;
import com.proyectospringboot.proyectosaas.repository.PlanRepository;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/* SuscripcionServiceTest
 *
 * Probamos el cambio de plan sin base de datos:
 * - Upgrade: genera factura prorrateada.
 * - Downgrade: no genera factura (MVP).
 * - Suscripción no activa: lanza excepción.
 *
 * Usamos repositorios mockeados con Mockito para centrarnos en la lógica del servicio. */

@ExtendWith(MockitoExtension.class)
class SuscripcionServiceTest {

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private FacturaService facturaService;

    @InjectMocks
    private SuscripcionService suscripcionService;

    private Usuario usuario;
    private Suscripcion suscripcion;
    private Plan planBasic;
    private Plan planPremium;

    @BeforeEach
    void setUp() {

        // Usamos objetos reales para que el test no sea tan artificial
        usuario = new Usuario("test@test.com", "ES", "hashedPassword", RolUsuario.USER);

        planBasic = new Plan("BASIC", new BigDecimal("10.00"));
        ponerId(planBasic, 1L);

        planPremium = new Plan("PREMIUM", new BigDecimal("50.00"));
        ponerId(planPremium, 2L);

        // Suscripción empieza en BASIC y está ACTIVA
        suscripcion = new Suscripcion(usuario, planBasic);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);

        // Dejamos 15 días restantes para que el prorrateo del upgrade sea aprox. el 50%
        suscripcion.setFechaFinCiclo(LocalDateTime.now().plusDays(15).plusHours(1));
    }

    @Test
    void cambiarPlan_upgrade_generaFacturaProrrateada() {

        // BASIC (1) -> PREMIUM (2)
        when(suscripcionRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(2L)).thenReturn(Optional.of(planPremium));

        // El impuesto lo mockeamos porque es lógica de otro servicio
        when(facturaService.calcularImpuesto(eq("ES"), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("4.20"));

        // Ejecutamos el cambio
        suscripcionService.cambiarPlan(1L, 2L);

        // Se guarda la suscripción con el nuevo plan
        assertEquals(planPremium, suscripcion.getPlan());
        verify(suscripcionRepository).save(suscripcion);

        // Se genera factura de prorrateo
        ArgumentCaptor<Factura> facturaCaptor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(facturaCaptor.capture());

        Factura factura = facturaCaptor.getValue();

        // Diferencia: 50 - 10 = 40. Medio mes: 20.00
        BigDecimal importeEsperado = new BigDecimal("20.00");
        assertEquals(0, factura.getImporte().compareTo(importeEsperado));

        // Total: 20.00 + 4.20 = 24.20
        BigDecimal totalEsperado = new BigDecimal("24.20");
        assertEquals(0, factura.getTotal().compareTo(totalEsperado));

        assertTrue(factura.getConcepto().contains("Cambio de plan"));
    }

    @Test
    void cambiarPlan_downgrade_noGeneraFactura() {

        // Empezamos en PREMIUM (2)
        suscripcion.setPlan(planPremium);

        when(suscripcionRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(1L)).thenReturn(Optional.of(planBasic));

        // Ejecutamos el cambio a BASIC (MVP: no se cobra downgrade)
        suscripcionService.cambiarPlan(1L, 1L);

        assertEquals(planBasic, suscripcion.getPlan());
        verify(facturaRepository, never()).save(any(Factura.class));
        verify(facturaService, never()).calcularImpuesto(anyString(), any(BigDecimal.class));
        verify(suscripcionRepository).save(suscripcion);
    }

    @Test
    void cambiarPlan_estadoNoActivo_lanzaExcepcion() {

        suscripcion.setEstado(EstadoSuscripcion.CANCELADA);

        when(suscripcionRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(suscripcion));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> suscripcionService.cambiarPlan(1L, 2L));

        assertTrue(ex.getMessage().contains("no está activa"));

        verify(suscripcionRepository, never()).save(any());
        verify(facturaRepository, never()).save(any());
    }

    // =========================================================
    // UTILIDADES DE TEST
    // =========================================================

    private void ponerId(Object entidad, Long id) {
        try {
            Field field = entidad.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entidad, id);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo asignar el id en el test (reflexión).", e);
        }
    }
}
