package com.gascorocora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Solicitud de conexion de un usuario potencial (mismas llaves que formSolicitudConexion). */
public record SolicitudConexionRequest(
        @NotBlank String nombreSolicitante,
        @NotBlank String documentoIdentidad,
        @NotBlank @Email String emailSolicitante,
        @NotBlank String telefonoSolicitante,
        @NotBlank String direccionPredio,
        @NotBlank String municipio,
        @NotBlank @Pattern(regexp = "RESIDENCIAL|COMERCIAL|INDUSTRIAL") String tipoPredio,
        @NotNull @Min(1) @Max(6) Integer estrato) {
}
