package com.gascorocora.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gascorocora.services.CasosService;

/** Consulta de casos de negocio (solicitud, factura, PQRS) con su bitacora y el estado del proceso. */
@RestController
@RequestMapping("/api/casos")
public class CasosController {

    private final CasosService casosService;

    public CasosController(CasosService casosService) {
        this.casosService = casosService;
    }

    @GetMapping
    public List<Map<String, Object>> recientes(@RequestParam(defaultValue = "30") int limite) {
        return casosService.recientes(limite);
    }

    @GetMapping("/{referencia}")
    public Map<String, Object> consultar(@PathVariable String referencia) {
        return casosService.consultar(referencia);
    }

    @GetMapping("/por-instancia/{processInstanceId}")
    public Map<String, Object> porInstancia(@PathVariable String processInstanceId) {
        return casosService.consultarPorInstancia(processInstanceId);
    }
}
