package com.telco.ventas.controller;

import com.telco.ventas.dto.response.ResumenResponse;
import com.telco.ventas.security.UsuarioPrincipal;
import com.telco.ventas.service.VentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final VentaService ventaService;

    /**
     * Acepta CUALQUIERA de las dos formas de filtrar que pide el enunciado:
     *  - por rango explicito:  ?desde=2026-08-01&hasta=2026-08-31
     *  - por periodo:          ?mes=2026-08   (dia a dia dentro de ese mes)
     * Si no se manda nada, resume el mes actual.
     */
    @GetMapping("/resumen")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<ResumenResponse> resumen(
            @AuthenticationPrincipal UsuarioPrincipal actual,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) YearMonth mes) {

        LocalDateTime[] rango = resolverRango(desde, hasta, mes);
        ResumenResponse resumen = ventaService.resumen(actual.getUsuario(), rango[0], rango[1]);
        return ResponseEntity.ok(resumen);
    }

    private LocalDateTime[] resolverRango(LocalDate desde, LocalDate hasta, YearMonth mes) {
        if (mes != null) {
            return new LocalDateTime[]{
                    mes.atDay(1).atStartOfDay(),
                    mes.atEndOfMonth().atTime(LocalTime.MAX)
            };
        }
        if (desde != null && hasta != null) {
            return new LocalDateTime[]{desde.atStartOfDay(), hasta.atTime(LocalTime.MAX)};
        }
        // Default: mes actual
        YearMonth actual = YearMonth.now();
        return new LocalDateTime[]{
                actual.atDay(1).atStartOfDay(),
                actual.atEndOfMonth().atTime(LocalTime.MAX)
        };
    }
}