package com.proyectospringboot.proyectosaas.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/* LoginController:
 * Gestiona la vista de login.
 * Spring Security maneja la autenticación real, este controlador solo muestra la vista. */

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
            @RequestParam(required = false) String registered,
            Model model) {

        if (error != null) {
            model.addAttribute("error", "Email o contraseña incorrectos");
        }

        if (registered != null) {
            model.addAttribute("success", "Registro exitoso. Ya puedes iniciar sesión.");
        }

        return "login";
    }
}
