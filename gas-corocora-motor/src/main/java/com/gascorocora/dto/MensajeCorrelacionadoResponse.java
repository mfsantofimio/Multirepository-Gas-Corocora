package com.gascorocora.dto;

/** Respuesta al correlacionar un mensaje BPMN con una instancia en espera. */
public record MensajeCorrelacionadoResponse(
        String mensaje,
        String processInstanceId,
        String llaveCorrelacion) {
}
