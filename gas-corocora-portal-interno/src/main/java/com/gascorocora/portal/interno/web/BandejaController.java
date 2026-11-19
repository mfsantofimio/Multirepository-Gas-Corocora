package com.gascorocora.portal.interno.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gascorocora.portal.interno.cliente.CamundaClient;
import com.gascorocora.portal.interno.cliente.MotorClient;
import com.gascorocora.portal.interno.cliente.ServicioException;
import com.gascorocora.portal.interno.formularios.CampoFormulario;
import com.gascorocora.portal.interno.formularios.FormularioCamunda;

/**
 * Bandeja de tareas por rol: lista las tareas de los grupos del funcionario, muestra la informacion
 * del caso y el Camunda Form de la tarea, y la completa actualizando las variables del proceso.
 */
@Controller
public class BandejaController {

    private final CamundaClient camunda;
    private final MotorClient motor;
    private final Formato fmt;

    public BandejaController(CamundaClient camunda, MotorClient motor, Formato fmt) {
        this.camunda = camunda;
        this.motor = motor;
        this.fmt = fmt;
    }

    @GetMapping("/tareas")
    public String bandeja(@SessionAttribute(SesionInterna.ATRIBUTO) SesionInterna funcionario, Model modelo) {
        try {
            List<Map<String, Object>> tareas = camunda.tareas(funcionario.getGrupos(), funcionario.getUsuario());
            Map<String, List<Map<String, Object>>> porProceso = new TreeMap<>();
            int mias = 0;
            for (Map<String, Object> t : tareas) {
                porProceso.computeIfAbsent(fmt.proceso(t.get("processDefinitionId")), k -> new ArrayList<>()).add(t);
                if (funcionario.getUsuario().equals(t.get("assignee"))) {
                    mias++;
                }
            }
            modelo.addAttribute("porProceso", porProceso);
            modelo.addAttribute("total", tareas.size());
            modelo.addAttribute("mias", mias);
        } catch (ServicioException e) {
            modelo.addAttribute("porProceso", Collections.emptyMap());
            modelo.addAttribute("total", 0);
            modelo.addAttribute("mias", 0);
            modelo.addAttribute("error", e.getMessage());
        }
        return "tareas";
    }

    @GetMapping("/tareas/{id}")
    public String detalle(@PathVariable String id, @SessionAttribute(SesionInterna.ATRIBUTO) SesionInterna funcionario,
                          Model modelo, RedirectAttributes redireccion) {
        try {
            Map<String, Object> tarea = camunda.tarea(id);
            if (!puedeAtender(tarea, id, funcionario)) {
                redireccion.addFlashAttribute("error", "La tarea no corresponde a sus roles.");
                return "redirect:/tareas";
            }
            Map<String, Object> variables = camunda.variables(id);
            prepararDetalle(modelo, tarea, variables, leerCampos(id, variables));
            return "tarea";
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getEstado() == 404
                    ? "La tarea ya no existe: probablemente otro funcionario la completó." : e.getMessage());
            return "redirect:/tareas";
        }
    }

    @PostMapping("/tareas/{id}/completar")
    public String completar(@PathVariable String id, @RequestParam Map<String, String> enviado,
                            @SessionAttribute(SesionInterna.ATRIBUTO) SesionInterna funcionario, Model modelo, RedirectAttributes redireccion) {
        try {
            Map<String, Object> tarea = camunda.tarea(id);
            if (!puedeAtender(tarea, id, funcionario)) {
                redireccion.addFlashAttribute("error", "La tarea no corresponde a sus roles.");
                return "redirect:/tareas";
            }
            Object asignada = tarea.get("assignee");
            if (asignada != null && !funcionario.getUsuario().equals(asignada)) {
                redireccion.addFlashAttribute("error", "La tarea está reclamada por " + asignada + ".");
                return "redirect:/tareas/" + id;
            }
            Map<String, Object> variables = camunda.variables(id);
            List<CampoFormulario> campos = leerCampos(id, variables);
            Map<String, Map<String, Object>> nuevas = campos == null ? Map.of() : FormularioCamunda.convertir(campos, enviado);
            if (nuevas == null) {
                prepararDetalle(modelo, tarea, variables, campos);
                modelo.addAttribute("error", "Revise los campos marcados.");
                return "tarea";
            }
            if (asignada == null) {
                camunda.reclamar(id, funcionario.getUsuario());
            }
            camunda.completar(id, nuevas);
            redireccion.addFlashAttribute("exito", "Tarea «" + tarea.get("name") + "» completada. El proceso avanzó.");
            redireccion.addFlashAttribute("instanciaAvanzada", tarea.get("processInstanceId"));
            return "redirect:/tareas";
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", "No se pudo completar la tarea: " + e.getMessage());
            return "redirect:/tareas";
        }
    }

    @PostMapping("/tareas/{id}/reclamar")
    public String reclamar(@PathVariable String id, @SessionAttribute(SesionInterna.ATRIBUTO) SesionInterna funcionario,
                           RedirectAttributes redireccion) {
        try {
            Map<String, Object> tarea = camunda.tarea(id);
            if (!puedeAtender(tarea, id, funcionario)) {
                redireccion.addFlashAttribute("error", "La tarea no corresponde a sus roles.");
                return "redirect:/tareas";
            }
            camunda.reclamar(id, funcionario.getUsuario());
            redireccion.addFlashAttribute("exito", "Tarea reclamada: ahora está a su cargo.");
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tareas/" + id;
    }

    @PostMapping("/tareas/{id}/liberar")
    public String liberar(@PathVariable String id, @SessionAttribute(SesionInterna.ATRIBUTO) SesionInterna funcionario,
                          RedirectAttributes redireccion) {
        try {
            Map<String, Object> tarea = camunda.tarea(id);
            if (!funcionario.getUsuario().equals(tarea.get("assignee"))) {
                redireccion.addFlashAttribute("error", "Solo quien reclamó la tarea puede liberarla.");
                return "redirect:/tareas/" + id;
            }
            camunda.liberar(id);
            redireccion.addFlashAttribute("exito", "Tarea liberada: vuelve a estar disponible para su grupo.");
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tareas";
    }

    // ------------------------------------------------------------------ apoyo

    /** Puede atenderla si esta asignada a el o si comparte alguno de los grupos candidatos. */
    private boolean puedeAtender(Map<String, Object> tarea, String id, SesionInterna funcionario) {
        if (funcionario.getUsuario().equals(tarea.get("assignee"))) {
            return true;
        }
        return funcionario.tieneAlguno(camunda.gruposDeTarea(id));
    }

    /** Campos del Camunda Form de la tarea, o null si la tarea no tiene formulario desplegado. */
    private List<CampoFormulario> leerCampos(String id, Map<String, Object> variables) {
        String esquema;
        try {
            esquema = camunda.formulario(id);
        } catch (ServicioException e) {
            if (e.getEstado() >= 400 && e.getEstado() < 500) {
                return null;
            }
            throw e;
        }
        return FormularioCamunda.leer(esquema, variables);
    }

    private void prepararDetalle(Model modelo, Map<String, Object> tarea, Map<String, Object> variables,
                                 List<CampoFormulario> campos) {
        modelo.addAttribute("tarea", tarea);
        modelo.addAttribute("campos", campos);
        modelo.addAttribute("variables", new TreeMap<>(variables));
        List<String> grupos = new ArrayList<>();
        try {
            grupos = camunda.gruposDeTarea(String.valueOf(tarea.get("id")));
        } catch (ServicioException e) {
            // solo informativo
        }
        modelo.addAttribute("gruposTarea", grupos);
        Map<String, Object> caso = null;
        try {
            caso = motor.casoPorInstancia(String.valueOf(tarea.get("processInstanceId")));
        } catch (ServicioException e) {
            modelo.addAttribute("avisoCaso", e.getMessage());
        }
        modelo.addAttribute("caso", caso == null ? new LinkedHashMap<>() : caso);
        modelo.addAttribute("hayCaso", caso != null);
    }
}
