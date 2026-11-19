package com.gascorocora.implementations.conexion;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service Task "Notificar no viabilidad tecnica" (implementacion: Java Class).
 * Se ejecuta cuando la DMN Decision_ViabilidadTecnica devuelve NO_VIABLE.
 */
public class NotificarNoViabilidadDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(NotificarNoViabilidadDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        execution.setVariable("estadoSolicitud", "NO_VIABLE");

        LOG.info("[Conexion] Para: {} - La solicitud {} no es viable tecnicamente (distancia {} m, capacidad {}). "
                        + "Se registra como demanda potencial para el plan de expansion.",
                execution.getVariable("emailSolicitante"), execution.getVariable("numeroSolicitud"),
                execution.getVariable("distanciaRedMetros"), execution.getVariable("capacidadRedSuficiente"));
    }
}
