package com.gascorocora.dto;

import jakarta.validation.constraints.NotBlank;

/** Recurso de reposicion (y en subsidio apelacion) interpuesto por el usuario (mensaje MensajeRecursoReposicion). */
public record RecursoReposicionRequest(
        @NotBlank String motivoRecurso) {
}
