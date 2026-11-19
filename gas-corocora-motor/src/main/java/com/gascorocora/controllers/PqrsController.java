package com.gascorocora.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gascorocora.dto.MensajeCorrelacionadoResponse;
import com.gascorocora.dto.ProcesoIniciadoResponse;
import com.gascorocora.dto.RadicarPqrsRequest;
import com.gascorocora.dto.RecursoReposicionRequest;
import com.gascorocora.services.PqrsService;

import jakarta.validation.Valid;

/** API REST del proceso Atencion al Usuario y PQRS (canal web). */
@RestController
@RequestMapping("/api/pqrs")
public class PqrsController {

    private final PqrsService pqrsService;

    public PqrsController(PqrsService pqrsService) {
        this.pqrsService = pqrsService;
    }

    /** Radica una PQRS por el canal web. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProcesoIniciadoResponse radicar(@Valid @RequestBody RadicarPqrsRequest solicitud) {
        return pqrsService.radicar(solicitud);
    }

    /** El usuario interpone recurso de reposicion: dispara el evento de mensaje "Recurso de reposicion recibido". */
    @PostMapping("/{numeroRadicado}/recursos")
    public MensajeCorrelacionadoResponse presentarRecurso(@PathVariable String numeroRadicado,
                                                          @Valid @RequestBody RecursoReposicionRequest recurso) {
        return pqrsService.presentarRecurso(numeroRadicado, recurso);
    }
}
