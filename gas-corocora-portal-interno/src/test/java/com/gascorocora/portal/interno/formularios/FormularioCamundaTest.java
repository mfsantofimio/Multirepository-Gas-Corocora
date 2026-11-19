package com.gascorocora.portal.interno.formularios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de la lectura, validacion y conversion de los Camunda Forms del proyecto
 * (copias en src/test/resources/formularios). Casos CP-I01 a CP-I06 de docs/pruebas.md del hub.
 */
class FormularioCamundaTest {

    private static String formulario(String nombre) throws IOException {
        try (InputStream in = FormularioCamundaTest.class.getResourceAsStream("/formularios/" + nombre + ".form")) {
            assertNotNull(in, "no se encontro " + nombre);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, Object> variable(String tipo, Object valor) {
        Map<String, Object> v = new HashMap<>();
        v.put("type", tipo);
        v.put("value", valor);
        return v;
    }

    private static CampoFormulario campo(List<CampoFormulario> campos, String llave) {
        return campos.stream().filter(c -> llave.equals(c.getLlave())).findFirst().orElseThrow();
    }

    @Test
    void cpI01PrecargaVariablesEnCamposDeSoloLectura() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("numeroRadicado", variable("String", "PQRS-20261118-1234"));
        variables.put("valorReclamado", variable("Double", 120000.0));
        List<CampoFormulario> campos = FormularioCamunda.leer(formulario("formAnalizarCaso"), variables);

        CampoFormulario radicado = campo(campos, "numeroRadicado");
        assertTrue(radicado.isDeshabilitado());
        assertEquals("PQRS-20261118-1234", radicado.getValor());
        assertEquals("120000", campo(campos, "valorReclamado").getValor());
    }

    @Test
    void cpI02RechazaEnvioSinCamposObligatorios() throws IOException {
        List<CampoFormulario> campos = FormularioCamunda.leer(formulario("formAnalizarCaso"), Map.of());
        assertNull(FormularioCamunda.convertir(campos, Map.of()));
        assertTrue(campos.stream().anyMatch(c -> c.getError() != null));
    }

    @Test
    void cpI03ConvierteDecisionesAVariablesTipadasYProtegeCamposDeshabilitados() throws IOException {
        List<CampoFormulario> campos = FormularioCamunda.leer(formulario("formAnalizarCaso"), Map.of());
        Map<String, String> enviado = new HashMap<>();
        enviado.put("respuestaUsuario", "Se reviso la lectura y procede el ajuste.");
        enviado.put("procedeAjuste", "true");
        enviado.put("valorAjuste", "45000");
        enviado.put("numeroRadicado", "ALTERADO");

        Map<String, Map<String, Object>> variables = FormularioCamunda.convertir(campos, enviado);
        assertNotNull(variables);
        assertEquals(Boolean.TRUE, variables.get("procedeAjuste").get("value"));
        assertEquals("Boolean", variables.get("procedeAjuste").get("type"));
        assertEquals(45000L, variables.get("valorAjuste").get("value"));
        assertEquals("Long", variables.get("valorAjuste").get("type"));
        assertFalse(variables.containsKey("numeroRadicado"));
    }

    @Test
    void cpI03bCampoOpcionalVacioSeEnviaComoNull() throws IOException {
        List<CampoFormulario> campos = FormularioCamunda.leer(formulario("formAnalizarCaso"), Map.of());
        Map<String, String> enviado = new HashMap<>();
        enviado.put("respuestaUsuario", "No procede ajuste.");
        Map<String, Map<String, Object>> variables = FormularioCamunda.convertir(campos, enviado);
        assertNotNull(variables);
        assertEquals(Boolean.FALSE, variables.get("procedeAjuste").get("value"));
        assertTrue(variables.containsKey("valorAjuste"));
        assertNull(variables.get("valorAjuste").get("value"));
        assertEquals("Null", variables.get("valorAjuste").get("type"));
    }

    @Test
    void cpI04ValidaPatronListaYRangos() throws IOException {
        List<CampoFormulario> campos = FormularioCamunda.leer(formulario("formIniciarCicloFacturacion"), Map.of());
        Map<String, String> enviado = new HashMap<>();
        enviado.put("numeroCuenta", "CTA-1001");
        enviado.put("nombreUsuario", "Ana Perez");
        enviado.put("emailUsuario", "no-es-correo");
        enviado.put("tipoUsuario", "OTRO");
        enviado.put("estrato", "9");
        enviado.put("consumoPromedioM3", "20");
        enviado.put("lecturaAnterior", "1000");

        assertNull(FormularioCamunda.convertir(campos, enviado));
        assertNotNull(campo(campos, "emailUsuario").getError());
        assertNotNull(campo(campos, "tipoUsuario").getError());
        assertNotNull(campo(campos, "estrato").getError());
    }

    @Test
    void cpI05NumerosDecimalesYEnteros() throws IOException {
        List<CampoFormulario> campos = FormularioCamunda.leer(formulario("formIniciarCicloFacturacion"), Map.of());
        Map<String, String> enviado = new HashMap<>();
        enviado.put("numeroCuenta", "CTA-1001");
        enviado.put("nombreUsuario", "Ana Perez");
        enviado.put("emailUsuario", "ana@correo.co");
        enviado.put("tipoUsuario", "RESIDENCIAL");
        enviado.put("estrato", "2");
        enviado.put("consumoPromedioM3", "20,5");
        enviado.put("lecturaAnterior", "1000");

        Map<String, Map<String, Object>> variables = FormularioCamunda.convertir(campos, enviado);
        assertNotNull(variables);
        assertEquals("Long", variables.get("estrato").get("type"));
        assertEquals(20.5, variables.get("consumoPromedioM3").get("value"));
        assertEquals("Double", variables.get("consumoPromedioM3").get("type"));
    }

    @Test
    void cpI06CasillaSinMarcarEsFalse() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("lecturaActual", variable("Long", 1080));
        List<CampoFormulario> campos = FormularioCamunda.leer(formulario("formVerificarLectura"), variables);
        Map<String, String> enviado = new HashMap<>();
        for (CampoFormulario c : campos) {
            if (c.esEntrada() && !c.isDeshabilitado() && c.isRequerido() && !"checkbox".equals(c.getTipo())) {
                enviado.put(c.getLlave(), "number".equals(c.getTipo()) ? "1021" : "Verificado en terreno");
            }
        }
        Map<String, Map<String, Object>> resultado = FormularioCamunda.convertir(campos, enviado);
        assertNotNull(resultado);
        assertEquals(Boolean.FALSE, resultado.get("lecturaVerificada").get("value"));
    }
}
