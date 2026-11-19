package com.gascorocora.services;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import com.gascorocora.config.ProcesosConstantes;
import com.gascorocora.dto.MensajeCorrelacionadoResponse;
import com.gascorocora.dto.ProcesoIniciadoResponse;
import com.gascorocora.dto.RadicarPqrsRequest;
import com.gascorocora.dto.RecursoReposicionRequest;

/** Casos de uso del proceso P6 - Atencion al Usuario y PQRS. */
@Service
public class PqrsService {

    private final RuntimeService runtimeService;
    private final CorrelacionMensajesService correlacionMensajesService;

    public PqrsService(RuntimeService runtimeService, CorrelacionMensajesService correlacionMensajesService) {
        this.runtimeService = runtimeService;
        this.correlacionMensajesService = correlacionMensajesService;
    }

    public ProcesoIniciadoResponse radicar(RadicarPqrsRequest solicitud) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("numeroCuenta", solicitud.numeroCuenta());
        variables.put("nombreUsuario", solicitud.nombreUsuario());
        variables.put("emailUsuario", solicitud.emailUsuario());
        variables.put("tipoSolicitud", solicitud.tipoSolicitud());
        variables.put("canal", solicitud.canal());
        variables.put("valorReclamado", solicitud.valorReclamado() == null ? 0.0 : solicitud.valorReclamado());
        variables.put("descripcion", solicitud.descripcion());

        ProcessInstance instancia = runtimeService.startProcessInstanceByKey(
                ProcesosConstantes.PROCESO_ATENCION_PQRS, solicitud.numeroCuenta(), variables);
        return new ProcesoIniciadoResponse(instancia.getId(), instancia.getProcessDefinitionId(),
                instancia.getBusinessKey());
    }

    public MensajeCorrelacionadoResponse presentarRecurso(String numeroRadicado, RecursoReposicionRequest recurso) {
        return correlacionMensajesService.correlacionar(ProcesosConstantes.MENSAJE_RECURSO_REPOSICION,
                ProcesosConstantes.VAR_NUMERO_RADICADO, numeroRadicado,
                Map.of("motivoRecurso", recurso.motivoRecurso()));
    }
}
