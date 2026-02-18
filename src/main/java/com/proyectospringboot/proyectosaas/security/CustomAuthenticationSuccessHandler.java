package com.proyectospringboot.proyectosaas.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/* CustomAuthenticationSuccessHandler:
 * Redirige al usuario según su rol después del login exitoso.
 * - ADMIN → /admin/auditoria
 * - USER → /dashboard */

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        // Verificamos si el usuario tiene rol ADMIN
        boolean esAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (esAdmin) {
            response.sendRedirect("/admin/auditoria");
        } else {
            response.sendRedirect("/dashboard");
        }
    }
}
