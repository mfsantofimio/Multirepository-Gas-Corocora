package com.gascorocora.implementations.conexion;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service Task "Notificar devolucion de la solicitud" (implementacion: Java Class).
 */
public class NotificarDevolucionDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(NotificarDevolucionDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        execution.setVariable("estadoSolicitud", "DEVUELTA");

        LOG.info("[Conexion] Para: {} - Su solicitud {} fue devuelta por documentos incompletos. Observaciones: {}",
                execution.getVariable("emailSolicitante"), execution.getVariable("numeroSolicitud"),
                execution.getVariable("observacionesDocumentos"));
    }
}
