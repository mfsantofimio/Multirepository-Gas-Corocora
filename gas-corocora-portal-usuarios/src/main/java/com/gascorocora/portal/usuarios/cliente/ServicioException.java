package com.gascorocora.portal.usuarios.cliente;

import java.util.function.Supplier;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Error de un servicio remoto (motor o recaudo) con un mensaje listo para mostrar al usuario. */
public class ServicioException extends RuntimeException {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final int estado;

    public ServicioException(int estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public int getEstado() {
        return estado;
    }

    /** Ejecuta la llamada y traduce los errores HTTP y de conexion. */
    public static <T> T ejecutar(String servicio, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RestClientResponseException e) {
            throw new ServicioException(e.getStatusCode().value(), mensajeDe(e.getResponseBodyAsString(), e.getStatusText()));
        } catch (ResourceAccessException e) {
            throw new ServicioException(503, "El servicio " + servicio + " no esta disponible. Verifique que este iniciado.");
        }
    }

    private static String mensajeDe(String cuerpo, String porDefecto) {
        try {
            JsonNode nodo = JSON.readTree(cuerpo);
            for (String campo : new String[] {"detalle", "message", "error"}) {
                String valor = nodo.path(campo).asText("");
                if (!valor.isBlank()) {
                    return valor;
                }
            }
        } catch (Exception ignorada) {
            // cuerpo no JSON
        }
        return porDefecto;
    }
}
