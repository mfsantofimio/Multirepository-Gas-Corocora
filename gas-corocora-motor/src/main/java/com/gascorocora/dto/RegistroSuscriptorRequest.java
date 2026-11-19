package com.gascorocora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Registro de un usuario externo en el portal (crea la fila en GC_SUSCRIPTOR). */
public record RegistroSuscriptorRequest(
        @NotBlank @Pattern(regexp = "[0-9]{6,12}", message = "debe tener entre 6 y 12 digitos") String documento,
        @NotBlank @Size(min = 3, max = 120) String nombre,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "3[0-9]{9}", message = "debe ser un celular de 10 digitos que empiece por 3") String telefono,
        @NotBlank @Size(max = 200) String direccion,
        @NotBlank String municipio,
        @NotBlank @Size(min = 6, max = 40, message = "debe tener entre 6 y 40 caracteres") String clave) {
}
