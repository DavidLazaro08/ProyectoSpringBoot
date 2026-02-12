package com.proyectospringboot.proyectosaas.web.controller;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.service.FacturaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            Model model) {

        model.addAttribute("email", email != null ? email : "");

        if (email != null && !email.isBlank()) {
            List<Factura> facturas = facturaService.buscarFacturasPorEmail(email);
            model.addAttribute("facturas", facturas);
        }

        return "facturas";
    }

    @PostMapping("/facturas/renovar")
    public String renovar(@RequestParam String email, RedirectAttributes redirectAttributes) {
        var resultado = facturaService.renovarSiToca(email);

        redirectAttributes.addFlashAttribute("mensaje", resultado.mensaje());
        redirectAttributes.addFlashAttribute("tipoMensaje", resultado.exito() ? "exito" : "error");

        return "redirect:/facturas?email=" + email;
    }
}
