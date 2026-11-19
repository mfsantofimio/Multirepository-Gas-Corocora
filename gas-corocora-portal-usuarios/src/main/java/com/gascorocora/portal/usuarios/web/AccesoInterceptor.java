package com.gascorocora.portal.usuarios.web;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Exige sesion iniciada en todas las paginas salvo ingreso y registro (ver WebConfig). */
public class AccesoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession sesion = request.getSession(false);
        if (sesion == null || sesion.getAttribute(UsuarioSesion.ATRIBUTO) == null) {
            response.sendRedirect(request.getContextPath() + "/ingresar");
            return false;
        }
        return true;
    }
}
