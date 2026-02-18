package com.proyectospringboot.proyectosaas.web.controller;

import com.proyectospringboot.proyectosaas.service.AuditoriaService;
import com.proyectospringboot.proyectosaas.service.FacturaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/* AuditoriaController:
 * Panel de administración protegido por Spring Security (solo ROLE_ADMIN).
 * Permite:
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
    // PANEL DE AUDITORÍA (protegido por Spring Security)
    // =========================================================

    @GetMapping("/admin/auditoria")
    public String mostrarAuditoria(Model model) {

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

            List<AuditoriaService.RevisionDTO> revisiones = auditoriaService.obtenerHistorialCambios();

            model.addAttribute("revisiones", revisiones);

        } catch (Exception e) {

            model.addAttribute("error",
                    "Error cargando auditoría: " + e.getMessage());

            model.addAttribute("revisiones", List.of());
        }

        return "auditoria";
    }
}
