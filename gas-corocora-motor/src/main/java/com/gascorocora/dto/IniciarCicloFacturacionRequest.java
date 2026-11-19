package com.gascorocora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/** Datos para iniciar el ciclo de facturacion de una cuenta (mismas llaves que formIniciarCicloFacturacion). */
public record IniciarCicloFacturacionRequest(
        @NotBlank String numeroCuenta,
        @NotBlank String nombreUsuario,
        @NotBlank @Email String emailUsuario,
        @NotBlank @Pattern(regexp = "RESIDENCIAL|COMERCIAL|INDUSTRIAL") String tipoUsuario,
        @NotNull @Min(1) @Max(6) Integer estrato,
        @NotNull @PositiveOrZero Double consumoPromedioM3,
        @NotNull @PositiveOrZero Double lecturaAnterior) {
}
