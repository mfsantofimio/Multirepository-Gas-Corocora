package com.gascorocora.dto;

import jakarta.validation.constraints.NotBlank;

/** Ingreso de un usuario externo al portal. */
public record AutenticacionRequest(
        @NotBlank String documento,
        @NotBlank String clave) {
}
