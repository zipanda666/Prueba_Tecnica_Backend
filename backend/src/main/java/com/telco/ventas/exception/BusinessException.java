package com.telco.ventas.exception;

/**
 * Para violaciones de reglas de negocio que no son errores de validacion de campos:
 * codigo_llamada duplicado, intentar aprobar una venta que no esta PENDIENTE, etc.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}