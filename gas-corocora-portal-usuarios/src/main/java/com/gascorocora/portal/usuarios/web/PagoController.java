package com.gascorocora.portal.usuarios.web;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gascorocora.portal.usuarios.cliente.MotorClient;
import com.gascorocora.portal.usuarios.cliente.RecaudoClient;
import com.gascorocora.portal.usuarios.cliente.ServicioException;
import com.gascorocora.portal.usuarios.formularios.PagoForm;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Pago en linea de una factura. El pago lo registra el servicio de Recaudo, que a su vez
 * avisa al motor (mensaje "Pago recibido") para que el proceso P1 continue.
 */
@Controller
public class PagoController {

    private final MotorClient motor;
    private final RecaudoClient recaudo;

    public PagoController(MotorClient motor, RecaudoClient recaudo) {
        this.motor = motor;
        this.recaudo = recaudo;
    }

    @GetMapping("/facturas/{numeroFactura}/pagar")
    public String formularioPago(@PathVariable String numeroFactura, HttpSession sesion, Model model,
                                 RedirectAttributes redireccion) {
        try {
            Map<String, Object> obligacion = cargar(numeroFactura, sesion, model);
            PagoForm form = new PagoForm();
            form.setValor(((Number) obligacion.get("saldo")).doubleValue());
            model.addAttribute("form", form);
            return "pagar";
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/panel";
        }
    }

    @PostMapping("/facturas/{numeroFactura}/pagar")
    public String pagar(@PathVariable String numeroFactura, @Valid @ModelAttribute("form") PagoForm form,
                        BindingResult errores, HttpSession sesion, Model model, RedirectAttributes redireccion) {
        Map<String, Object> obligacion;
        try {
            obligacion = cargar(numeroFactura, sesion, model);
        } catch (ServicioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/panel";
        }
        double saldo = ((Number) obligacion.get("saldo")).doubleValue();
        if (form.getValor() != null && form.getValor() > saldo) {
            errores.rejectValue("valor", "saldo", "No puede pagar más del saldo pendiente");
        }
        if (errores.hasErrors()) {
            return "pagar";
        }
        try {
            Map<String, Object> resultado = recaudo.pagar(String.valueOf(obligacion.get("referenciaPago")),
                    form.getValor(), form.getMedioPago());
            model.addAttribute("resultado", resultado);
            return "pago-resultado";
        } catch (ServicioException e) {
            model.addAttribute("error", e.getMessage());
            return "pagar";
        }
    }

    /** Verifica que la factura sea del usuario y este esperando pago; carga la obligacion de Recaudo. */
    private Map<String, Object> cargar(String numeroFactura, HttpSession sesion, Model model) {
        UsuarioSesion usuario = (UsuarioSesion) sesion.getAttribute(UsuarioSesion.ATRIBUTO);
        Map<String, Object> factura = Propiedad.buscar(motor.tramites(usuario.getDocumento()), numeroFactura);
        if (factura == null) {
            throw new ServicioException(403, "La factura " + numeroFactura + " no pertenece a sus cuentas.");
        }
        if (!Propiedad.tieneAccion(factura, "PAGAR")) {
            throw new ServicioException(409, "La factura " + numeroFactura + " no está esperando pago en este momento.");
        }
        Map<String, Object> obligacion;
        try {
            obligacion = recaudo.obligacion(numeroFactura);
        } catch (ServicioException e) {
            if (e.getEstado() == 404) {
                throw new ServicioException(404, "La factura aún no tiene referencia de pago en Recaudo. Intente en unos segundos.");
            }
            throw e;
        }
        model.addAttribute("factura", factura);
        model.addAttribute("obligacion", obligacion);
        return obligacion;
    }
}
