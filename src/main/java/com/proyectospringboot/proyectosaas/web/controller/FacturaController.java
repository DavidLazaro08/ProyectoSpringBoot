package com.proyectospringboot.proyectosaas.web.controller;

import com.proyectospringboot.proyectosaas.domain.entity.Factura;
import com.proyectospringboot.proyectosaas.service.FacturaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
            @RequestParam(required = false) String mensaje,
            Model model) {

        model.addAttribute("email", email != null ? email : "");
        model.addAttribute("mensaje", mensaje);

        if (email != null && !email.isBlank()) {
            List<Factura> facturas = facturaService.buscarFacturasPorEmail(email);
            model.addAttribute("facturas", facturas);
        }

        return "facturas";
    }

    @PostMapping("/facturas/renovar")
    public String renovar(@RequestParam String email) {
        var resultado = facturaService.renovarSiToca(email);
        return "redirect:/facturas?email=" + email + "&mensaje=" + resultado.mensaje();
    }
}
