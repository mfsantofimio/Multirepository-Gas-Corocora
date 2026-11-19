package com.gascorocora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Plazos (ISO-8601) de los eventos de temporizador. El bean se llama "plazosProceso" y se
 * usa directamente desde el BPMN, por ejemplo: ${plazosProceso.pagoFactura}.
 * Los valores se leen de application.yaml (gascorocora.plazos.*).
 */
@Component("plazosProceso")
@ConfigurationProperties(prefix = "gascorocora.plazos")
public class PlazosProceso {

    private String pagoFactura = "PT5M";
    private String alertaTerminoPqrs = "PT2M";
    private String terminoRecursoPqrs = "PT5M";
    private String plazoObraConexion = "PT10M";

    public String getPagoFactura() {
        return pagoFactura;
    }

    public void setPagoFactura(String pagoFactura) {
        this.pagoFactura = pagoFactura;
    }

    public String getAlertaTerminoPqrs() {
        return alertaTerminoPqrs;
    }

    public void setAlertaTerminoPqrs(String alertaTerminoPqrs) {
        this.alertaTerminoPqrs = alertaTerminoPqrs;
    }

    public String getTerminoRecursoPqrs() {
        return terminoRecursoPqrs;
    }

    public void setTerminoRecursoPqrs(String terminoRecursoPqrs) {
        this.terminoRecursoPqrs = terminoRecursoPqrs;
    }

    public String getPlazoObraConexion() {
        return plazoObraConexion;
    }

    public void setPlazoObraConexion(String plazoObraConexion) {
        this.plazoObraConexion = plazoObraConexion;
    }
}
