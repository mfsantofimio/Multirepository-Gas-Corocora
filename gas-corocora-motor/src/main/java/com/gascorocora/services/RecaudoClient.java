package com.gascorocora.services;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP del servicio externo de Recaudo / Tesoreria (repositorio gas-corocora-recaudo).
 * Lo usa el subproceso "Ajuste de factura" para aplicar notas credito sobre la cartera del usuario.
 */
@Component
public class RecaudoClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final String urlBase;

    public RecaudoClient(@Value("${gascorocora.recaudo.url:http://127.0.0.1:8083}") String urlBase) {
        this.urlBase = urlBase;
        this.restClient = RestClient.builder().baseUrl(urlBase).build();
    }

    /**
     * POST /api/notas-credito -> aplica el valor sobre la obligacion pendiente mas reciente de la cuenta.
     * @return respuesta del servicio: numeroNotaCredito, referenciaPago, saldoAnterior, nuevoSaldo, saldoAFavor
     */
    public Map<String, Object> aplicarNotaCredito(String numeroCuenta, double valor, String numeroRadicado) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("numeroCuenta", numeroCuenta);
        cuerpo.put("valor", valor);
        cuerpo.put("numeroRadicado", numeroRadicado);
        return restClient.post()
                .uri("/api/notas-credito")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(MAPA);
    }

    public String getUrlBase() {
        return urlBase;
    }
}
