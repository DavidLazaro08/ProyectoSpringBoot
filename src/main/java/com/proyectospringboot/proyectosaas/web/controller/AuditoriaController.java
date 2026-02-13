package com.proyectospringboot.proyectosaas.web.controller;

import com.proyectospringboot.proyectosaas.service.AuditoriaService;
import com.proyectospringboot.proyectosaas.service.FacturaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/* AuditoriaController:
 * Panel de administración sencillo.
 * Permite:
 *  - Acceso mediante clave (admin / admin123).
 *  - Visualizar historial de cambios (Envers).
 *  - Ejecutar migración de facturas antiguas si fuese necesario. */

@Controller
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final FacturaService facturaService;

    public AuditoriaController(AuditoriaService auditoriaService,
                               FacturaService facturaService) {
        this.auditoriaService = auditoriaService;
        this.facturaService = facturaService;
    }

    // =========================================================
    // LOGIN ADMIN
    // =========================================================

    @GetMapping("/admin")
    public String loginAdmin(@RequestParam(required = false) String error,
                             Model model) {

        if (error != null) {
            model.addAttribute("error", error);
        }

        return "auditoria-login";
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin")
    public String procesarLoginAdmin(@RequestParam String key) {

        // Claves válidas (admin / admin123)
        if ("admin".equals(key) || "admin123".equalsIgnoreCase(key)) {
            return "redirect:/admin/auditoria?key=" + key;
        }

        return "redirect:/admin?error=Clave incorrecta";
    }

    // =========================================================
    // PANEL DE AUDITORÍA
    // =========================================================

    @GetMapping("/admin/auditoria")
    public String mostrarAuditoria(@RequestParam(required = false) String key,
                                   Model model) {

        // Protección básica: si no hay clave o no es válida, vuelta al login
        if (key == null ||
                (!key.equals("admin") && !key.equalsIgnoreCase("admin123"))) {
            return "redirect:/admin";
        }

        try {

            // Migración puntual de facturas antiguas
            try {
                int facturasActualizadas = facturaService.migrarFacturasAntiguas();

                if (facturasActualizadas > 0) {
                    model.addAttribute("mensaje",
                            "Se actualizaron " + facturasActualizadas + " facturas antiguas con impuestos.");
                }

            } catch (Exception e) {
                // No bloqueamos el panel si falla
                model.addAttribute("error",
                        "Error al migrar facturas antiguas: " + e.getMessage());
            }

            List<AuditoriaService.RevisionDTO> revisiones =
                    auditoriaService.obtenerHistorialCambios();

            model.addAttribute("revisiones", revisiones);

        } catch (Exception e) {

            model.addAttribute("error",
                    "Error cargando auditoría: " + e.getMessage());

            model.addAttribute("revisiones", List.of());
        }

        return "auditoria";
    }
}
