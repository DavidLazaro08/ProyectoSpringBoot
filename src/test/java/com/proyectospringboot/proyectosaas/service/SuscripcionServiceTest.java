package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import com.proyectospringboot.proyectosaas.domain.enums.EstadoSuscripcion;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        usuario = new Usuario("test@test.com", "ES");

        planBasic = new Plan("BASIC", new BigDecimal("10.00"));
        planPremium = new Plan("PREMIUM", new BigDecimal("50.00"));

        // En tests unitarios no hay BD, así que el id no se genera solo.
        // Como el servicio compara por id, se lo asignamos manualmente.
        ponerId(planBasic, 2L);
        ponerId(planPremium, 1L);

        suscripcion = new Suscripcion(usuario, planBasic);

        // Dejamos el ciclo a mitad de camino para que haya prorrateo en un upgrade
        suscripcion.setFechaFinCiclo(LocalDateTime.now().plusDays(15).plusHours(1));
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
    }

    @Test
    void cambiarPlan_upgrade_generaFacturaProrrateo() {

        when(suscripcionRepository.buscarPorUsuarioId(1L))
                .thenReturn(Optional.of(suscripcion));

        when(planRepository.findById(1L))
                .thenReturn(Optional.of(planPremium));

        // Para este test no nos interesa el cálculo real del impuesto: lo fijamos a mano
        when(facturaService.calcularImpuesto(anyString(), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("4.20"));

        suscripcionService.cambiarPlan(1L, 1L);

        assertEquals(planPremium, suscripcion.getPlan());
        verify(suscripcionRepository).save(suscripcion);

        ArgumentCaptor<Factura> facturaCaptor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(facturaCaptor.capture());

        Factura facturaGenerada = facturaCaptor.getValue();

        BigDecimal importeEsperado = new BigDecimal("20.00");
        assertEquals(0, facturaGenerada.getImporte().compareTo(importeEsperado),
                "El importe prorrateado debe coincidir con lo esperado");

        BigDecimal totalEsperado = importeEsperado.add(new BigDecimal("4.20"));
        assertEquals(0, facturaGenerada.getTotal().compareTo(totalEsperado),
                "El total debe ser la suma de base + impuesto");

        assertTrue(facturaGenerada.getConcepto().contains("Cambio de plan"));
    }

    @Test
    void cambiarPlan_downgrade_noGeneraFactura() {

        suscripcion.setPlan(planPremium);

        when(suscripcionRepository.buscarPorUsuarioId(1L))
                .thenReturn(Optional.of(suscripcion));

        when(planRepository.findById(2L))
                .thenReturn(Optional.of(planBasic));

        suscripcionService.cambiarPlan(1L, 2L);

        assertEquals(planBasic, suscripcion.getPlan());

        verify(facturaRepository, never()).save(any(Factura.class));
        verify(facturaService, never()).calcularImpuesto(anyString(), any(BigDecimal.class));
        verify(suscripcionRepository).save(suscripcion);
    }

    @Test
    void cambiarPlan_suscripcionNoActiva_lanzaExcepcion() {

        suscripcion.setEstado(EstadoSuscripcion.CANCELADA);

        when(suscripcionRepository.buscarPorUsuarioId(1L))
                .thenReturn(Optional.of(suscripcion));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> suscripcionService.cambiarPlan(1L, 2L)
        );

        assertTrue(ex.getMessage().contains("no está activa"));

        verify(suscripcionRepository, never()).save(any());
        verify(facturaRepository, never()).save(any());
    }

    // =========================================================
    // UTILIDAD DE TEST (solo aquí)
    // =========================================================

    private void ponerId(Plan plan, Long id) {
        try {
            Field field = Plan.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(plan, id);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo asignar el id al Plan en el test", e);
        }
    }
}
