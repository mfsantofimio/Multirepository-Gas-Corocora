package com.gascorocora.portal.interno.web;

import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

/** Ayudas de presentacion usadas desde las plantillas Thymeleaf: ${fmt.proceso(clave)}. */
@Component
public class Formato {

    private static final DateTimeFormatter CAMUNDA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final DateTimeFormatter SALIDA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> PROCESOS = Map.of(
            "Proceso_MedicionFacturacion", "Medición y facturación",
            "Proceso_AtencionPqrs", "Atención de PQRS",
            "Proceso_ConexionNuevosUsuarios", "Conexión de nuevos usuarios",
            "Proceso_SuspensionServicio", "Suspensión del servicio",
            "Proceso_AjusteFactura", "Ajuste de factura",
            "Proceso_ConstruccionAcometida", "Construcción de acometida");

    private static final Map<String, String> GRUPOS = Map.of(
            "asesoresComerciales", "Asesor comercial", "ingenierosRedes", "Ingeniero de redes",
            "contratistas", "Contratista", "inspectoresCalidad", "Inspector de calidad",
            "lectoresCampo", "Lector de campo", "analistasFacturacion", "Analista de facturación",
            "tecnicosSuspension", "Técnico de suspensión", "agentesServicio", "Agente de servicio",
            "analistasPqrs", "Analista de PQRS", "jefesServicioCliente", "Jefe de servicio al cliente");

    private static final Map<String, String> ESTADOS = Map.ofEntries(
            Map.entry("RADICADA", "Radicada"), Map.entry("DEVUELTA", "Devuelta"),
            Map.entry("NO_VIABLE", "No viable"), Map.entry("CONTRATO_GENERADO", "Contrato generado"),
            Map.entry("ESPERANDO_FIRMA", "Esperando firma"), Map.entry("EN_CONSTRUCCION", "En construcción"),
            Map.entry("OBRA_ESCALADA", "Obra escalada"), Map.entry("CONECTADA", "Conectada"),
            Map.entry("EN_REVISION", "En revisión"), Map.entry("PENDIENTE_PAGO", "Pendiente de pago"),
            Map.entry("PAGADA", "Pagada"), Map.entry("PAGO_PARCIAL", "Pago parcial"),
            Map.entry("EN_MORA", "En mora"), Map.entry("SUSPENDIDA", "Suspendida"),
            Map.entry("RESPONDIDA", "Respondida"), Map.entry("RECURSO_EN_TRAMITE", "Recurso en trámite"),
            Map.entry("RECURSO_CONFIRMADO", "Recurso: decisión confirmada"),
            Map.entry("RECURSO_A_FAVOR", "Recurso a favor del usuario"),
            Map.entry("EN_APELACION_SSPD", "En apelación SSPD"),
            Map.entry("ACTIVE", "En curso"), Map.entry("COMPLETED", "Finalizado"),
            Map.entry("EXTERNALLY_TERMINATED", "Cancelado"), Map.entry("INTERNALLY_TERMINATED", "Cerrado"));

    /** Clave de definicion o processDefinitionId (clave:version:id) -> nombre legible. */
    public String proceso(Object definicion) {
        if (definicion == null) {
            return "—";
        }
        String clave = definicion.toString().split(":")[0];
        return PROCESOS.getOrDefault(clave, clave);
    }

    public String grupo(Object grupo) {
        return grupo == null ? "—" : GRUPOS.getOrDefault(grupo.toString(), grupo.toString());
    }

    public String estado(Object estado) {
        return estado == null ? "—" : ESTADOS.getOrDefault(estado.toString(), estado.toString());
    }

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

    /** Fecha de Camunda (2026-11-18T10:15:00.000-0500) -> 2026-11-18 10:15. */
    public String fecha(Object valor) {
        if (valor == null) {
            return "—";
        }
        String texto = valor.toString();
        try {
            return OffsetDateTime.parse(texto, CAMUNDA).format(SALIDA);
        } catch (DateTimeParseException e) {
            return texto.length() > 16 ? texto.substring(0, 16).replace('T', ' ') : texto;
        }
    }

    public String cop(Object valor) {
        if (!(valor instanceof Number numero)) {
            return "—";
        }
        return "$" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(Math.round(numero.doubleValue()));
    }

    /** numeroSolicitud -> "Numero solicitud" */
    public String etiqueta(String llave) {
        if (llave == null || llave.isEmpty()) {
            return "";
        }
        String texto = llave.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    public String texto(Object valor) {
        return valor == null || valor.toString().isBlank() ? "—" : valor.toString();
    }

    /** Valor de una variable REST de Camunda {type, value}. */
    public String variable(Object variable) {
        if (variable instanceof Map<?, ?> m) {
            Object valor = m.get("value");
            if ("Json".equals(m.get("type")) || "Object".equals(m.get("type"))) {
                return "(" + m.get("type") + ")";
            }
            return texto(valor);
        }
        return texto(variable);
    }
}
