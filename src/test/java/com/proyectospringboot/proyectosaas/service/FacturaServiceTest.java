package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import com.proyectospringboot.proyectosaas.domain.enums.EstadoSuscripcion;
import com.proyectospringboot.proyectosaas.repository.FacturaRepository;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import com.proyectospringboot.proyectosaas.service.FacturaService.RenovacionResultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private FacturaService facturaService;

    private Usuario usuario;
    private Suscripcion suscripcion;
    private Plan plan;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("test@test.com", "ES");
        plan = new Plan("BASIC", new BigDecimal("10.00"));
        suscripcion = new Suscripcion(usuario, plan);
        // Por defecto activa
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
    }

    // 3) renovarSiToca: no existe suscripción -> no éxito
    @Test
    void testRenovarSiToca_NoExisteSuscripcion() {
        when(suscripcionRepository.buscarPorEmail("test@test.com")).thenReturn(Optional.empty());

        RenovacionResultado resultado = facturaService.renovarSiToca("test@test.com");

        assertFalse(resultado.exito());
        assertEquals("No se encontró suscripción para este email", resultado.mensaje());
        verify(facturaRepository, never()).save(any(Factura.class));
    }

    // 4) renovarSiToca: no activa -> no éxito
    @Test
    void testRenovarSiToca_NoActiva() {
        suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
        when(suscripcionRepository.buscarPorEmail("test@test.com")).thenReturn(Optional.of(suscripcion));

        RenovacionResultado resultado = facturaService.renovarSiToca("test@test.com");

        assertFalse(resultado.exito());
        assertTrue(resultado.mensaje().contains("no está activa"));
        verify(facturaRepository, never()).save(any(Factura.class));
    }

    // 5) renovarSiToca: aún no toca -> no éxito
    @Test
    void testRenovarSiToca_AunNoToca() {
        // Fecha fin en el futuro (+5 días)
        suscripcion.setFechaFinCiclo(LocalDateTime.now().plusDays(5));
        when(suscripcionRepository.buscarPorEmail("test@test.com")).thenReturn(Optional.of(suscripcion));

        RenovacionResultado resultado = facturaService.renovarSiToca("test@test.com");

        assertFalse(resultado.exito());
        assertTrue(resultado.mensaje().contains("Aún no toca renovar"));
        verify(facturaRepository, never()).save(any(Factura.class));
    }

    // Caso de Éxito de Renovación
    @Test
    void testRenovarSiToca_Exito() {
        // Fecha fin en el pasado (-1 día), toca renovar
        suscripcion.setFechaFinCiclo(LocalDateTime.now().minusDays(1));
        when(suscripcionRepository.buscarPorEmail("test@test.com")).thenReturn(Optional.of(suscripcion));

        RenovacionResultado resultado = facturaService.renovarSiToca("test@test.com");

        assertTrue(resultado.exito());

        // Verifica que se guarda la factura
        ArgumentCaptor<Factura> facturaCaptor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(facturaCaptor.capture());

        Factura factura = facturaCaptor.getValue();
        assertEquals("Renovación Mensual", factura.getConcepto());

        // Uso de compareTo para evitar problemas de escala en BigDecimal
        assertEquals(0, factura.getTotal().compareTo(new BigDecimal("12.10")));

        // Verifica que se actualiza la suscripción (fecha fin + 30 días)
        verify(suscripcionRepository).save(suscripcion);
    }

    // 6) generarFacturasPendientes: genera facturas para suscripciones vencidas
    @Test
    void testGenerarFacturasPendientes() {
        // Creamos 2 suscripciones vencidas
        Suscripcion s1 = new Suscripcion(new Usuario("u1@test.com", "ES"), plan);
        s1.setFechaFinCiclo(LocalDateTime.now().minusDays(1));

        Suscripcion s2 = new Suscripcion(new Usuario("u2@test.com", "FR"), plan); // Francia
        s2.setFechaFinCiclo(LocalDateTime.now().minusDays(2));

        when(suscripcionRepository.buscarVencidas(eq(EstadoSuscripcion.ACTIVA), any(LocalDateTime.class)))
                .thenReturn(List.of(s1, s2));

        int generadas = facturaService.generarFacturasPendientes();

        assertEquals(2, generadas);

        // Debe haber guardado 2 facturas
        verify(facturaRepository, times(2)).save(any(Factura.class));

        // Debe haber actualizado las 2 suscripciones
        verify(suscripcionRepository, times(2)).save(any(Suscripcion.class));
    }

    // TESTS DE IMPUESTOS (Moviéndolos aquí al centralizar la lógica)
    @Test
    void testCalculoImpuestos_ES() {
        BigDecimal base = new BigDecimal("100.00");
        BigDecimal impuesto = facturaService.calcularImpuesto("ES", base);

        // 21% de 100 = 21
        assertEquals(0, impuesto.compareTo(new BigDecimal("21.00")));
    }

    @Test
    void testCalculoImpuestos_Resto() {
        BigDecimal base = new BigDecimal("100.00");
        // USA o cualquier otro debe ser 0 según MVP
        BigDecimal impuesto = facturaService.calcularImpuesto("USA", base);

        assertEquals(0, impuesto.compareTo(BigDecimal.ZERO));
    }
}
