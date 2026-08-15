package com.telco.ventas.dto.request;

import com.telco.ventas.enums.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(

        @NotBlank(message = "username es obligatorio")
        @Size(max = 50, message = "username no puede superar 50 caracteres")
        String username,

        @NotBlank(message = "password es obligatorio")
        @Size(min = 6, message = "password debe tener al menos 6 caracteres")
        String password,

        @NotNull(message = "rol es obligatorio")
        RolUsuario rol,

        // Solo aplica/tiene sentido cuando rol = AGENTE. Opcional para los demas roles.
        Long supervisorId
) {
}