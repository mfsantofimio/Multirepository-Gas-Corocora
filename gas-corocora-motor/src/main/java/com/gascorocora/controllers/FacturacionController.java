package com.gascorocora.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gascorocora.dto.IniciarCicloFacturacionRequest;
import com.gascorocora.dto.MensajeCorrelacionadoResponse;
import com.gascorocora.dto.PagoRecibidoRequest;
import com.gascorocora.dto.ProcesoIniciadoResponse;
import com.gascorocora.services.FacturacionService;

import jakarta.validation.Valid;

/** API REST del proceso Gestion de Medicion y Facturacion. */
@RestController
@RequestMapping("/api/facturacion")
public class FacturacionController {

    private final FacturacionService facturacionService;

    public FacturacionController(FacturacionService facturacionService) {
        this.facturacionService = facturacionService;
    }

    /** Inicia el ciclo de facturacion de una cuenta (equivale al formulario de inicio en Tasklist). */
    @PostMapping("/ciclos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcesoIniciadoResponse iniciarCiclo(@Valid @RequestBody IniciarCicloFacturacionRequest solicitud) {
        return facturacionService.iniciarCiclo(solicitud);
    }

    /** La entidad recaudadora confirma el pago: dispara el evento de mensaje "Pago recibido". */
    @PostMapping("/facturas/{numeroFactura}/pagos")
    public MensajeCorrelacionadoResponse registrarPago(@PathVariable String numeroFactura,
                                                       @Valid @RequestBody PagoRecibidoRequest pago) {
        return facturacionService.registrarPago(numeroFactura, pago);
    }
}
