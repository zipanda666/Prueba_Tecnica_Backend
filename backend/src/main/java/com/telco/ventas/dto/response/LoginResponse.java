package com.telco.ventas.dto.response;

import com.telco.ventas.enums.RolUsuario;

public record LoginResponse(
        String token,
        String tokenType,
        String username,
        RolUsuario rol,
        long expiresInMs
) {
    public static LoginResponse of(String token, String username, RolUsuario rol, long expiresInMs) {
        return new LoginResponse(token, "Bearer", username, rol, expiresInMs);
    }
}