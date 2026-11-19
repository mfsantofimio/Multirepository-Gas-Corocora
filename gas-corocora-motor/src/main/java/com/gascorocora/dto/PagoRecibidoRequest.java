package com.gascorocora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Confirmacion de pago enviada por la entidad recaudadora (mensaje MensajePagoRecibido). */
public record PagoRecibidoRequest(
        @NotNull @Positive Double valorPagado,
        @NotBlank String medioPago) {
}
