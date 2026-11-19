package com.gascorocora.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.EventSubscription;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gascorocora.config.ProcesosConstantes;
import com.gascorocora.persistence.NegocioRepository;

/**
 * Une el dato de negocio (tablas GC_*) con el estado vivo del motor: estado de la instancia,
 * tareas pendientes (incluidas las de subprocesos) y mensajes que el proceso esta esperando.
 * Con los mensajes esperados el portal sabe que accion ofrecer al usuario (firmar, pagar, recurrir).
 */
@Service
public class CasosService {

    private final NegocioRepository repositorio;
    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public CasosService(NegocioRepository repositorio, HistoryService historyService,
                        RuntimeService runtimeService, TaskService taskService) {
        this.repositorio = repositorio;
        this.historyService = historyService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    public Map<String, Object> consultar(String referencia) {
        Map<String, Object> caso = repositorio.buscarCaso(referencia);
        if (caso == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un caso con la referencia " + referencia);
        }
        return completar(caso);
    }

    /** Caso vinculado a una instancia (acepta tambien la instancia de un subproceso). */
    public Map<String, Object> consultarPorInstancia(String processInstanceId) {
        String raiz = processInstanceId;
        HistoricProcessInstance historica = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (historica != null && historica.getRootProcessInstanceId() != null) {
            raiz = historica.getRootProcessInstanceId();
        }
        Map<String, Object> caso = repositorio.buscarCasoPorInstancia(raiz);
        if (caso == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "La instancia " + processInstanceId + " aun no tiene un caso de negocio registrado");
        }
        return completar(caso);
    }

    public List<Map<String, Object>> recientes(int limite) {
        return enriquecer(repositorio.casosRecientes(Math.max(1, Math.min(limite, 200))));
    }

    private Map<String, Object> completar(Map<String, Object> caso) {
        @SuppressWarnings("unchecked")
        Map<String, Object> registro = (Map<String, Object>) caso.get("registro");
        enriquecer(registro);
        caso.put("eventos", repositorio.eventosDe(String.valueOf(registro.get("processInstanceId"))));
        return caso;
    }

    public List<Map<String, Object>> enriquecer(List<Map<String, Object>> registros) {
        registros.forEach(this::enriquecer);
        return registros;
    }

    /** Agrega estadoProceso, tareasPendientes, mensajesEsperados y accionesUsuario al registro. */
    public Map<String, Object> enriquecer(Map<String, Object> registro) {
        Object pi = registro.get("processInstanceId");
        if (pi == null) {
            return registro;
        }
        String processInstanceId = pi.toString();
        HistoricProcessInstance historica = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        registro.put("estadoProceso", historica == null ? "DESCONOCIDO" : historica.getState());

        List<Map<String, Object>> tareas = new ArrayList<>();
        List<String> mensajes = new ArrayList<>();
        recorrer(processInstanceId, tareas, mensajes);
        registro.put("tareasPendientes", tareas);
        registro.put("mensajesEsperados", mensajes);

        List<String> acciones = new ArrayList<>();
        if (mensajes.contains(ProcesosConstantes.MENSAJE_CONTRATO_FIRMADO)) {
            acciones.add("FIRMAR_CONTRATO");
        }
        if (mensajes.contains(ProcesosConstantes.MENSAJE_PAGO_RECIBIDO)) {
            acciones.add("PAGAR");
        }
        if (mensajes.contains(ProcesosConstantes.MENSAJE_RECURSO_REPOSICION)) {
            acciones.add("INTERPONER_RECURSO");
        }
        registro.put("accionesUsuario", acciones);
        return registro;
    }

    /** Recorre la instancia y sus subprocesos activos (call activities). */
    private void recorrer(String processInstanceId, List<Map<String, Object>> tareas, List<String> mensajes) {
        for (Task tarea : taskService.createTaskQuery().processInstanceId(processInstanceId).list()) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", tarea.getId());
            t.put("nombre", tarea.getName());
            t.put("asignadaA", tarea.getAssignee());
            t.put("creada", tarea.getCreateTime());
            tareas.add(t);
        }
        for (EventSubscription suscripcion : runtimeService.createEventSubscriptionQuery()
                .processInstanceId(processInstanceId).eventType("message").list()) {
            mensajes.add(suscripcion.getEventName());
        }
        for (ProcessInstance hija : runtimeService.createProcessInstanceQuery()
                .superProcessInstanceId(processInstanceId).list()) {
            recorrer(hija.getId(), tareas, mensajes);
        }
    }
}
