package com.gascorocora.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gascorocora.dto.ContratoFirmadoRequest;
import com.gascorocora.dto.MensajeCorrelacionadoResponse;
import com.gascorocora.dto.ProcesoIniciadoResponse;
import com.gascorocora.dto.SolicitudConexionRequest;
import com.gascorocora.services.ConexionService;

import jakarta.validation.Valid;

/** API REST del proceso Gestion de Conexion de Nuevos Usuarios. */
@RestController
@RequestMapping("/api/conexiones")
public class ConexionController {

    private final ConexionService conexionService;

    public ConexionController(ConexionService conexionService) {
        this.conexionService = conexionService;
    }

    /** Un usuario potencial solicita la conexion al servicio de gas natural. */
    @PostMapping("/solicitudes")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcesoIniciadoResponse solicitarConexion(@Valid @RequestBody SolicitudConexionRequest solicitud) {
        return conexionService.solicitarConexion(solicitud);
    }

    /** El usuario firma el contrato: dispara el evento de mensaje "Contrato firmado por el usuario". */
    @PostMapping("/solicitudes/{numeroSolicitud}/firma-contrato")
    public MensajeCorrelacionadoResponse registrarFirmaContrato(@PathVariable String numeroSolicitud,
                                                                @Valid @RequestBody ContratoFirmadoRequest firma) {
        return conexionService.registrarFirmaContrato(numeroSolicitud, firma);
    }
}
