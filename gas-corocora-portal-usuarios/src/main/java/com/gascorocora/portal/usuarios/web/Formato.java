package com.gascorocora.portal.usuarios.web;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

/** Ayudas de presentacion usadas desde las plantillas Thymeleaf: ${fmt.cop(valor)}. */
@Component
public class Formato {

    private static final Map<String, String> ESTADOS = Map.ofEntries(
            Map.entry("RADICADA", "Radicada"), Map.entry("DEVUELTA", "Devuelta (documentos)"),
            Map.entry("NO_VIABLE", "No viable"), Map.entry("CONTRATO_GENERADO", "Contrato en elaboración"),
            Map.entry("ESPERANDO_FIRMA", "Esperando su firma"), Map.entry("EN_CONSTRUCCION", "En construcción"),
            Map.entry("OBRA_ESCALADA", "Obra escalada"), Map.entry("CONECTADA", "Conectada"),
            Map.entry("EN_REVISION", "En revisión"), Map.entry("PENDIENTE_PAGO", "Pendiente de pago"),
            Map.entry("PAGADA", "Pagada"), Map.entry("PAGO_PARCIAL", "Pago parcial"),
            Map.entry("EN_MORA", "En mora"), Map.entry("SUSPENDIDA", "Servicio suspendido"),
            Map.entry("RESPONDIDA", "Respondida"), Map.entry("RECURSO_EN_TRAMITE", "Recurso en trámite"),
            Map.entry("RECURSO_CONFIRMADO", "Recurso resuelto (decisión confirmada)"),
            Map.entry("RECURSO_A_FAVOR", "Recurso resuelto a su favor"),
            Map.entry("EN_APELACION_SSPD", "En apelación ante la SSPD"),
            Map.entry("ACTIVE", "En curso"), Map.entry("COMPLETED", "Finalizado"),
            Map.entry("EXTERNALLY_TERMINATED", "Cancelado"), Map.entry("INTERNALLY_TERMINATED", "Cerrado"));

    public String cop(Object valor) {
        if (!(valor instanceof Number numero)) {
            return "—";
        }
        NumberFormat formato = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO"));
        return "$" + formato.format(Math.round(numero.doubleValue()));
    }

    public String estado(Object estado) {
        if (estado == null) {
            return "—";
        }
        return ESTADOS.getOrDefault(estado.toString(), estado.toString());
    }

    /** Clase CSS del chip segun el estado. */
    public String claseEstado(Object estado) {
        String e = estado == null ? "" : estado.toString();
        if (e.matches("CONECTADA|PAGADA|COMPLETED|RECURSO_A_FAVOR")) {
            return "chip ok";
        }
        if (e.matches("ESPERANDO_FIRMA|PENDIENTE_PAGO|PAGO_PARCIAL|RECURSO_EN_TRAMITE|EN_REVISION")) {
            return "chip alerta";
        }
        if (e.matches("NO_VIABLE|DEVUELTA|EN_MORA|SUSPENDIDA|OBRA_ESCALADA|EXTERNALLY_TERMINATED")) {
            return "chip error";
        }
        return "chip info";
    }

    /** numeroSolicitud -> "Numero solicitud" */
    public String etiqueta(String llave) {
        String texto = llave.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    public String texto(Object valor) {
        return valor == null || valor.toString().isBlank() ? "—" : valor.toString();
    }
}
