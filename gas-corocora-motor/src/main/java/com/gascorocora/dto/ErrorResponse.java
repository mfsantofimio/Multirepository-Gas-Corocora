package com.gascorocora.dto;

import java.time.LocalDateTime;

/** Formato uniforme de error para la API. */
public record ErrorResponse(
        int estado,
        String error,
        String detalle,
        LocalDateTime fecha) {

    public static ErrorResponse of(int estado, String error, String detalle) {
        return new ErrorResponse(estado, error, detalle, LocalDateTime.now());
    }
}
