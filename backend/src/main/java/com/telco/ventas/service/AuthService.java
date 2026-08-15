package com.telco.ventas.service;

import com.telco.ventas.dto.request.LoginRequest;
import com.telco.ventas.dto.response.LoginResponse;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.repository.UsuarioRepository;
import com.telco.ventas.security.JwtService;
import com.telco.ventas.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger AUDIT_AUTH = LoggerFactory.getLogger("AUDIT.AUTH");

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (BadCredentialsException | DisabledException e) {
            AUDIT_AUTH.info("usuario={} accion=LOGIN resultado=FALLIDO motivo={}",
                    request.username(), e.getClass().getSimpleName());
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        // Si authenticate() no lanzo excepcion, las credenciales son validas
        Usuario usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        String token = jwtService.generarToken(new UsuarioPrincipal(usuario));

        AUDIT_AUTH.info("usuario={} rol={} accion=LOGIN resultado=OK", usuario.getUsername(), usuario.getRol());

        return LoginResponse.of(token, usuario.getUsername(), usuario.getRol(), jwtService.getExpirationMs());
    }
}