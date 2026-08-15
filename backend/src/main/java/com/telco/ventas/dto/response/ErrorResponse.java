package com.telco.ventas.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato consistente de error para toda la API:
 * { "timestamp": ..., "path": ..., "error": ..., "message": ... }
 * "details" es opcional, se llena solo en errores de validacion de campos (400).
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        String path,
        String error,
        String message,
        List<String> details
) {
    public ErrorResponse(String path, String error, String message) {
        this(LocalDateTime.now(), path, error, message, null);
    }

    public ErrorResponse(String path, String error, String message, List<String> details) {
        this(LocalDateTime.now(), path, error, message, details);
    }
}