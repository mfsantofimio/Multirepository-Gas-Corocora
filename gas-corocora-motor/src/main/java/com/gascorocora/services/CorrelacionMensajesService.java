package com.gascorocora.services;

import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.springframework.stereotype.Service;

import com.gascorocora.dto.MensajeCorrelacionadoResponse;

/**
 * Logica comun para entregar un mensaje BPMN a la instancia que lo esta esperando.
 * La correlacion se hace por una variable de negocio (numero de factura, radicado o solicitud).
 */
@Service
public class CorrelacionMensajesService {

    private final RuntimeService runtimeService;

    public CorrelacionMensajesService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    /**
     * @throws org.camunda.bpm.engine.MismatchingMessageCorrelationException si ninguna instancia espera el mensaje
     */
    public MensajeCorrelacionadoResponse correlacionar(String mensaje, String variableCorrelacion,
                                                      String valorCorrelacion, Map<String, Object> variables) {
        MessageCorrelationResult resultado = runtimeService.createMessageCorrelation(mensaje)
                .processInstanceVariableEquals(variableCorrelacion, valorCorrelacion)
                .setVariables(variables)
                .correlateWithResult();
        return new MensajeCorrelacionadoResponse(mensaje,
                resultado.getExecution().getProcessInstanceId(), valorCorrelacion);
    }
}
