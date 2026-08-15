package com.telco.ventas.controller;

import com.telco.ventas.dto.request.RechazarVentaRequest;
import com.telco.ventas.dto.request.VentaCreateRequest;
import com.telco.ventas.dto.response.VentaResponse;
import com.telco.ventas.enums.EstadoVenta;
import com.telco.ventas.security.UsuarioPrincipal;
import com.telco.ventas.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * El VentaService ya devuelve VentaResponse (no la entidad Venta) - la conversion pasa
 * DENTRO del Service, mientras la transaccion/sesion de Hibernate sigue abierta, para poder
 * resolver las relaciones lazy (agente, validadoPor) sin LazyInitializationException.
 * Este Controller solo traduce HTTP <-> Service, no toca entidades JPA en ningun momento.
 */
@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    @PreAuthorize("hasRole('AGENTE')")
    public ResponseEntity<VentaResponse> crear(
            @Valid @RequestBody VentaCreateRequest request,
            @AuthenticationPrincipal UsuarioPrincipal actual) {

        VentaResponse creada = ventaService.crear(request, actual.getUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/mis-ventas")
    @PreAuthorize("hasRole('AGENTE')")
    public ResponseEntity<Page<VentaResponse>> misVentas(
            @AuthenticationPrincipal UsuarioPrincipal actual,
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fechaRegistro") Pageable pageable) {

        Page<VentaResponse> resultado = ventaService
                .misVentas(actual.getUsuario(), estado, inicioDelDia(desde), finDelDia(hasta), pageable);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('BACKOFFICE')")
    public ResponseEntity<Page<VentaResponse>> pendientes(
            @PageableDefault(size = 20, sort = "fechaRegistro") Pageable pageable) {

        Page<VentaResponse> resultado = ventaService.pendientes(pageable);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('BACKOFFICE')")
    public ResponseEntity<VentaResponse> aprobar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal actual) {

        VentaResponse actualizada = ventaService.aprobar(id, actual.getUsuario());
        return ResponseEntity.ok(actualizada);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('BACKOFFICE')")
    public ResponseEntity<VentaResponse> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody RechazarVentaRequest request,
            @AuthenticationPrincipal UsuarioPrincipal actual) {

        VentaResponse actualizada = ventaService.rechazar(id, request.motivoRechazo(), actual.getUsuario());
        return ResponseEntity.ok(actualizada);
    }

    @GetMapping("/equipo")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<Page<VentaResponse>> equipo(
            @AuthenticationPrincipal UsuarioPrincipal actual,
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) Long agenteId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fechaRegistro") Pageable pageable) {

        Page<VentaResponse> resultado = ventaService
                .equipo(actual.getUsuario(), estado, agenteId, inicioDelDia(desde), finDelDia(hasta), pageable);
        return ResponseEntity.ok(resultado);
    }

    private LocalDateTime inicioDelDia(LocalDate fecha) {
        return fecha != null ? fecha.atStartOfDay() : null;
    }

    private LocalDateTime finDelDia(LocalDate fecha) {
        return fecha != null ? fecha.atTime(LocalTime.MAX) : null;
    }
}