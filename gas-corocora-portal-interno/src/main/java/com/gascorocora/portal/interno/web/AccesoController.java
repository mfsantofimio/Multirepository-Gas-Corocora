package com.gascorocora.portal.interno.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gascorocora.portal.interno.cliente.CamundaClient;
import com.gascorocora.portal.interno.cliente.ServicioException;

import jakarta.servlet.http.HttpSession;

/** Ingreso de funcionarios con los usuarios y grupos de la identidad de Camunda. */
@Controller
public class AccesoController {

    private final CamundaClient camunda;

    public AccesoController(CamundaClient camunda) {
        this.camunda = camunda;
    }

    @GetMapping("/")
    public String inicio(HttpSession sesion) {
        return sesion.getAttribute(SesionInterna.ATRIBUTO) == null ? "redirect:/ingresar" : "redirect:/tareas";
    }

    @GetMapping("/ingresar")
    public String formulario() {
        return "ingresar";
    }

    @PostMapping("/ingresar")
    public String ingresar(@RequestParam String usuario, @RequestParam String clave, HttpSession sesion, Model modelo) {
        modelo.addAttribute("usuarioDigitado", usuario);
        if (usuario.isBlank() || clave.isBlank()) {
            modelo.addAttribute("error", "Ingrese usuario y contraseña");
            return "ingresar";
        }
        try {
            if (!camunda.verificarClave(usuario.trim(), clave)) {
                modelo.addAttribute("error", "Usuario o contraseña incorrectos");
                return "ingresar";
            }
            List<String> grupos = new ArrayList<>();
            List<String> nombres = new ArrayList<>();
            for (Map<String, Object> g : camunda.grupos(usuario.trim())) {
                grupos.add(String.valueOf(g.get("id")));
                nombres.add(String.valueOf(g.get("name")));
            }
            Map<String, Object> perfil = camunda.perfil(usuario.trim());
            String nombre = (texto(perfil.get("firstName")) + " " + texto(perfil.get("lastName"))).trim();
            sesion.setAttribute(SesionInterna.ATRIBUTO,
                    new SesionInterna(usuario.trim(), nombre.isEmpty() ? usuario.trim() : nombre, grupos, nombres));
            return "redirect:/tareas";
        } catch (ServicioException e) {
            modelo.addAttribute("error", e.getMessage());
            return "ingresar";
        }
    }

    @PostMapping("/salir")
    public String salir(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/ingresar";
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }
}
