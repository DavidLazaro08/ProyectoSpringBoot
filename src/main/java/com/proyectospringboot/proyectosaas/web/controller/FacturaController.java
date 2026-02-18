package com.proyectospringboot.proyectosaas.web.controller;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.service.FacturaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @GetMapping("/facturas")
    public String mostrarFacturas(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime fechaFin,
            @RequestParam(required = false) BigDecimal totalMin,
            @RequestParam(required = false) BigDecimal totalMax,
            Model model) {

        model.addAttribute("email", email != null ? email : "");
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("totalMin", totalMin);
        model.addAttribute("totalMax", totalMax);

        if (email != null && !email.isBlank()) {
            List<Factura> facturas;

            // Si hay filtros, usar búsqueda con filtros
            if (fechaInicio != null || fechaFin != null || totalMin != null || totalMax != null) {
                facturas = facturaService.buscarFacturasConFiltros(email, fechaInicio, fechaFin, totalMin, totalMax);
            } else {
                // Sin filtros, usar búsqueda simple
                facturas = facturaService.buscarFacturasPorEmail(email);
            }

            model.addAttribute("facturas", facturas);

            // Validar si usuario existe (si no hay facturas, quizás usuario tampoco)
            if (facturas.isEmpty()) {
                try {
                    // Intento renovación "fake" solo para chequear usuario, o mejor:
                    // Si FacturaService lanza excepcion al buscar usuario, capturarlo.
                    // Pero buscarFacturasPorEmail devuelve lista vacía si no hay.
                    // Podríamos verificar si existe suscripción para ese email.
                } catch (Exception e) {
                    model.addAttribute("mensaje", "No se encontró usuario con ese email.");
                    model.addAttribute("tipoMensaje", "error");
                }
            }
        }

        return "facturas";
    }

    @PostMapping("/facturas/renovar")
    public String renovar(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            var resultado = facturaService.renovarSiToca(email);

            redirectAttributes.addFlashAttribute("mensaje", resultado.mensaje());
            redirectAttributes.addFlashAttribute("tipoMensaje", resultado.exito() ? "exito" : "error"); // o warning

            // Si nos dice que no toca renovar, lo marcamos como warning para estilos
            // (amarillo)
            if (!resultado.exito() && resultado.mensaje().contains("Aún no toca")) {
                redirectAttributes.addFlashAttribute("tipoMensaje", "warning");
            }

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensaje", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "error");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error inesperado al renovar: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "error");
        }

        return "redirect:/dashboard";
    }

    /**
     * Vista de facturas para usuarios normales (solo sus propias facturas)
     */
    @GetMapping("/mis-facturas")
    public String misFacturas(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) LocalDateTime fechaInicio,
            @RequestParam(required = false) LocalDateTime fechaFin,
            @RequestParam(required = false) BigDecimal totalMin,
            @RequestParam(required = false) BigDecimal totalMax,
            Model model) {

        String email = userDetails.getUsername();

        // Buscar facturas del usuario autenticado con filtros opcionales
        List<Factura> facturas = facturaService.buscarFacturasConFiltros(email, fechaInicio, fechaFin, totalMin,
                totalMax);

        model.addAttribute("facturas", facturas);
        model.addAttribute("email", email);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("totalMin", totalMin);
        model.addAttribute("totalMax", totalMax);

        return "mis-facturas";
    }

}
