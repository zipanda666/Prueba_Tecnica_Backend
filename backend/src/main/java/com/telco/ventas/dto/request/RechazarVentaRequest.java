package com.telco.ventas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarVentaRequest(

        @NotBlank(message = "motivo_rechazo es obligatorio para rechazar una venta")
        @Size(max = 255, message = "motivo_rechazo no puede superar 255 caracteres")
        String motivoRechazo
) {
}