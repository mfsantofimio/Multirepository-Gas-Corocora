package com.gascorocora.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gascorocora.dto.AutenticacionRequest;
import com.gascorocora.dto.RegistroSuscriptorRequest;
import com.gascorocora.services.SuscriptorService;

import jakarta.validation.Valid;

/** API para el portal de usuarios externos: registro, ingreso y consulta de tramites. */
@RestController
@RequestMapping("/api/suscriptores")
public class SuscriptorController {

    private final SuscriptorService suscriptorService;

    public SuscriptorController(SuscriptorService suscriptorService) {
        this.suscriptorService = suscriptorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registrar(@Valid @RequestBody RegistroSuscriptorRequest registro) {
        return suscriptorService.registrar(registro);
    }

    @PostMapping("/autenticar")
    public Map<String, Object> autenticar(@Valid @RequestBody AutenticacionRequest credenciales) {
        return suscriptorService.autenticar(credenciales.documento(), credenciales.clave());
    }

    @GetMapping("/{documento}")
    public Map<String, Object> consultar(@PathVariable String documento) {
        return suscriptorService.consultar(documento);
    }

    @GetMapping("/{documento}/tramites")
    public Map<String, Object> tramites(@PathVariable String documento) {
        return suscriptorService.tramites(documento);
    }
}
