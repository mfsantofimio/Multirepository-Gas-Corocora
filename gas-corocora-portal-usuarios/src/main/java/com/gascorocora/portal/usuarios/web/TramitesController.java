package com.gascorocora.portal.usuarios.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gascorocora.portal.usuarios.cliente.MotorClient;
import com.gascorocora.portal.usuarios.cliente.ServicioException;
import com.gascorocora.portal.usuarios.formularios.PqrsForm;
import com.gascorocora.portal.usuarios.formularios.SolicitudConexionForm;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Panel del usuario y tramites: solicitud de conexion (P3), PQRS (P2), firma del contrato y recurso.
 * Cada accion se valida en el portal y luego en el motor, que inicia el proceso o correlaciona el mensaje.
 */
@Controller
public class TramitesController {

    private final MotorClient motor;

    public TramitesController(MotorClient motor) {
        this.motor = motor;
    }

    private static UsuarioSesion usuario(HttpSession sesion) {
        return (UsuarioSesion) sesion.getAttribute(UsuarioSesion.ATRIBUTO);
    }

    // ------------------------------------------------------------------ panel

    @GetMapping("/panel")
    public String panel(HttpSession sesion, Model model) {
        try {
            Map<String, Object> tramites = motor.tramites(usuario(sesion).getDocumento());
            model.addAllAttributes(tramites);
        } catch (ServicioException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "panel";
    }

    @GetMapping("/tramites/{referencia}")
    public String detalle(@PathVariable String referencia, HttpSession sesion, Model model, RedirectAttributes redireccion) {
        try {
            Map<String, Object> tramites = motor.tramites(usuario(sesion).getDocumento());
            if (Propiedad.buscar(tramites, referencia) == null) {
                redireccion.addFlashAttribute("error", "El trámite " + referencia + " no pertenece a su usuario.");
                return "redirect:/panel";
            }
            model.addAttribute("caso", motor.caso(referencia));
            return "tramite";
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/panel";
        }
    }

    // ------------------------------------------------------------------ P3 conexion

    @GetMapping("/conexion/nueva")
    public String formularioConexion(HttpSession sesion, Model model) {
        SolicitudConexionForm form = new SolicitudConexionForm();
        form.setDireccionPredio(usuario(sesion).getDireccion());
        form.setMunicipio(usuario(sesion).getMunicipio());
        form.setTipoPredio("RESIDENCIAL");
        model.addAttribute("form", form);
        return "conexion";
    }

    @PostMapping("/conexion/nueva")
    public String solicitarConexion(@Valid @ModelAttribute("form") SolicitudConexionForm form, BindingResult errores,
                                    HttpSession sesion, Model model, RedirectAttributes redireccion) {
        if (errores.hasErrors()) {
            return "conexion";
        }
        UsuarioSesion u = usuario(sesion);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombreSolicitante", u.getNombre());
        datos.put("documentoIdentidad", u.getDocumento());
        datos.put("emailSolicitante", u.getEmail());
        datos.put("telefonoSolicitante", u.getTelefono());
        datos.put("direccionPredio", form.getDireccionPredio().trim());
        datos.put("municipio", form.getMunicipio());
        datos.put("tipoPredio", form.getTipoPredio());
        datos.put("estrato", form.getEstrato());
        try {
            motor.solicitarConexion(datos);
            redireccion.addFlashAttribute("exito",
                    "Su solicitud de conexión fue radicada. Un asesor revisará los documentos del predio.");
            return "redirect:/panel";
        } catch (ServicioException e) {
            model.addAttribute("error", e.getMessage());
            return "conexion";
        }
    }

    @PostMapping("/conexion/{numeroSolicitud}/firmar")
    public String firmarContrato(@PathVariable String numeroSolicitud, HttpSession sesion, RedirectAttributes redireccion) {
        try {
            Map<String, Object> solicitud = Propiedad.buscar(motor.tramites(usuario(sesion).getDocumento()), numeroSolicitud);
            if (solicitud == null || !Propiedad.tieneAccion(solicitud, "FIRMAR_CONTRATO")) {
                redireccion.addFlashAttribute("error", "La solicitud " + numeroSolicitud + " no está esperando la firma del contrato.");
                return "redirect:/panel";
            }
            motor.firmarContrato(numeroSolicitud, usuario(sesion).getNombre());
            redireccion.addFlashAttribute("exito", "Contrato firmado. Se programará la construcción de la acometida.");
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/panel";
    }

    // ------------------------------------------------------------------ P2 PQRS

    @GetMapping("/pqrs/nueva")
    public String formularioPqrs(HttpSession sesion, Model model, RedirectAttributes redireccion) {
        try {
            List<Map<String, Object>> cuentas = Propiedad.cuentas(motor.tramites(usuario(sesion).getDocumento()));
            if (cuentas.isEmpty()) {
                redireccion.addFlashAttribute("error", "Para radicar una PQRS necesita una cuenta de servicio activa.");
                return "redirect:/panel";
            }
            PqrsForm form = new PqrsForm();
            form.setNumeroCuenta(String.valueOf(cuentas.get(0).get("numeroCuenta")));
            form.setTipoSolicitud("PETICION");
            model.addAttribute("form", form);
            model.addAttribute("cuentas", cuentas);
            return "pqrs";
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/panel";
        }
    }

    @PostMapping("/pqrs/nueva")
    public String radicarPqrs(@Valid @ModelAttribute("form") PqrsForm form, BindingResult errores,
                              HttpSession sesion, Model model, RedirectAttributes redireccion) {
        UsuarioSesion u = usuario(sesion);
        List<Map<String, Object>> cuentas;
        try {
            cuentas = Propiedad.cuentas(motor.tramites(u.getDocumento()));
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/panel";
        }
        model.addAttribute("cuentas", cuentas);
        boolean cuentaPropia = cuentas.stream().anyMatch(c -> String.valueOf(c.get("numeroCuenta")).equals(form.getNumeroCuenta()));
        if (!cuentaPropia) {
            errores.rejectValue("numeroCuenta", "propia", "Seleccione una de sus cuentas");
        }
        if ("RECLAMO".equals(form.getTipoSolicitud()) && (form.getValorReclamado() == null || form.getValorReclamado() <= 0)) {
            errores.rejectValue("valorReclamado", "reclamo", "Para un reclamo indique el valor que reclama");
        }
        if (errores.hasErrors()) {
            return "pqrs";
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("numeroCuenta", form.getNumeroCuenta());
        datos.put("nombreUsuario", u.getNombre());
        datos.put("emailUsuario", u.getEmail());
        datos.put("tipoSolicitud", form.getTipoSolicitud());
        datos.put("canal", "WEB");
        datos.put("valorReclamado", form.getValorReclamado() == null ? 0.0 : form.getValorReclamado());
        datos.put("descripcion", form.getDescripcion().trim());
        try {
            motor.radicarPqrs(datos);
            redireccion.addFlashAttribute("exito",
                    "Su PQRS fue radicada. La empresa tiene 15 días hábiles para responder (art. 158 Ley 142 de 1994).");
            return "redirect:/panel";
        } catch (ServicioException e) {
            model.addAttribute("error", e.getMessage());
            return "pqrs";
        }
    }

    @PostMapping("/pqrs/{numeroRadicado}/recurso")
    public String interponerRecurso(@PathVariable String numeroRadicado, @RequestParam(defaultValue = "") String motivoRecurso,
                                    HttpSession sesion, RedirectAttributes redireccion) {
        String motivo = motivoRecurso.trim();
        if (motivo.length() < 10 || motivo.length() > 1000) {
            redireccion.addFlashAttribute("error", "Explique el motivo del recurso (entre 10 y 1000 caracteres).");
            return "redirect:/panel";
        }
        try {
            Map<String, Object> pqrs = Propiedad.buscar(motor.tramites(usuario(sesion).getDocumento()), numeroRadicado);
            if (pqrs == null || !Propiedad.tieneAccion(pqrs, "INTERPONER_RECURSO")) {
                redireccion.addFlashAttribute("error", "La PQRS " + numeroRadicado + " no admite recurso en este momento.");
                return "redirect:/panel";
            }
            motor.interponerRecurso(numeroRadicado, motivo);
            redireccion.addFlashAttribute("exito", "Recurso de reposición radicado sobre la PQRS " + numeroRadicado + ".");
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/panel";
    }
}
