package com.gascorocora.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import com.gascorocora.config.ProcesosConstantes;
import com.gascorocora.dto.ContratoFirmadoRequest;
import com.gascorocora.dto.MensajeCorrelacionadoResponse;
import com.gascorocora.dto.ProcesoIniciadoResponse;
import com.gascorocora.dto.SolicitudConexionRequest;

/** Casos de uso del proceso P2 - Gestion de Conexion de Nuevos Usuarios. */
@Service
public class ConexionService {

    private final RuntimeService runtimeService;
    private final CorrelacionMensajesService correlacionMensajesService;

    public ConexionService(RuntimeService runtimeService, CorrelacionMensajesService correlacionMensajesService) {
        this.runtimeService = runtimeService;
        this.correlacionMensajesService = correlacionMensajesService;
    }

    public ProcesoIniciadoResponse solicitarConexion(SolicitudConexionRequest solicitud) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombreSolicitante", solicitud.nombreSolicitante());
        variables.put("documentoIdentidad", solicitud.documentoIdentidad());
        variables.put("emailSolicitante", solicitud.emailSolicitante());
        variables.put("telefonoSolicitante", solicitud.telefonoSolicitante());
        variables.put("direccionPredio", solicitud.direccionPredio());
        variables.put("municipio", solicitud.municipio());
        variables.put("tipoPredio", solicitud.tipoPredio());
        variables.put("estrato", solicitud.estrato());

        ProcessInstance instancia = runtimeService.startProcessInstanceByKey(
                ProcesosConstantes.PROCESO_CONEXION_NUEVOS_USUARIOS, solicitud.documentoIdentidad(), variables);
        return new ProcesoIniciadoResponse(instancia.getId(), instancia.getProcessDefinitionId(),
                instancia.getBusinessKey());
    }

    public MensajeCorrelacionadoResponse registrarFirmaContrato(String numeroSolicitud, ContratoFirmadoRequest firma) {
        Map<String, Object> variables = Map.of(
                "contratoFirmadoPor", firma.firmadoPor(),
                "canalFirma", firma.canalFirma(),
                "fechaFirmaContrato", LocalDateTime.now().toString());
        return correlacionMensajesService.correlacionar(ProcesosConstantes.MENSAJE_CONTRATO_FIRMADO,
                ProcesosConstantes.VAR_NUMERO_SOLICITUD, numeroSolicitud, variables);
    }
}
