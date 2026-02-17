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

/* FacturaServiceTest
 *
 * Probamos la lógica de facturación sin arrancar Spring ni base de datos.
 * Como el servicio depende de repositorios, usamos Mockito para simularlos
 * y centrarnos únicamente en la lógica: renovación, generación masiva e impuestos.
 *
 * La idea es comprobar:
 * - Qué ocurre cuando toca renovar y cuando no.
 * - Que se crean facturas correctamente.
 * - Que el cálculo de impuestos funciona según país. */

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
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
    }

    @Test
    void renovarSiToca_sinSuscripcion_noGeneraFactura() {

        when(suscripcionRepository.buscarPorEmail("test@test.com"))
                .thenReturn(Optional.empty());

        RenovacionResultado resultado = facturaService.renovarSiToca("test@test.com");

        assertFalse(resultado.exito());
        assertEquals("No se encontró suscripción para este email", resultado.mensaje());
        verify(facturaRepository, never()).save(any(Factura.class));
    }

    @Test
    void renovarSiToca_noActiva_noGeneraFactura() {

        suscripcion.setEstado(EstadoSuscripcion.CANCELADA);

        when(suscripcionRepository.buscarPorEmail("test@test.com"))
                .thenReturn(Optional.of(suscripcion));

        RenovacionResultado resultado = facturaService.renovarSiToca("test@test.com");

        assertFalse(resultado.exito());
        assertTrue(resultado.mensaje().contains("no está activa"));
        verify(facturaRepository, never()).save(any(Factura.class));
    }

    @Test
    void renovarSiToca_noVencida_noGeneraFactura() {

        suscripcion.setFechaFinCiclo(LocalDateTime.now().plusDays(5));

        when(suscripcionRepository.buscarPorEmail("test@test.com"))
                .thenReturn(Optional.of(suscripcion));

        RenovacionResultado resultado = facturaService.renovarSiToca("test@test.com");

        assertFalse(resultado.exito());
        assertTrue(resultado.mensaje().contains("Aún no toca renovar"));
        verify(facturaRepository, never()).save(any(Factura.class));
    }

    @Test
    void renovarSiToca_vencida_creaFacturaYActualizaCiclo() {

        suscripcion.setFechaFinCiclo(LocalDateTime.now().minusDays(1));

        when(suscripcionRepository.buscarPorEmail("test@test.com"))
                .thenReturn(Optional.of(suscripcion));

        RenovacionResultado resultado = facturaService.renovarSiToca("test@test.com");

        assertTrue(resultado.exito());

        // Capturamos la factura generada
        ArgumentCaptor<Factura> facturaCaptor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(facturaCaptor.capture());

        Factura factura = facturaCaptor.getValue();
        assertEquals("Renovación Mensual", factura.getConcepto());

        // Usamos compareTo para evitar problemas de escala en BigDecimal
        assertEquals(0, factura.getTotal().compareTo(new BigDecimal("12.10")));

        verify(suscripcionRepository).save(suscripcion);
    }

    @Test
    void generarFacturasPendientes_creaFacturaPorCadaSuscripcionVencida() {

        Suscripcion s1 = new Suscripcion(new Usuario("u1@test.com", "ES"), plan);
        s1.setFechaFinCiclo(LocalDateTime.now().minusDays(1));

        Suscripcion s2 = new Suscripcion(new Usuario("u2@test.com", "FR"), plan);
        s2.setFechaFinCiclo(LocalDateTime.now().minusDays(2));

        when(suscripcionRepository.buscarVencidas(eq(EstadoSuscripcion.ACTIVA), any(LocalDateTime.class)))
                .thenReturn(List.of(s1, s2));

        int generadas = facturaService.generarFacturasPendientes();

        assertEquals(2, generadas);
        verify(facturaRepository, times(2)).save(any(Factura.class));
        verify(suscripcionRepository, times(2)).save(any(Suscripcion.class));
    }

    @Test
    void calcularImpuesto_es_aplicaIva21() {

        BigDecimal base = new BigDecimal("100.00");
        BigDecimal impuesto = facturaService.calcularImpuesto("ES", base);

        assertEquals(0, impuesto.compareTo(new BigDecimal("21.00")));
    }

    @Test
    void calcularImpuesto_otroPais_devuelveCero() {

        BigDecimal base = new BigDecimal("100.00");
        BigDecimal impuesto = facturaService.calcularImpuesto("USA", base);

        assertEquals(0, impuesto.compareTo(BigDecimal.ZERO));
    }
}
