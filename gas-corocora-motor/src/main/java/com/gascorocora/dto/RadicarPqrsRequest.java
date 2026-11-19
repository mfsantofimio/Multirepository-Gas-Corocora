package com.gascorocora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/** Datos para radicar una PQRS (mismas llaves que formRadicarPqrs). */
public record RadicarPqrsRequest(
        @NotBlank String numeroCuenta,
        @NotBlank String nombreUsuario,
        @NotBlank @Email String emailUsuario,
        @NotBlank @Pattern(regexp = "PETICION|QUEJA|RECLAMO|SUGERENCIA") String tipoSolicitud,
        @NotBlank @Pattern(regexp = "PRESENCIAL|TELEFONICO|WEB|ESCRITO") String canal,
        @PositiveOrZero Double valorReclamado,
        @NotBlank String descripcion) {
}
