package com.proyectospringboot.proyectosaas.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/* SecurityConfig:
 * Configuración de Spring Security.
 * - Define qué rutas son públicas y cuáles requieren autenticación.
 * - Configura el login y logout.
 * - Asigna roles a rutas específicas (USER, ADMIN). */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas (sin login)
                        .requestMatchers("/", "/registro", "/css/**", "/js/**", "/error").permitAll()

                        // Rutas de usuario normal (requieren USER o ADMIN)
                        .requestMatchers("/dashboard/**").hasAnyRole("USER", "ADMIN")

                        // Rutas de administración (solo ADMIN)
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Cualquier otra ruta requiere autenticación
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(successHandler) // Redirige según rol
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
