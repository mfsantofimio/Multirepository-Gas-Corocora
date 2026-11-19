package com.gascorocora.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;

/** Consulta de estado y variables de una instancia (activa o finalizada) usando el historico. */
@Service
public class ConsultaProcesosService {

    private final HistoryService historyService;

    public ConsultaProcesosService(HistoryService historyService) {
        this.historyService = historyService;
    }

    /** @return null si la instancia no existe */
    public Map<String, Object> consultar(String processInstanceId) {
        HistoricProcessInstance instancia = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (instancia == null) {
            return null;
        }
        Map<String, Object> variables = new TreeMap<>();
        for (HistoricVariableInstance variable : historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).list()) {
            variables.put(variable.getName(), variable.getValue());
        }
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("processInstanceId", instancia.getId());
        respuesta.put("proceso", instancia.getProcessDefinitionKey());
        respuesta.put("businessKey", instancia.getBusinessKey());
        respuesta.put("estado", instancia.getState());
        respuesta.put("inicio", instancia.getStartTime());
        respuesta.put("fin", instancia.getEndTime());
        respuesta.put("variables", variables);
        return respuesta;
    }
}
