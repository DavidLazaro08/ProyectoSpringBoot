package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.domain.entity.Perfil;
import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import com.proyectospringboot.proyectosaas.repository.FacturaRepository;
import com.proyectospringboot.proyectosaas.repository.PerfilRepository;
import com.proyectospringboot.proyectosaas.repository.PlanRepository;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import com.proyectospringboot.proyectosaas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/* RegistroServiceTest
 *
 * Probamos la lógica del alta completa de usuario sin arrancar Spring ni base de datos.
 * El servicio depende de varios repositorios y de FacturaService, así que usamos Mockito
 * para simular esas dependencias y centrarnos únicamente en la lógica.
 *
 * Comprobamos:
 * - Que si el email es nuevo, se crean Usuario + Perfil + Suscripción + Factura.
 * - Que si el email ya existe, se lanza excepción y no se guarda nada.
 */

@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private SuscripcionRepository suscripcionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private FacturaService facturaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistroService registroService;

    @Test
    void registrar_emailNuevo_creaUsuarioPerfilSuscripcionYFactura() {

        String email = "nuevo@test.com";
        String pais = "ES";
        Long planId = 1L;

        Plan plan = new Plan("BASIC", new BigDecimal("10.00"));

        when(usuarioRepository.buscarPorEmail(email)).thenReturn(Optional.empty());
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(facturaService.calcularImpuesto(eq(pais), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("2.10"));

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArguments()[0]);
        when(perfilRepository.save(any(Perfil.class))).thenAnswer(i -> i.getArguments()[0]);
        when(suscripcionRepository.save(any(Suscripcion.class))).thenAnswer(i -> i.getArguments()[0]);
        when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$10$hashedPassword");

        Usuario resultado = registroService.registrar(email, pais, "Nom", "Ape", "600", planId, "password123");

        assertNotNull(resultado);
        assertEquals(email, resultado.getEmail());

        verify(usuarioRepository).save(any(Usuario.class));
        verify(perfilRepository).save(any(Perfil.class));
        verify(suscripcionRepository).save(any(Suscripcion.class));

        ArgumentCaptor<Factura> facturaCaptor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(facturaCaptor.capture());

        Factura facturaGuardada = facturaCaptor.getValue();
        assertEquals("Alta de Suscripción", facturaGuardada.getConcepto());

        BigDecimal totalEsperado = new BigDecimal("12.10");
        assertEquals(0, facturaGuardada.getTotal().compareTo(totalEsperado));

        // Verificar que se hasheó la contraseña
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void registrar_emailDuplicado_lanzaExcepcionYNoGuardaNada() {

        String email = "yaexiste@test.com";

        Usuario usuarioMock = mock(Usuario.class);
        when(usuarioRepository.buscarPorEmail(email)).thenReturn(Optional.of(usuarioMock));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> registroService.registrar(email, "ES", "Nom", "Ape", "600", 1L, "password123"));

        assertEquals("Ya existe un usuario con ese email.", ex.getMessage());

        verify(usuarioRepository, never()).save(any());
        verify(perfilRepository, never()).save(any());
        verify(suscripcionRepository, never()).save(any());
        verify(facturaRepository, never()).save(any());
    }
}
