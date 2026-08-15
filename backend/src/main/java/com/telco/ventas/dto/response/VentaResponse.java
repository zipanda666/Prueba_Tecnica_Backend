package com.telco.ventas.dto.response;

import com.telco.ventas.entity.Venta;
import com.telco.ventas.enums.EstadoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VentaResponse(
        Long id,
        Long agenteId,
        String agenteUsername,
        String dniCliente,
        String nombreCliente,
        String telefonoCliente,
        String direccionCliente,
        String planActual,
        String planNuevo,
        String codigoLlamada,
        String producto,
        BigDecimal monto,
        EstadoVenta estado,
        String motivoRechazo,
        Long validadoPorId,
        String validadoPorUsername,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaValidacion
) {
    public static VentaResponse from(Venta v) {
        return new VentaResponse(
                v.getId(),
                v.getAgente().getId(),
                v.getAgente().getUsername(),
                v.getDniCliente(),
                v.getNombreCliente(),
                v.getTelefonoCliente(),
                v.getDireccionCliente(),
                v.getPlanActual(),
                v.getPlanNuevo(),
                v.getCodigoLlamada(),
                v.getProducto(),
                v.getMonto(),
                v.getEstado(),
                v.getMotivoRechazo(),
                v.getValidadoPor() != null ? v.getValidadoPor().getId() : null,
                v.getValidadoPor() != null ? v.getValidadoPor().getUsername() : null,
                v.getFechaRegistro(),
                v.getFechaValidacion()
        );
    }
}