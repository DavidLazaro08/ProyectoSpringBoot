package com.proyectospringboot.proyectosaas.web.controller;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.service.FacturaService;
import com.proyectospringboot.proyectosaas.service.SuscripcionService;
import com.proyectospringboot.proyectosaas.repository.PlanRepository;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import com.proyectospringboot.proyectosaas.repository.UsuarioRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final SuscripcionRepository suscripcionRepository;
    private final FacturaService facturaService;
    private final PlanRepository planRepository;
    private final SuscripcionService suscripcionService;
    private final UsuarioRepository usuarioRepository;

    public DashboardController(SuscripcionRepository suscripcionRepository,
            FacturaService facturaService,
            PlanRepository planRepository,
            SuscripcionService suscripcionService,
            UsuarioRepository usuarioRepository) {
        this.suscripcionRepository = suscripcionRepository;
        this.facturaService = facturaService;
        this.planRepository = planRepository;
        this.suscripcionService = suscripcionService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        // Obtener email del usuario autenticado
        String email = userDetails.getUsername();

        // Buscar suscripción (y usuario)
        var suscripcionOpt = suscripcionRepository.buscarPorEmail(email);
        if (suscripcionOpt.isEmpty()) {
            return "redirect:/?error=Usuario no encontrado";
        }

        Suscripcion suscripcion = suscripcionOpt.get();
        model.addAttribute("suscripcion", suscripcion);
        model.addAttribute("usuario", suscripcion.getUsuario());
        model.addAttribute("email", email);

        // Buscar última factura para mostrar
        List<Factura> facturas = facturaService.buscarFacturasPorEmail(email);
        if (!facturas.isEmpty()) {
            Factura ultima = facturas.get(0);
            model.addAttribute("ultimaFactura", ultima);
            model.addAttribute("ultimaFacturaPagada", facturaService.estaPagada(ultima.getId()));
        } else {
            model.addAttribute("ultimaFacturaPagada", true); // Si no hay facturas, no hay deuda
        }

        // Determinar método de pago "preferido" (en base al último usado)
        String metodoPago = facturaService.obtenerUltimoMetodoPago(email);
        model.addAttribute("metodoPago", metodoPago);

        // Método de pago preferido del usuario (guardado en BD)
        String metodoPagoPreferido = suscripcion.getUsuario().getMetodoPagoPreferido();
        model.addAttribute("metodoPagoPreferido", metodoPagoPreferido != null ? metodoPagoPreferido : "Tarjeta");

        // Estado de Pago Automático
        model.addAttribute("pagoAutomatico", suscripcion.getUsuario().isPagoAutomatico());

        // Cargar todos los planes disponibles para cambio de plan
        List<Plan> planes = planRepository.findAll();
        model.addAttribute("planes", planes);

        return "dashboard";
    }

    @PostMapping("/pago-automatico")
    public String activarPagoAutomatico(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "false") boolean activo,
            RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();

        try {
            var usuario = usuarioRepository.buscarPorEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            usuario.setPagoAutomatico(activo);
            usuarioRepository.save(usuario);

            String estado = activo ? "activado" : "desactivado";
            redirectAttributes.addFlashAttribute("mensaje", "Pago automático " + estado + " correctamente.");
            redirectAttributes.addFlashAttribute("tipoMensaje", "exito");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error al cambiar configuración: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "error");
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/pago")
    public String guardarPagoDemo(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String tipoPago,
            // Campos tarjeta
            @RequestParam(required = false) String ultimos4,
            @RequestParam(required = false) String titular,
            // Campo PayPal
            @RequestParam(required = false) String emailPaypal,
            // Campos transferencia
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) String referencia,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();

        try {
            boolean hayFacturaPendiente = facturaService.tieneFacturaPendiente(email);

            facturaService.registrarPagoConDatos(email, tipoPago,
                    ultimos4, titular, emailPaypal, iban, referencia);

            if (hayFacturaPendiente) {
                redirectAttributes.addFlashAttribute("mensaje",
                        "Método de pago registrado: " + tipoPago);
            } else {
                redirectAttributes.addFlashAttribute("mensaje",
                        "Método de pago " + tipoPago + " guardado para tu próxima factura");
            }
            redirectAttributes.addFlashAttribute("tipoMensaje", "exito");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensaje", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "error");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error registrando pago: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "error");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/cambiar-plan")
    public String cambiarPlan(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long planId,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();

        try {
            var usuario = usuarioRepository.buscarPorEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            suscripcionService.cambiarPlan(usuario.getId(), planId);

            redirectAttributes.addFlashAttribute("mensaje",
                    "Plan cambiado correctamente. Si hubo diferencia de precio, se generó una factura de ajuste.");
            redirectAttributes.addFlashAttribute("tipoMensaje", "exito");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensaje", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "warning");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error al cambiar plan: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "error");
        }
        return "redirect:/dashboard";
    }
}
