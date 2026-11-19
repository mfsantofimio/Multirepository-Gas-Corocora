package com.gascorocora.portal.usuarios.cliente;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** API del servicio de Recaudo / Tesoreria (gas-corocora-recaudo). */
@Component
public class RecaudoClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };
    private static final String SERVICIO = "de Recaudo";

    private final RestClient http;

    public RecaudoClient(@Value("${gascorocora.recaudo.url}") String url) {
        this.http = RestClient.builder().baseUrl(url).build();
    }

    /** Acepta el numero de factura o la referencia de pago. */
    public Map<String, Object> obligacion(String numeroFactura) {
        return ServicioException.ejecutar(SERVICIO,
                () -> http.get().uri("/api/obligaciones/{n}", numeroFactura).retrieve().body(MAPA));
    }

    /** Registra el pago; Recaudo lo notifica al motor (mensaje "Pago recibido"). */
    public Map<String, Object> pagar(String referenciaPago, double valor, String medioPago) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("referenciaPago", referenciaPago);
        cuerpo.put("valor", valor);
        cuerpo.put("medioPago", medioPago);
        return ServicioException.ejecutar(SERVICIO, () -> http.post().uri("/api/pagos")
                .contentType(MediaType.APPLICATION_JSON).body(cuerpo).retrieve().body(MAPA));
    }
}
