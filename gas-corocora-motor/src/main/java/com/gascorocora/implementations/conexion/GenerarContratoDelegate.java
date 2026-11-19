package com.gascorocora.implementations.conexion;

import java.time.LocalDate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gascorocora.implementations.comun.VariablesUtil;

/**
 * Service Task "Generar contrato de condiciones uniformes" (implementacion: Java Class).
 * Corre en paralelo (compuerta AND) con el diseno de la acometida.
 */
public class GenerarContratoDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(GenerarContratoDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String numeroContrato = VariablesUtil.generarCodigo("CCU");
        execution.setVariable("numeroContrato", numeroContrato);
        execution.setVariable("fechaContrato", LocalDate.now().toString());
        execution.setVariable("estadoSolicitud", "CONTRATO_GENERADO");

        LOG.info("[Conexion] Contrato de condiciones uniformes {} generado para {} (documento {})",
                numeroContrato, execution.getVariable("nombreSolicitante"),
                execution.getVariable("documentoIdentidad"));
    }
}
