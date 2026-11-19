package com.gascorocora.portal.interno.web;

import java.util.Collections;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import com.gascorocora.portal.interno.cliente.MotorClient;
import com.gascorocora.portal.interno.cliente.ServicioException;

/** Consulta de casos de negocio (solicitudes, facturas, PQRS) con su estado vivo y su bitacora. */
@Controller
public class CasosController {

    private final MotorClient motor;

    public CasosController(MotorClient motor) {
        this.motor = motor;
    }

    @GetMapping("/casos")
    public String lista(@RequestParam(required = false) String referencia, Model modelo) {
        if (referencia != null && !referencia.isBlank()) {
            return "redirect:/casos/" + UriUtils.encodePathSegment(referencia.trim(), StandardCharsets.UTF_8);
        }
        try {
            modelo.addAttribute("casos", motor.recientes(60));
        } catch (ServicioException e) {
            modelo.addAttribute("casos", Collections.emptyList());
            modelo.addAttribute("error", e.getMessage());
        }
        return "casos";
    }

    @GetMapping("/casos/{referencia}")
    public String detalle(@PathVariable String referencia, Model modelo, RedirectAttributes redireccion) {
        try {
            modelo.addAttribute("caso", motor.caso(referencia));
            return "caso";
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/casos";
        }
    }

    @GetMapping("/casos/instancia/{processInstanceId}")
    public String porInstancia(@PathVariable String processInstanceId, Model modelo, RedirectAttributes redireccion) {
        try {
            modelo.addAttribute("caso", motor.casoPorInstancia(processInstanceId));
            return "caso";
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/casos";
        }
    }
}
