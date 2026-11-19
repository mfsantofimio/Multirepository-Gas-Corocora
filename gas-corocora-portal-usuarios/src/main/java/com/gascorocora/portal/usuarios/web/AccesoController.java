package com.gascorocora.portal.usuarios.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gascorocora.portal.usuarios.cliente.MotorClient;
import com.gascorocora.portal.usuarios.cliente.ServicioException;
import com.gascorocora.portal.usuarios.formularios.IngresoForm;
import com.gascorocora.portal.usuarios.formularios.RegistroForm;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/** Registro, ingreso y salida del usuario externo. */
@Controller
public class AccesoController {

    private final MotorClient motor;

    public AccesoController(MotorClient motor) {
        this.motor = motor;
    }

    @GetMapping("/")
    public String inicio(HttpSession sesion) {
        return sesion.getAttribute(UsuarioSesion.ATRIBUTO) == null ? "redirect:/ingresar" : "redirect:/panel";
    }

    @GetMapping("/ingresar")
    public String formularioIngreso(Model model) {
        model.addAttribute("form", new IngresoForm());
        return "ingresar";
    }

    @PostMapping("/ingresar")
    public String ingresar(@Valid @ModelAttribute("form") IngresoForm form, BindingResult errores,
                           HttpSession sesion, Model model) {
        if (errores.hasErrors()) {
            return "ingresar";
        }
        try {
            Map<String, Object> suscriptor = motor.autenticar(form.getDocumento(), form.getClave());
            sesion.setAttribute(UsuarioSesion.ATRIBUTO, UsuarioSesion.desde(suscriptor));
            return "redirect:/panel";
        } catch (ServicioException e) {
            model.addAttribute("error", e.getMessage());
            return "ingresar";
        }
    }

    @GetMapping("/registro")
    public String formularioRegistro(Model model) {
        RegistroForm form = new RegistroForm();
        form.setMunicipio("VILLAVICENCIO");
        model.addAttribute("form", form);
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("form") RegistroForm form, BindingResult errores,
                            HttpSession sesion, Model model, RedirectAttributes redireccion) {
        if (form.getClave() != null && !form.getClave().equals(form.getConfirmacion())) {
            errores.rejectValue("confirmacion", "coincidencia", "Las claves no coinciden");
        }
        if (errores.hasErrors()) {
            return "registro";
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("documento", form.getDocumento());
        datos.put("nombre", form.getNombre());
        datos.put("email", form.getEmail());
        datos.put("telefono", form.getTelefono());
        datos.put("direccion", form.getDireccion());
        datos.put("municipio", form.getMunicipio());
        datos.put("clave", form.getClave());
        try {
            Map<String, Object> suscriptor = motor.registrar(datos);
            sesion.setAttribute(UsuarioSesion.ATRIBUTO, UsuarioSesion.desde(suscriptor));
            redireccion.addFlashAttribute("exito", "Registro exitoso. Ya puede solicitar la conexión o radicar PQRS.");
            return "redirect:/panel";
        } catch (ServicioException e) {
            model.addAttribute("error", e.getMessage());
            return "registro";
        }
    }

    @PostMapping("/salir")
    public String salir(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/ingresar";
    }
}
