package com.telco.ventas.controller;

import com.telco.ventas.dto.request.ActualizarUsuarioRequest;
import com.telco.ventas.dto.request.CambiarEstadoRequest;
import com.telco.ventas.dto.request.CambiarPasswordRequest;
import com.telco.ventas.dto.request.CrearUsuarioRequest;
import com.telco.ventas.dto.response.AgenteResponse;
import com.telco.ventas.dto.response.UsuarioResponse;
import com.telco.ventas.security.UsuarioPrincipal;
import com.telco.ventas.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /** Lista los agentes que reportan al supervisor autenticado - se usa para poblar el filtro de "equipo" sin adivinar IDs. */
    @GetMapping("/mi-equipo")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<List<AgenteResponse>> miEquipo(@AuthenticationPrincipal UsuarioPrincipal actual) {
        return ResponseEntity.ok(usuarioService.listarAgentesDeSupervisor(actual.getUsuario().getId()));
    }

    /** Panel de administracion: lista todos los usuarios del sistema. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    /** Panel de administracion: crea un nuevo usuario (cualquier rol). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> crear(
            @Valid @RequestBody CrearUsuarioRequest request,
            @AuthenticationPrincipal UsuarioPrincipal actual) {

        UsuarioResponse creado = usuarioService.crear(request, actual.getUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /** Panel de administracion: edita username, rol y supervisor de un usuario existente. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request,
            @AuthenticationPrincipal UsuarioPrincipal actual) {

        return ResponseEntity.ok(usuarioService.actualizar(id, request, actual.getUsuario()));
    }

    /** Panel de administracion: activa o desactiva un usuario. Un admin no puede desactivarse a si mismo. */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal actual) {

        return ResponseEntity.ok(usuarioService.cambiarEstado(id, request.activo(), actual.getUsuario()));
    }

    /** Panel de administracion: resetea la contraseña de un usuario (sin pedir la anterior, es un reset de ADMIN). */
    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable Long id,
            @Valid @RequestBody CambiarPasswordRequest request,
            @AuthenticationPrincipal UsuarioPrincipal actual) {

        usuarioService.cambiarPassword(id, request.password(), actual.getUsuario());
        return ResponseEntity.noContent().build();
    }
}