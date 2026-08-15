package com.telco.ventas.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Lo que envia el AGENTE al crear una venta.
 * No incluye agenteId (se toma del usuario autenticado) ni estado (siempre nace PENDIENTE).
 */
public record VentaCreateRequest(

        @NotBlank(message = "dni_cliente es obligatorio")
        @Pattern(regexp = "^\\d{8}$|^\\d{11}$", message = "dni_cliente debe tener 8 digitos (DNI) u 11 digitos (RUC)")
        String dniCliente,

        @NotBlank(message = "nombre_cliente es obligatorio")
        String nombreCliente,

        @NotBlank(message = "telefono_cliente es obligatorio")
        @Pattern(regexp = "^\\d{9}$", message = "telefono_cliente debe tener 9 digitos")
        String telefonoCliente,

        @NotBlank(message = "direccion_cliente es obligatoria")
        String direccionCliente,

        String planActual,

        @NotBlank(message = "plan_nuevo es obligatorio")
        String planNuevo,

        @NotBlank(message = "codigo_llamada es obligatorio")
        String codigoLlamada,

        String producto,

        @NotNull(message = "monto es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "monto no puede ser negativo")
        BigDecimal monto
) {
}