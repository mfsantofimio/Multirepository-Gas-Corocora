package com.gascorocora.portal.usuarios.cliente;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** API del motor de procesos (gas-corocora-motor). */
@Component
public class MotorClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };
    private static final String SERVICIO = "del motor de procesos";

    private final RestClient http;

    public MotorClient(@Value("${gascorocora.motor.url}") String url) {
        this.http = RestClient.builder().baseUrl(url).build();
    }

    public Map<String, Object> registrar(Map<String, Object> datos) {
        return post("/api/suscriptores", datos);
    }

    public Map<String, Object> autenticar(String documento, String clave) {
        return post("/api/suscriptores/autenticar", Map.of("documento", documento, "clave", clave));
    }

    public Map<String, Object> tramites(String documento) {
        return get("/api/suscriptores/{documento}/tramites", documento);
    }

    public Map<String, Object> caso(String referencia) {
        return get("/api/casos/{referencia}", referencia);
    }

    /** Inicia el proceso Gestion de Conexion de Nuevos Usuarios. */
    public Map<String, Object> solicitarConexion(Map<String, Object> datos) {
        return post("/api/conexiones/solicitudes", datos);
    }

    /** Mensaje BPMN "Contrato firmado por el usuario". */
    public Map<String, Object> firmarContrato(String numeroSolicitud, String firmadoPor) {
        return post("/api/conexiones/solicitudes/{numero}/firma-contrato",
                Map.of("firmadoPor", firmadoPor, "canalFirma", "WEB"), numeroSolicitud);
    }

    /** Inicia el proceso Atencion al Usuario y PQRS (canal WEB). */
    public Map<String, Object> radicarPqrs(Map<String, Object> datos) {
        return post("/api/pqrs", datos);
    }

    /** Mensaje BPMN "Recurso de reposicion recibido". */
    public Map<String, Object> interponerRecurso(String numeroRadicado, String motivo) {
        return post("/api/pqrs/{radicado}/recursos", Map.of("motivoRecurso", motivo), numeroRadicado);
    }

    private Map<String, Object> get(String uri, Object... variables) {
        return ServicioException.ejecutar(SERVICIO, () -> http.get().uri(uri, variables).retrieve().body(MAPA));
    }

    private Map<String, Object> post(String uri, Object cuerpo, Object... variables) {
        return ServicioException.ejecutar(SERVICIO, () -> http.post().uri(uri, variables)
                .contentType(MediaType.APPLICATION_JSON).body(cuerpo).retrieve().body(MAPA));
    }
}
