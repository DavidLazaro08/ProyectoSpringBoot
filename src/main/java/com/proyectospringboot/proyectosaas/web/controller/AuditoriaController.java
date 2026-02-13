package com.proyectospringboot.proyectosaas.web.controller;

import com.proyectospringboot.proyectosaas.service.AuditoriaService;
import com.proyectospringboot.proyectosaas.service.FacturaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final FacturaService facturaService;

    public AuditoriaController(AuditoriaService auditoriaService, FacturaService facturaService) {
        this.auditoriaService = auditoriaService;
        this.facturaService = facturaService;
    }

    // Pantalla de login (GET /admin)
    @GetMapping("/admin")
    public String loginAdmin(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "auditoria-login";
    }

    // Procesar login (POST /admin)
    @org.springframework.web.bind.annotation.PostMapping("/admin")
    public String procesarLoginAdmin(@RequestParam String key) {
        if ("admin".equals(key) || "admin123".equalsIgnoreCase(key)) {
            return "redirect:/admin/auditoria?key=" + key;
        }
        return "redirect:/admin?error=Clave incorrecta";
    }

    @GetMapping("/admin/auditoria")
    public String mostrarAuditoria(@RequestParam(required = false) String key, Model model) {
        // Claves válidas (MVP: admin o admin123)
        // PROTECCIÓN: Si no hay clave o es inválida, redirigir al login (/admin)
        if (key == null || (!key.equals("admin") && !key.equalsIgnoreCase("admin123"))) {
            return "redirect:/admin";
        }

        try {
            // Migrar facturas antiguas si es necesario (solo la primera vez)
            try {
                int facturasActualizadas = facturaService.migrarFacturasAntiguas();
                if (facturasActualizadas > 0) {
                    model.addAttribute("mensaje",
                            "Se actualizaron " + facturasActualizadas + " facturas antiguas con impuestos.");
                }
            } catch (Exception e) {
                // Si falla la migración, la ignoramos para no bloquear el panel
                model.addAttribute("error", "Error al migrar facturas antiguas: " + e.getMessage());
            }

            List<AuditoriaService.RevisionDTO> revisiones = auditoriaService.obtenerHistorialCambios();
            model.addAttribute("revisiones", revisiones);

        } catch (Exception e) {
            model.addAttribute("error", "Error cargando auditoría: " + e.getMessage());
            model.addAttribute("revisiones", List.of());
        }

        return "auditoria";
    }
}
