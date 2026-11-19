package com.gascorocora.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirmacion de firma del contrato de condiciones uniformes (mensaje MensajeContratoFirmado). */
public record ContratoFirmadoRequest(
        @NotBlank String firmadoPor,
        @NotBlank String canalFirma) {
}
