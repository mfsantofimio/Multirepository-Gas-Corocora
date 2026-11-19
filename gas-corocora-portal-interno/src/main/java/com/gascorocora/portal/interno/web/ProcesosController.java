package com.gascorocora.portal.interno.web;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
 * Inicio de procesos por parte de funcionarios (canal presencial/telefonico), con el formulario de inicio
 * desplegado en el motor. Cada proceso solo lo puede iniciar el rol responsable.
 */
@Controller
public class ProcesosController {

    /** Proceso que se puede iniciar desde el portal interno. */
    public record ProcesoIniciable(String clave, String nombre, String descripcion, String grupo, String campoBusinessKey) {
        public String getClave() { return clave; }
        public String getNombre() { return nombre; }
        public String getDescripcion() { return descripcion; }
        public String getGrupo() { return grupo; }
    }

    static final List<ProcesoIniciable> PROCESOS = List.of(
            new ProcesoIniciable("Proceso_ConexionNuevosUsuarios", "Conexión de nuevos usuarios",
                    "Radicar en ventanilla la solicitud de conexión de un predio.", "asesoresComerciales", "documentoIdentidad"),
            new ProcesoIniciable("Proceso_MedicionFacturacion", "Medición y facturación",
                    "Abrir el ciclo de facturación mensual de una cuenta.", "analistasFacturacion", "numeroCuenta"),
            new ProcesoIniciable("Proceso_AtencionPqrs", "Atención de PQRS",
                    "Radicar una petición, queja, reclamo o sugerencia recibida por teléfono o en oficina.",
                    "agentesServicio", "numeroCuenta"));

    private final CamundaClient camunda;
    private final MotorClient motor;

    public ProcesosController(CamundaClient camunda, MotorClient motor) {
        this.camunda = camunda;
        this.motor = motor;
    }

    @GetMapping("/procesos")
    public String lista(@SessionAttribute(SesionInterna.ATRIBUTO) SesionInterna funcionario, Model modelo) {
        modelo.addAttribute("procesos", PROCESOS.stream().filter(p -> funcionario.tieneGrupo(p.grupo())).toList());
        return "procesos";
    }

    @GetMapping("/procesos/{clave}/iniciar")
    public String formulario(@PathVariable String clave, @SessionAttribute(SesionInterna.ATRIBUTO) SesionInterna funcionario,
                             Model modelo, RedirectAttributes redireccion) {
        ProcesoIniciable proceso = buscar(clave, funcionario);
        if (proceso == null) {
            redireccion.addFlashAttribute("error", "Su rol no puede iniciar ese proceso.");
            return "redirect:/procesos";
        }
        try {
            modelo.addAttribute("proceso", proceso);
            modelo.addAttribute("campos", FormularioCamunda.leer(camunda.formularioInicio(clave), Collections.emptyMap()));
            return "iniciar";
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/procesos";
        }
    }

    @PostMapping("/procesos/{clave}/iniciar")
    public String iniciar(@PathVariable String clave, @RequestParam Map<String, String> enviado,
                          @SessionAttribute(SesionInterna.ATRIBUTO) SesionInterna funcionario,
                          Model modelo, RedirectAttributes redireccion) {
        ProcesoIniciable proceso = buscar(clave, funcionario);
        if (proceso == null) {
            redireccion.addFlashAttribute("error", "Su rol no puede iniciar ese proceso.");
            return "redirect:/procesos";
        }
        try {
            List<CampoFormulario> campos = FormularioCamunda.leer(camunda.formularioInicio(clave), Collections.emptyMap());
            Map<String, Map<String, Object>> variables = FormularioCamunda.convertir(campos, enviado);
            if (variables == null) {
                modelo.addAttribute("proceso", proceso);
                modelo.addAttribute("campos", campos);
                modelo.addAttribute("error", "Revise los campos marcados.");
                return "iniciar";
            }
            Map<String, Object> businessKey = variables.get(proceso.campoBusinessKey());
            Map<String, Object> instancia = camunda.iniciar(clave, variables,
                    businessKey == null ? null : String.valueOf(businessKey.get("value")));
            String pi = String.valueOf(instancia.get("id"));
            try {
                Map<String, Object> caso = motor.casoPorInstancia(pi);
                redireccion.addFlashAttribute("exito", "Proceso «" + proceso.nombre() + "» iniciado: caso " + caso.get("referencia") + ".");
                return "redirect:/casos/instancia/" + pi;
            } catch (ServicioException sinCaso) {
                // P1 genera el numero de factura mas adelante (al liquidar): el caso aun no existe
                redireccion.addFlashAttribute("exito", "Proceso «" + proceso.nombre() + "» iniciado (instancia " + pi
                        + "). El caso de negocio aparecerá cuando el proceso genere su referencia.");
                return "redirect:/tareas";
            }
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", "No se pudo iniciar el proceso: " + e.getMessage());
            return "redirect:/procesos";
        }
    }

    private static ProcesoIniciable buscar(String clave, SesionInterna funcionario) {
        return PROCESOS.stream()
                .filter(p -> p.clave().equals(clave) && funcionario.tieneGrupo(p.grupo()))
                .findFirst().orElse(null);
    }
}
