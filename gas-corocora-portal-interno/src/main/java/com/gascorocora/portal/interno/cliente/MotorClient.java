package com.gascorocora.portal.interno.cliente;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** API de negocio del motor: casos (solicitud, factura, PQRS) con su bitacora. */
@Component
public class MotorClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA = new ParameterizedTypeReference<>() {
    };
    private static final String SERVICIO = "del motor de procesos";

    private final RestClient http;

    public MotorClient(@Value("${gascorocora.motor.url}") String url) {
        this.http = RestClient.builder().baseUrl(url).build();
    }

    public Map<String, Object> casoPorInstancia(String processInstanceId) {
        return ServicioException.ejecutar(SERVICIO,
                () -> http.get().uri("/api/casos/por-instancia/{pi}", processInstanceId).retrieve().body(MAPA));
    }

    public Map<String, Object> caso(String referencia) {
        return ServicioException.ejecutar(SERVICIO, () -> http.get().uri("/api/casos/{r}", referencia).retrieve().body(MAPA));
    }

    public List<Map<String, Object>> recientes(int limite) {
        return ServicioException.ejecutar(SERVICIO, () -> http.get().uri("/api/casos?limite={l}", limite).retrieve().body(LISTA));
    }
}
