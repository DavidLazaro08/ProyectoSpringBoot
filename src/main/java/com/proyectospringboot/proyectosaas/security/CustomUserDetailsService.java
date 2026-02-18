package com.proyectospringboot.proyectosaas.security;

import com.proyectospringboot.proyectosaas.domain.entity.Usuario;
import com.proyectospringboot.proyectosaas.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/* CustomUserDetailsService:
 * Implementación de UserDetailsService para Spring Security.
 * Carga usuarios desde la BD y los convierte al formato que Security necesita. */

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        // Convertimos el rol del usuario a formato Spring Security (ROLE_USER,
        // ROLE_ADMIN)
        String rolConPrefijo = "ROLE_" + usuario.getRol().name();

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(rolConPrefijo)))
                .build();
    }
}
