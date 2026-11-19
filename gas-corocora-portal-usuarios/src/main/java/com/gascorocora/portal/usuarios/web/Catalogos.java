package com.gascorocora.portal.usuarios.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Listas de valores. Los codigos coinciden con los Camunda Forms y las validaciones del motor
 * (formSolicitudConexion, formRadicarPqrs, DTO del motor).
 */
@Component
public class Catalogos {

    private final Map<String, String> municipios = new LinkedHashMap<>();
    private final Map<String, String> tiposPredio = new LinkedHashMap<>();
    private final Map<String, String> tiposPqrs = new LinkedHashMap<>();
    private final Map<String, String> mediosPago = new LinkedHashMap<>();

    public Catalogos() {
        municipios.put("VILLAVICENCIO", "Villavicencio");
        municipios.put("ACACIAS", "Acacías");
        municipios.put("GRANADA", "Granada");
        municipios.put("PUERTO_LOPEZ", "Puerto López");
        municipios.put("RESTREPO", "Restrepo");
        municipios.put("CUMARAL", "Cumaral");
        municipios.put("OTRO", "Otro municipio del Meta");

        tiposPredio.put("RESIDENCIAL", "Residencial");
        tiposPredio.put("COMERCIAL", "Comercial");
        tiposPredio.put("INDUSTRIAL", "Industrial");

        tiposPqrs.put("PETICION", "Petición");
        tiposPqrs.put("QUEJA", "Queja");
        tiposPqrs.put("RECLAMO", "Reclamo (por un cobro de la factura)");
        tiposPqrs.put("SUGERENCIA", "Sugerencia");

        mediosPago.put("PSE", "PSE (débito a cuenta bancaria)");
        mediosPago.put("TARJETA", "Tarjeta de crédito");
    }

    public Map<String, String> getMunicipios() { return municipios; }
    public Map<String, String> getTiposPredio() { return tiposPredio; }
    public Map<String, String> getTiposPqrs() { return tiposPqrs; }
    public Map<String, String> getMediosPago() { return mediosPago; }
}
