package com.telco.ventas.dto.request;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
        @NotNull(message = "activo es obligatorio")
        Boolean activo
) {
}