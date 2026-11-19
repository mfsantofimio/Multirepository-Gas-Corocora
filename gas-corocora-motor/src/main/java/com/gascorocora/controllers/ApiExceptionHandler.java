package com.gascorocora.controllers;

import java.util.stream.Collectors;

import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.gascorocora.dto.ErrorResponse;

/** Traduce las excepciones del motor y de validacion a respuestas HTTP claras. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MismatchingMessageCorrelationException.class)
    public ResponseEntity<ErrorResponse> mensajeSinDestino(MismatchingMessageCorrelationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(404,
                "Ninguna instancia esta esperando este mensaje",
                "Verifique el numero enviado y que el proceso este detenido en el evento de mensaje. " + ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> datosInvalidos(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Datos invalidos", detalle));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> errorDeNegocio(ResponseStatusException ex) {
        int estado = ex.getStatusCode().value();
        return ResponseEntity.status(estado).body(ErrorResponse.of(estado, "Solicitud no valida", ex.getReason()));
    }

    @ExceptionHandler(ProcessEngineException.class)
    public ResponseEntity<ErrorResponse> errorMotor(ProcessEngineException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(409,
                "Error del motor de procesos", ex.getMessage()));
    }
}
