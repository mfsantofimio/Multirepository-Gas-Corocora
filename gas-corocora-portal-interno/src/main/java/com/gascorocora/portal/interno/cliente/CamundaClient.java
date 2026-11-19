package com.gascorocora.portal.interno.cliente;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente de la API REST de Camunda 7 (/engine-rest) expuesta por gas-corocora-motor.
 * Identidad, consulta y atencion de tareas, formularios desplegados e inicio de procesos.
 */
@Component
public class CamundaClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA = new ParameterizedTypeReference<>() {
    };
    private static final String SERVICIO = "de Camunda (motor)";

    private final RestClient http;

    public CamundaClient(@Value("${gascorocora.camunda.url}") String url) {
        this.http = RestClient.builder().baseUrl(url).build();
    }

    // ------------------------------------------------------------------ identidad

    public boolean verificarClave(String usuario, String clave) {
        Map<String, Object> respuesta = ServicioException.ejecutar(SERVICIO, () -> http.post().uri("/identity/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", usuario, "password", clave))
                .retrieve().body(MAPA));
        return respuesta != null && Boolean.TRUE.equals(respuesta.get("authenticated"));
    }

    public Map<String, Object> perfil(String usuario) {
        return ServicioException.ejecutar(SERVICIO, () -> http.get().uri("/user/{id}/profile", usuario).retrieve().body(MAPA));
    }

    public List<Map<String, Object>> grupos(String usuario) {
        return ServicioException.ejecutar(SERVICIO, () -> http.get().uri("/group?member={u}", usuario).retrieve().body(LISTA));
    }

    // ------------------------------------------------------------------ tareas

    /** Tareas de los grupos del usuario (libres o asignadas) mas las asignadas directamente a el. */
    public List<Map<String, Object>> tareas(List<String> grupos, String usuario) {
        Map<String, Map<String, Object>> porId = new LinkedHashMap<>();
        if (!grupos.isEmpty()) {
            String lista = String.join(",", grupos);
            for (Map<String, Object> t : ServicioException.ejecutar(SERVICIO, () -> http.get()
                    .uri("/task?candidateGroups={g}&includeAssignedTasks=true&sortBy=created&sortOrder=desc", lista)
                    .retrieve().body(LISTA))) {
                porId.put(String.valueOf(t.get("id")), t);
            }
        }
        for (Map<String, Object> t : ServicioException.ejecutar(SERVICIO, () -> http.get()
                .uri("/task?assignee={u}&sortBy=created&sortOrder=desc", usuario).retrieve().body(LISTA))) {
            porId.putIfAbsent(String.valueOf(t.get("id")), t);
        }
        return new ArrayList<>(porId.values());
    }

    public Map<String, Object> tarea(String id) {
        return ServicioException.ejecutar(SERVICIO, () -> http.get().uri("/task/{id}", id).retrieve().body(MAPA));
    }

    /** Grupos candidatos de la tarea (para validar que el usuario la pueda atender). */
    public List<String> gruposDeTarea(String id) {
        List<Map<String, Object>> enlaces = ServicioException.ejecutar(SERVICIO,
                () -> http.get().uri("/task/{id}/identity-links?type=candidate", id).retrieve().body(LISTA));
        List<String> grupos = new ArrayList<>();
        for (Map<String, Object> enlace : enlaces) {
            if (enlace.get("groupId") != null) {
                grupos.add(String.valueOf(enlace.get("groupId")));
            }
        }
        return grupos;
    }

    /** Variables visibles desde la tarea: {nombre: {type, value}}. */
    public Map<String, Object> variables(String id) {
        return ServicioException.ejecutar(SERVICIO, () -> http.get().uri("/task/{id}/variables", id).retrieve().body(MAPA));
    }

    /** Contenido JSON del Camunda Form de la tarea (formRef). */
    public String formulario(String id) {
        return ServicioException.ejecutar(SERVICIO, () -> http.get().uri("/task/{id}/deployed-form", id).retrieve().body(String.class));
    }

    public void reclamar(String id, String usuario) {
        ServicioException.ejecutar(SERVICIO, () -> http.post().uri("/task/{id}/claim", id)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("userId", usuario)).retrieve().toBodilessEntity());
    }

    public void liberar(String id) {
        ServicioException.ejecutar(SERVICIO, () -> http.post().uri("/task/{id}/unclaim", id)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of()).retrieve().toBodilessEntity());
    }

    /** Completa la tarea con variables tipadas {nombre: {value, type}}. */
    public void completar(String id, Map<String, Map<String, Object>> variables) {
        ServicioException.ejecutar(SERVICIO, () -> http.post().uri("/task/{id}/submit-form", id)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("variables", variables)).retrieve().toBodilessEntity());
    }

    // ------------------------------------------------------------------ procesos

    public Map<String, Object> definicion(String clave) {
        return ServicioException.ejecutar(SERVICIO, () -> http.get().uri("/process-definition/key/{k}", clave).retrieve().body(MAPA));
    }

    public String formularioInicio(String clave) {
        return ServicioException.ejecutar(SERVICIO,
                () -> http.get().uri("/process-definition/key/{k}/deployed-start-form", clave).retrieve().body(String.class));
    }

    public Map<String, Object> iniciar(String clave, Map<String, Map<String, Object>> variables, String businessKey) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("variables", variables);
        if (businessKey != null && !businessKey.isBlank()) {
            cuerpo.put("businessKey", businessKey);
        }
        return ServicioException.ejecutar(SERVICIO, () -> http.post().uri("/process-definition/key/{k}/submit-form", clave)
                .contentType(MediaType.APPLICATION_JSON).body(cuerpo).retrieve().body(MAPA));
    }
}
