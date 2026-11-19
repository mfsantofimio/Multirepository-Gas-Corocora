package com.gascorocora.dto;

/** Respuesta al iniciar una instancia de proceso. */
public record ProcesoIniciadoResponse(
        String processInstanceId,
        String processDefinitionId,
        String businessKey) {
}
