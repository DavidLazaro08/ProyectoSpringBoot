package com.proyectospringboot.proyectosaas.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/* HomeController:
 * Controlador básico que gestiona la ruta raíz de la aplicación.
 * Simplemente carga la vista principal (index). */

@Controller
public class HomeController {

    // =========================================================
    // HOME
    // =========================================================

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
