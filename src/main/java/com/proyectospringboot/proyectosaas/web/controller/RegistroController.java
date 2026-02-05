package com.proyectospringboot.proyectosaas.web.controller;

import com.proyectospringboot.proyectosaas.domain.entity.Plan;
import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import com.proyectospringboot.proyectosaas.service.RegistroService;
import com.proyectospringboot.proyectosaas.repository.PlanRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* Controlador encargado del registro de usuarios
 * y de la creación inicial de su suscripción. */

@Controller
public class RegistroController {

    private final PlanRepository planRepository;
    private final RegistroService registroService;

    public RegistroController(PlanRepository planRepository, RegistroService registroService) {
        this.planRepository = planRepository;
        this.registroService = registroService;
    }

    @GetMapping("/register")
    public String showRegister(Model model) {
        List<Plan> planes = planRepository.findAll();
        model.addAttribute("planes", planes);
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(
            @RequestParam String email,
            @RequestParam String pais,
            @RequestParam String nombre,
            @RequestParam String apellidos,
            @RequestParam(required = false) String telefono,
            @RequestParam Long planId
    ) {
        Usuario usuario = registroService.registrar(email, pais, nombre, apellidos, telefono, planId);
        return "redirect:/dashboard/" + usuario.getId();
    }
    
    @GetMapping("/dashboard/{usuarioId}")
    public String dashboard(@PathVariable Long usuarioId, Model model) {
        var dto = registroService.getDashboard(usuarioId);
        model.addAttribute("dash", dto);
        return "dashboard";
    }
}
