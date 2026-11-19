package com.gascorocora.portal.interno.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpSession;

/** Datos disponibles en todas las plantillas: funcionario en sesion y ayudas de formato. */
@ControllerAdvice
public class ModeloGlobal {

    private final Formato formato;

    public ModeloGlobal(Formato formato) {
        this.formato = formato;
    }

    @ModelAttribute("funcionario")
    public SesionInterna funcionario(HttpSession sesion) {
        return (SesionInterna) sesion.getAttribute(SesionInterna.ATRIBUTO);
    }

    @ModelAttribute("fmt")
    public Formato fmt() {
        return formato;
    }
}
