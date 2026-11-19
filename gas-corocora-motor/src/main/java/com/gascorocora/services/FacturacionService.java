package com.gascorocora.services;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import com.gascorocora.config.ProcesosConstantes;
import com.gascorocora.dto.IniciarCicloFacturacionRequest;
import com.gascorocora.dto.MensajeCorrelacionadoResponse;
import com.gascorocora.dto.PagoRecibidoRequest;
import com.gascorocora.dto.ProcesoIniciadoResponse;

/** Casos de uso del proceso P4 - Gestion de Medicion y Facturacion. */
@Service
public class FacturacionService {

    private final RuntimeService runtimeService;
    private final CorrelacionMensajesService correlacionMensajesService;

    public FacturacionService(RuntimeService runtimeService, CorrelacionMensajesService correlacionMensajesService) {
        this.runtimeService = runtimeService;
        this.correlacionMensajesService = correlacionMensajesService;
    }

    public ProcesoIniciadoResponse iniciarCiclo(IniciarCicloFacturacionRequest solicitud) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("numeroCuenta", solicitud.numeroCuenta());
        variables.put("nombreUsuario", solicitud.nombreUsuario());
        variables.put("emailUsuario", solicitud.emailUsuario());
        variables.put("tipoUsuario", solicitud.tipoUsuario());
        variables.put("estrato", solicitud.estrato());
        variables.put("consumoPromedioM3", solicitud.consumoPromedioM3());
        variables.put("lecturaAnterior", solicitud.lecturaAnterior());

        ProcessInstance instancia = runtimeService.startProcessInstanceByKey(
                ProcesosConstantes.PROCESO_MEDICION_FACTURACION, solicitud.numeroCuenta(), variables);
        return new ProcesoIniciadoResponse(instancia.getId(), instancia.getProcessDefinitionId(),
                instancia.getBusinessKey());
    }

    public MensajeCorrelacionadoResponse registrarPago(String numeroFactura, PagoRecibidoRequest pago) {
        Map<String, Object> variables = Map.of(
                "valorPagado", pago.valorPagado(),
                "medioPago", pago.medioPago());
        return correlacionMensajesService.correlacionar(ProcesosConstantes.MENSAJE_PAGO_RECIBIDO,
                ProcesosConstantes.VAR_NUMERO_FACTURA, numeroFactura, variables);
    }
}
