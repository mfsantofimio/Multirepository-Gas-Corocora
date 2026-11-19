package com.gascorocora.listeners;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.gascorocora.config.ProcesosConstantes;
import com.gascorocora.persistence.NegocioRepository;

/**
 * Proyeccion de datos de negocio a partir de los eventos del motor (Spring Eventing Bridge de Camunda).
 *
 * - Cada vez que termina una actividad, actualiza la fila del caso (solicitud, factura o PQRS)
 *   con las variables actuales del proceso, vinculada por PROCESS_INSTANCE_ID.
 * - Registra en GC_EVENTO_CASO las tareas creadas/asignadas/completadas y las actividades automaticas.
 *
 * Nunca lanza excepciones: un error de persistencia se registra en el log y el proceso continua.
 */
@Component
public class SincronizacionNegocioListener {

    private static final Logger LOG = LoggerFactory.getLogger(SincronizacionNegocioListener.class);

    private final NegocioRepository repositorio;

    public SincronizacionNegocioListener(NegocioRepository repositorio) {
        this.repositorio = repositorio;
    }

    // ================================================================== ejecuciones

    @EventListener
    public void alEjecutar(DelegateExecution execution) {
        try {
            if (!"end".equals(execution.getEventName()) || execution.getCurrentActivityId() == null) {
                return;
            }
            DelegateExecution raiz = raiz(execution);
            String procesoRaiz = clave(raiz.getProcessDefinitionId());
            Map<String, Object> v = execution.getVariables();

            sincronizarCaso(procesoRaiz, raiz.getProcessInstanceId(), v);
            registrarActividad(execution, raiz, v);
        } catch (Exception e) {
            LOG.warn("[Persistencia] No se pudo sincronizar la actividad {}: {}", execution.getCurrentActivityId(), e.getMessage());
        }
    }

    // ================================================================== tareas de usuario

    @EventListener
    public void alCambiarTarea(DelegateTask tarea) {
        try {
            String evento;
            switch (tarea.getEventName()) {
                case "create" -> evento = "TAREA_CREADA";
                case "assignment" -> evento = "TAREA_ASIGNADA";
                case "complete" -> evento = "TAREA_COMPLETADA";
                default -> {
                    return;
                }
            }
            DelegateExecution execution = tarea.getExecution();
            if (execution == null) {
                return;
            }
            DelegateExecution raiz = raiz(execution);
            String grupos = tarea.getCandidates().stream()
                    .map(IdentityLink::getGroupId)
                    .filter(g -> g != null)
                    .collect(Collectors.joining(", "));
            String detalle = "TAREA_CREADA".equals(evento) ? "Rol: " + grupos : null;
            repositorio.registrarEvento(raiz.getProcessInstanceId(), execution.getProcessInstanceId(),
                    clave(execution.getProcessDefinitionId()), referencia(execution.getVariables(), raiz),
                    evento, tarea.getName(), tarea.getAssignee(), detalle);
        } catch (Exception e) {
            LOG.warn("[Persistencia] No se pudo registrar el evento de la tarea {}: {}", tarea.getName(), e.getMessage());
        }
    }

    // ================================================================== proyeccion por proceso

    private void sincronizarCaso(String procesoRaiz, String rootPi, Map<String, Object> v) {
        switch (procesoRaiz) {
            case ProcesosConstantes.PROCESO_CONEXION_NUEVOS_USUARIOS -> sincronizarSolicitud(rootPi, v);
            case ProcesosConstantes.PROCESO_MEDICION_FACTURACION -> sincronizarFactura(rootPi, v);
            case ProcesosConstantes.PROCESO_ATENCION_PQRS -> sincronizarPqrs(rootPi, v);
            default -> {
                // procesos sin proyeccion de negocio
            }
        }
    }

    private void sincronizarSolicitud(String rootPi, Map<String, Object> v) {
        String numeroSolicitud = NegocioRepository.texto(v.get("numeroSolicitud"));
        if (numeroSolicitud == null) {
            return;
        }
        String documento = NegocioRepository.texto(v.get("documentoIdentidad"));
        repositorio.asegurarSuscriptor(documento, NegocioRepository.texto(v.get("nombreSolicitante")),
                NegocioRepository.texto(v.get("emailSolicitante")), NegocioRepository.texto(v.get("telefonoSolicitante")),
                NegocioRepository.texto(v.get("direccionPredio")), NegocioRepository.texto(v.get("municipio")));

        String estado = NegocioRepository.texto(v.get("estadoSolicitud"));
        if ("ESPERANDO_FIRMA".equals(estado) && v.get("fechaFirmaContrato") != null) {
            estado = "EN_CONSTRUCCION";
        }
        if (v.get("accionCorrectiva") != null && !"CONECTADA".equals(estado)) {
            estado = "OBRA_ESCALADA";
        }

        Map<String, Object> c = new LinkedHashMap<>();
        c.put("documento", documento);
        c.put("process_instance_id", rootPi);
        c.put("direccion_predio", NegocioRepository.texto(v.get("direccionPredio")));
        c.put("municipio", NegocioRepository.texto(v.get("municipio")));
        c.put("tipo_predio", NegocioRepository.texto(v.get("tipoPredio")));
        c.put("estrato", NegocioRepository.entero(v.get("estrato")));
        c.put("distancia_red_metros", NegocioRepository.decimal(v.get("distanciaRedMetros")));
        c.put("viabilidad", NegocioRepository.texto(v.get("viabilidad")));
        c.put("valor_cotizacion", NegocioRepository.decimal(v.get("valorCotizacion")));
        c.put("numero_contrato", NegocioRepository.texto(v.get("numeroContrato")));
        c.put("numero_certificado", NegocioRepository.texto(v.get("numeroCertificado")));
        c.put("numero_cuenta", NegocioRepository.texto(v.get("numeroCuenta")));
        c.put("estado", estado);
        repositorio.upsert("gc_solicitud_conexion", "numero_solicitud", numeroSolicitud, c);

        // La matricula crea la cuenta con la que el usuario entra al ciclo de facturacion
        String numeroCuenta = NegocioRepository.texto(v.get("numeroCuenta"));
        if (numeroCuenta != null && "CONECTADA".equals(estado)) {
            repositorio.asegurarCuenta(numeroCuenta, documento, NegocioRepository.texto(v.get("tipoPredio")),
                    NegocioRepository.entero(v.get("estrato")), NegocioRepository.texto(v.get("direccionPredio")),
                    null, BigDecimal.ZERO, numeroSolicitud);
        }
    }

    private void sincronizarFactura(String rootPi, Map<String, Object> v) {
        String numeroFactura = NegocioRepository.texto(v.get("numeroFactura"));
        String numeroCuenta = NegocioRepository.texto(v.get("numeroCuenta"));
        if (numeroFactura == null || numeroCuenta == null) {
            return;
        }
        repositorio.asegurarCuenta(numeroCuenta, null, NegocioRepository.texto(v.get("tipoUsuario")),
                NegocioRepository.entero(v.get("estrato")), null,
                NegocioRepository.decimal(v.get("consumoPromedioM3")), null, null);

        String estado;
        if (v.get("fechaPago") != null) {
            BigDecimal saldo = NegocioRepository.decimal(v.get("saldoPendiente"));
            estado = saldo != null && saldo.signum() > 0 ? "PAGO_PARCIAL" : "PAGADA";
        } else if (v.get("numeroOrdenSuspension") != null) {
            estado = Boolean.TRUE.equals(NegocioRepository.booleano(v.get("suspensionEjecutada"))) ? "SUSPENDIDA" : "EN_MORA";
        } else if (Boolean.TRUE.equals(NegocioRepository.booleano(v.get("facturaEnviada")))) {
            estado = "PENDIENTE_PAGO";
        } else {
            estado = "EN_REVISION";
        }

        Map<String, Object> c = new LinkedHashMap<>();
        c.put("numero_cuenta", numeroCuenta);
        c.put("process_instance_id", rootPi);
        c.put("lectura_anterior", NegocioRepository.decimal(v.get("lecturaAnterior")));
        c.put("lectura_actual", NegocioRepository.decimal(v.get("lecturaActual")));
        c.put("consumo_m3", NegocioRepository.decimal(v.get("consumoM3")));
        c.put("porcentaje_ajuste", NegocioRepository.decimal(v.get("porcentajeAjuste")));
        c.put("total_pagar", NegocioRepository.decimal(v.get("totalPagar")));
        c.put("fecha_emision", NegocioRepository.fecha(v.get("fechaEmision")));
        c.put("fecha_limite_pago", NegocioRepository.fecha(v.get("fechaLimitePago")));
        c.put("referencia_pago", NegocioRepository.texto(v.get("referenciaPago")));
        c.put("valor_pagado", NegocioRepository.decimal(v.get("valorPagado")));
        c.put("medio_pago", NegocioRepository.texto(v.get("medioPago")));
        c.put("fecha_pago", NegocioRepository.fechaHora(v.get("fechaPago")));
        c.put("numero_orden_suspension", NegocioRepository.texto(v.get("numeroOrdenSuspension")));
        c.put("estado", estado);
        repositorio.upsert("gc_factura", "numero_factura", numeroFactura, c);

        repositorio.actualizarCuenta(numeroCuenta, NegocioRepository.decimal(v.get("lecturaActual")),
                "SUSPENDIDA".equals(estado) ? "SUSPENDIDA" : null);
    }

    private void sincronizarPqrs(String rootPi, Map<String, Object> v) {
        String numeroRadicado = NegocioRepository.texto(v.get("numeroRadicado"));
        String numeroCuenta = NegocioRepository.texto(v.get("numeroCuenta"));
        if (numeroRadicado == null || numeroCuenta == null) {
            return;
        }
        repositorio.asegurarCuenta(numeroCuenta, null, null, null, null, null, null, null);

        String estado = NegocioRepository.texto(v.get("estadoPqrs"));
        Boolean decisionConfirmada = NegocioRepository.booleano(v.get("decisionConfirmada"));
        if (v.get("motivoRecurso") != null && "RESPONDIDA".equals(estado)) {
            estado = decisionConfirmada == null ? "RECURSO_EN_TRAMITE" : (decisionConfirmada ? "RECURSO_CONFIRMADO" : "RECURSO_A_FAVOR");
        }

        Map<String, Object> c = new LinkedHashMap<>();
        c.put("numero_cuenta", numeroCuenta);
        c.put("process_instance_id", rootPi);
        c.put("tipo_solicitud", NegocioRepository.texto(v.get("tipoSolicitud")));
        c.put("canal", NegocioRepository.texto(v.get("canal")));
        c.put("descripcion", NegocioRepository.texto(v.get("descripcion")));
        c.put("valor_reclamado", NegocioRepository.decimal(v.get("valorReclamado")));
        c.put("ruta_atencion", NegocioRepository.texto(v.get("rutaAtencion")));
        c.put("respuesta", NegocioRepository.texto(v.get("respuestaUsuario")));
        c.put("procede_ajuste", NegocioRepository.booleano(v.get("procedeAjuste")));
        c.put("valor_ajuste", NegocioRepository.decimal(v.get("valorAjuste")));
        c.put("numero_nota_credito", NegocioRepository.texto(v.get("numeroNotaCredito")));
        c.put("motivo_recurso", NegocioRepository.texto(v.get("motivoRecurso")));
        c.put("decision_confirmada", decisionConfirmada);
        c.put("estado", estado);
        c.put("fecha_radicacion", NegocioRepository.fecha(v.get("fechaRadicacion")));
        c.put("fecha_limite_respuesta", NegocioRepository.fecha(v.get("fechaLimiteRespuesta")));
        repositorio.upsert("gc_pqrs", "numero_radicado", numeroRadicado, c);
    }

    // ================================================================== bitacora de actividades

    private void registrarActividad(DelegateExecution execution, DelegateExecution raiz, Map<String, Object> v) {
        FlowElement elemento = execution.getBpmnModelElementInstance();
        String nombre = execution.getCurrentActivityName();
        if (elemento == null || nombre == null || elemento instanceof Gateway || elemento instanceof UserTask) {
            return;
        }
        boolean esSubproceso = !raiz.getProcessInstanceId().equals(execution.getProcessInstanceId());
        String evento;
        if (elemento instanceof StartEvent) {
            evento = esSubproceso ? "SUBPROCESO_INICIADO" : "PROCESO_INICIADO";
        } else if (elemento instanceof EndEvent) {
            evento = esSubproceso ? "SUBPROCESO_FINALIZADO" : "PROCESO_FINALIZADO";
        } else if (elemento instanceof BoundaryEvent || elemento instanceof IntermediateCatchEvent
                || elemento instanceof IntermediateThrowEvent) {
            evento = "EVENTO";
        } else if (elemento instanceof CallActivity) {
            evento = "SUBPROCESO";
        } else {
            evento = "TAREA_AUTOMATICA";
        }
        repositorio.registrarEvento(raiz.getProcessInstanceId(), execution.getProcessInstanceId(),
                clave(execution.getProcessDefinitionId()), referencia(v, raiz), evento, nombre, null, null);
    }

    // ================================================================== utilidades

    /** Instancia raiz: sube por las call activities hasta el proceso principal. */
    private static DelegateExecution raiz(DelegateExecution execution) {
        DelegateExecution actual = execution.getProcessInstance();
        while (actual.getSuperExecution() != null) {
            actual = actual.getSuperExecution().getProcessInstance();
        }
        return actual;
    }

    /** "Proceso_X:3:abc" -> "Proceso_X" */
    private static String clave(String processDefinitionId) {
        if (processDefinitionId == null) {
            return "";
        }
        int i = processDefinitionId.indexOf(':');
        return i < 0 ? processDefinitionId : processDefinitionId.substring(0, i);
    }

    private static String referencia(Map<String, Object> v, DelegateExecution raiz) {
        for (String llave : new String[] {"numeroSolicitud", "numeroRadicado", "numeroFactura"}) {
            Object valor = v.get(llave);
            if (valor != null) {
                return valor.toString();
            }
        }
        return raiz.getProcessBusinessKey();
    }
}
