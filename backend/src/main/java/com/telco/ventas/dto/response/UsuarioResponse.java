package com.telco.ventas.dto.response;

import com.telco.ventas.entity.Usuario;
import com.telco.ventas.enums.RolUsuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String username,
        RolUsuario rol,
        Long supervisorId,
        String supervisorUsername,
        Boolean activo,
        LocalDateTime createdAt
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getUsername(),
                u.getRol(),
                u.getSupervisor() != null ? u.getSupervisor().getId() : null,
                u.getSupervisor() != null ? u.getSupervisor().getUsername() : null,
                u.getActivo(),
                u.getCreatedAt()
        );
    }
}