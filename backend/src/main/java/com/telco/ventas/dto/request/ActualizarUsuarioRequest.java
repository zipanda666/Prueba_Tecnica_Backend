package com.telco.ventas.dto.request;

import com.telco.ventas.enums.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(

        @NotBlank(message = "username es obligatorio")
        @Size(max = 50, message = "username no puede superar 50 caracteres")
        String username,

        @NotNull(message = "rol es obligatorio")
        RolUsuario rol,

        // Solo aplica cuando rol = AGENTE. Enviar null para quitar la asignacion.
        Long supervisorId
) {
}