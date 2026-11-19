package com.gascorocora.portal.interno.formularios;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Interpreta el JSON de un Camunda Form (form-js) y lo convierte en campos para Thymeleaf.
 * Al enviar, valida (required, min, max, pattern, opciones) y convierte los valores a variables tipadas
 * de Camunda: number -> Long/Double, checkbox -> Boolean, el resto -> String.
 * Los campos deshabilitados son de solo lectura y no se envian (igual que en Tasklist).
 */
public final class FormularioCamunda {

    private static final ObjectMapper JSON = new ObjectMapper();

    private FormularioCamunda() {
    }

    /**
     * @param esquema JSON del formulario desplegado
     * @param variables variables actuales en formato REST de Camunda {nombre: {value, type}} (puede ser vacio)
     */
    public static List<CampoFormulario> leer(String esquema, Map<String, Object> variables) {
        List<CampoFormulario> campos = new ArrayList<>();
        JsonNode raiz;
        try {
            raiz = JSON.readTree(esquema);
        } catch (Exception e) {
            throw new IllegalArgumentException("El formulario no es un JSON valido: " + e.getMessage(), e);
        }
        for (JsonNode c : raiz.path("components")) {
            String tipo = c.path("type").asText();
            CampoFormulario campo = new CampoFormulario();
            campo.setTipo(tipo);
            if ("text".equals(tipo)) {
                String texto = c.path("text").asText("").trim();
                String[] lineas = texto.split("\\R", 2);
                campo.setTitulo(lineas[0].replaceFirst("^#+\\s*", ""));
                campo.setParrafo(lineas.length > 1 ? lineas[1].trim() : "");
                campos.add(campo);
                continue;
            }
            if (!List.of("textfield", "textarea", "number", "checkbox", "select").contains(tipo) || !c.hasNonNull("key")) {
                continue; // componentes no usados en los formularios del proyecto
            }
            String llave = c.path("key").asText();
            campo.setLlave(llave);
            campo.setEtiqueta(c.path("label").asText(llave));
            campo.setDescripcion(c.path("description").asText(null));
            campo.setDeshabilitado(c.path("disabled").asBoolean(false) || c.path("readonly").asBoolean(false));
            JsonNode v = c.path("validate");
            campo.setRequerido(v.path("required").asBoolean(false));
            campo.setPatron(v.hasNonNull("pattern") ? v.path("pattern").asText() : null);
            campo.setMinimo(v.hasNonNull("min") ? v.path("min").asText() : null);
            campo.setMaximo(v.hasNonNull("max") ? v.path("max").asText() : null);
            for (JsonNode o : c.path("values")) {
                campo.getOpciones().add(new CampoFormulario.Opcion(o.path("value").asText(), o.path("label").asText()));
            }

            Object actual = valorVariable(variables, llave);
            if (actual == null && c.has("defaultValue") && !c.path("defaultValue").isNull()) {
                JsonNode d = c.path("defaultValue");
                actual = d.isBoolean() ? d.asBoolean() : d.isNumber() ? d.numberValue() : d.asText();
            }
            if ("checkbox".equals(tipo)) {
                campo.setMarcado(Boolean.TRUE.equals(actual) || "true".equals(String.valueOf(actual)));
            } else {
                campo.setValor(comoTexto(actual));
            }
            campos.add(campo);
        }
        return campos;
    }

    /**
     * Valida lo enviado y arma las variables tipadas. Marca los errores en cada campo.
     * @return variables para /submit-form o null si hay errores
     */
    public static Map<String, Map<String, Object>> convertir(List<CampoFormulario> campos, Map<String, String> enviado) {
        Map<String, Map<String, Object>> variables = new LinkedHashMap<>();
        boolean hayErrores = false;
        for (CampoFormulario campo : campos) {
            if (!campo.esEntrada() || campo.isDeshabilitado()) {
                continue;
            }
            String llave = campo.getLlave();
            if ("checkbox".equals(campo.getTipo())) {
                boolean marcado = enviado.containsKey(llave);
                campo.setMarcado(marcado);
                variables.put(llave, tipada(marcado, "Boolean"));
                continue;
            }
            String texto = enviado.getOrDefault(llave, "").trim();
            campo.setValor(texto);
            if (texto.isEmpty()) {
                if (campo.isRequerido()) {
                    campo.setError("Este campo es obligatorio");
                    hayErrores = true;
                } else {
                    variables.put(llave, tipada(null, "Null")); // igual que Tasklist: campo opcional vacio = null
                }
                continue;
            }
            switch (campo.getTipo()) {
                case "number" -> {
                    BigDecimal numero;
                    try {
                        numero = new BigDecimal(texto.replace(',', '.'));
                    } catch (NumberFormatException e) {
                        campo.setError("Debe ser un número");
                        hayErrores = true;
                        continue;
                    }
                    if (campo.getMinimo() != null && numero.compareTo(new BigDecimal(campo.getMinimo())) < 0) {
                        campo.setError("El valor mínimo es " + campo.getMinimo());
                        hayErrores = true;
                        continue;
                    }
                    if (campo.getMaximo() != null && numero.compareTo(new BigDecimal(campo.getMaximo())) > 0) {
                        campo.setError("El valor máximo es " + campo.getMaximo());
                        hayErrores = true;
                        continue;
                    }
                    boolean entero = numero.stripTrailingZeros().scale() <= 0;
                    variables.put(llave, entero ? tipada(numero.longValueExact(), "Long") : tipada(numero.doubleValue(), "Double"));
                }
                case "select" -> {
                    boolean valida = campo.getOpciones().stream().anyMatch(o -> o.valor().equals(texto));
                    if (!valida) {
                        campo.setError("Seleccione una opción de la lista");
                        hayErrores = true;
                        continue;
                    }
                    variables.put(llave, tipada(texto, "String"));
                }
                default -> {
                    if (campo.getPatron() != null && !cumplePatron(campo.getPatron(), texto)) {
                        campo.setError("El formato no es válido");
                        hayErrores = true;
                        continue;
                    }
                    variables.put(llave, tipada(texto, "String"));
                }
            }
        }
        return hayErrores ? null : variables;
    }

    private static boolean cumplePatron(String patron, String texto) {
        try {
            return Pattern.compile(patron).matcher(texto).find();
        } catch (PatternSyntaxException e) {
            return true; // patron no compatible con Java: se valida en el motor
        }
    }

    private static Map<String, Object> tipada(Object valor, String tipo) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("value", valor);
        v.put("type", tipo);
        return v;
    }

    @SuppressWarnings("unchecked")
    private static Object valorVariable(Map<String, Object> variables, String llave) {
        if (variables == null) {
            return null;
        }
        Object v = variables.get(llave);
        return v instanceof Map<?, ?> m ? ((Map<String, Object>) m).get("value") : null;
    }

    private static String comoTexto(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Number n) {
            BigDecimal d = new BigDecimal(n.toString());
            return d.stripTrailingZeros().scale() <= 0 ? d.toBigInteger().toString() : d.stripTrailingZeros().toPlainString();
        }
        return valor.toString();
    }
}
