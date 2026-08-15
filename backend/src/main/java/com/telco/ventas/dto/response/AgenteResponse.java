package com.telco.ventas.dto.response;

import com.telco.ventas.entity.Usuario;

public record AgenteResponse(
        Long id,
        String username
) {
    public static AgenteResponse from(Usuario u) {
        return new AgenteResponse(u.getId(), u.getUsername());
    }
}