package com.gascorocora.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gascorocora.services.ConsultaProcesosService;

/** Consulta transversal del estado y las variables de cualquier instancia de proceso. */
@RestController
@RequestMapping("/api/procesos")
public class ProcesosController {

    private final ConsultaProcesosService consultaProcesosService;

    public ProcesosController(ConsultaProcesosService consultaProcesosService) {
        this.consultaProcesosService = consultaProcesosService;
    }

    @GetMapping("/{processInstanceId}")
    public ResponseEntity<Map<String, Object>> consultar(@PathVariable String processInstanceId) {
        Map<String, Object> instancia = consultaProcesosService.consultar(processInstanceId);
        return instancia == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(instancia);
    }
}
