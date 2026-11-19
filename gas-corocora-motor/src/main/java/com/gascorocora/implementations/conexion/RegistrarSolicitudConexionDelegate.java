package com.gascorocora.implementations.conexion;

import java.time.LocalDate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gascorocora.implementations.comun.VariablesUtil;

/**
 * Service Task "Registrar solicitud de conexion" (implementacion: Java Class).
 */
public class RegistrarSolicitudConexionDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrarSolicitudConexionDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String numeroSolicitud = VariablesUtil.generarCodigo("CON");
        execution.setVariable("numeroSolicitud", numeroSolicitud);
        execution.setVariable("fechaSolicitud", LocalDate.now().toString());
        execution.setVariable("estadoSolicitud", "RADICADA");

        LOG.info("[Conexion] Solicitud {} registrada para {} en {} ({})", numeroSolicitud,
                execution.getVariable("nombreSolicitante"), execution.getVariable("direccionPredio"),
                execution.getVariable("municipio"));
    }
}
