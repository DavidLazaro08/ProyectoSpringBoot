package com.proyectospringboot.proyectosaas.web.controller;

import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import com.proyectospringboot.proyectosaas.service.RegistroService;
import com.proyectospringboot.proyectosaas.repository.PlanRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* RegistroController:
 * Gestiona el alta de nuevos usuarios en la plataforma.
 * - Muestra los planes disponibles.
 * - Procesa el formulario de registro.
 * - Delegamos la lógica real en RegistroService. */

@Controller
public class RegistroController {

    private final PlanRepository planRepository;
    private final RegistroService registroService;

    public RegistroController(PlanRepository planRepository,
                              RegistroService registroService) {
        this.planRepository = planRepository;
        this.registroService = registroService;
    }

    // =========================================================
    // FORMULARIO DE REGISTRO
    // =========================================================

    @GetMapping("/registro")
    public String mostrarFormulario(Model model) {

        List<Plan> planes = planRepository.findAll();
        model.addAttribute("planes", planes);

        return "register";
    }

    // =========================================================
    // PROCESAR REGISTRO
    // =========================================================

    @PostMapping("/registro")
    public String procesarRegistro(
            @RequestParam String email,
            @RequestParam String pais,
            @RequestParam String nombre,
            @RequestParam String apellidos,
            @RequestParam(required = false) String telefono,
            @RequestParam Long planId) {

        Usuario usuario = registroService
                .registrar(email, pais, nombre, apellidos, telefono, planId);

        return "redirect:/dashboard?email=" + usuario.getEmail();
    }
}
