package com.gascorocora.portal.usuarios.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpSession;

/** Datos disponibles en todas las plantillas: usuario en sesion, catalogos y formato. */
@ControllerAdvice
public class ModeloGlobal {

    private final Catalogos catalogos;
    private final Formato formato;

    public ModeloGlobal(Catalogos catalogos, Formato formato) {
        this.catalogos = catalogos;
        this.formato = formato;
    }

    @ModelAttribute("usuario")
    public UsuarioSesion usuario(HttpSession sesion) {
        return (UsuarioSesion) sesion.getAttribute(UsuarioSesion.ATRIBUTO);
    }

    @ModelAttribute("catalogos")
    public Catalogos catalogos() {
        return catalogos;
    }

    @ModelAttribute("fmt")
    public Formato fmt() {
        return formato;
    }
}
