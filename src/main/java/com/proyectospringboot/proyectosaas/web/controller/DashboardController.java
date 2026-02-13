package com.proyectospringboot.proyectosaas.web.controller;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.service.FacturaService;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
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

    public DashboardController(SuscripcionRepository suscripcionRepository, FacturaService facturaService) {
        this.suscripcionRepository = suscripcionRepository;
        this.facturaService = facturaService;
    }

    @GetMapping
    public String dashboard(@RequestParam(required = false) String email, Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:/";
        }

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

        return "dashboard";
    }

    @PostMapping("/acceder")
    public String acceder(@RequestParam String email, RedirectAttributes redirectAttributes) {
        // Redirige al dashboard validando email
        var suscripcionOpt = suscripcionRepository.buscarPorEmail(email);
        if (suscripcionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El email introductido no existe en nuestra base de datos.");
            return "redirect:/";
        }
        return "redirect:/dashboard?email=" + email;
    }

    @PostMapping("/pago")
    public String guardarPagoDemo(@RequestParam String email,
            @RequestParam String tipoPago,
            RedirectAttributes redirectAttributes) {
        try {
            facturaService.registrarPagoPrueba(email, tipoPago);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Método de pago registrado (Demo: " + tipoPago + ") y vinculado a tu próxima factura.");
            redirectAttributes.addFlashAttribute("tipoMensaje", "exito");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error registrando pago: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "error");
        }
        return "redirect:/dashboard?email=" + email;
    }
}
