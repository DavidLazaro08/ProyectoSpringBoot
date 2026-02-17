package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

        // Simulamos que el usuario tiene el plan BASIC
        suscripcion = new Suscripcion(usuario, planBasic);
        // Ponemos fecha fin ciclo dentro de 15 días para probar prorrateo
        suscripcion.setFechaFinCiclo(LocalDateTime.now().plusDays(15));
    }

    @Test
    void testUpgradeGeneraFactura() {
        // GIVEN
        when(suscripcionRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(2L)).thenReturn(Optional.of(planPremium));
        // Mock de impuestos para evitar NullPointerException o ceros inesperados
        when(facturaService.calcularImpuesto(anyString(), any(BigDecimal.class))).thenReturn(new BigDecimal("4.20"));

        // WHEN
        suscripcionService.cambiarPlan(1L, 2L);

        // THEN
        // 1. El plan debe haber cambiado
        assertEquals(planPremium, suscripcion.getPlan());

        // 2. Se debe haber guardado la suscripción
        verify(suscripcionRepository).save(suscripcion);

        // 3. Se debe haber generado una factura de prorrateo
        ArgumentCaptor<Factura> facturaCaptor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(facturaCaptor.capture());

        Factura facturaGenerada = facturaCaptor.getValue();

        assertTrue(facturaGenerada.getImporte().compareTo(BigDecimal.ZERO) > 0);

        // Verificamos concepto
        assertTrue(facturaGenerada.getConcepto().contains("Cambio de plan"));
    }

    @Test
    void testDowngradeNoGeneraFactura() {
        // GIVEN: Usuario tiene Premium y baja a Basic
        suscripcion.setPlan(planPremium);

        when(suscripcionRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findById(2L)).thenReturn(Optional.of(planBasic));

        // WHEN
        suscripcionService.cambiarPlan(1L, 2L);

        // THEN
        // 1. El plan cambia
        assertEquals(planBasic, suscripcion.getPlan());

        // 2. NO se genera factura
        verify(facturaRepository, never()).save(any(Factura.class));
    }
}
