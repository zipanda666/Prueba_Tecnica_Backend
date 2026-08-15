package com.telco.ventas.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ResumenResponse(
        // conteos por estado, ej: {"PENDIENTE": 3, "APROBADA": 5, "RECHAZADA": 2}
        Map<String, Long> conteosPorEstado,
        BigDecimal montoTotalAprobadas,
        List<VentasPorDiaItem> ventasPorDia
) {
    public record VentasPorDiaItem(
            LocalDate fecha,
            Long cantidad,
            BigDecimal monto
    ) {
    }
}