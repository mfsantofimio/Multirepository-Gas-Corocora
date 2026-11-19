package com.gascorocora.implementations.facturacion;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gascorocora.implementations.comun.VariablesUtil;

/**
 * Service Task "Generar orden de suspension" del subproceso Proceso_SuspensionServicio
 * (invocado por Call Activity). Implementacion: Java Class.
 */
public class GenerarOrdenSuspensionDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(GenerarOrdenSuspensionDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String numeroOrden = VariablesUtil.generarCodigo("SUS");
        execution.setVariable("numeroOrdenSuspension", numeroOrden);
        execution.setVariable("estadoCartera", "EN_MORA");

        LOG.info("[Suspension] Orden {} generada para la cuenta {} (factura {} sin pagar)",
                numeroOrden, execution.getVariable("numeroCuenta"), execution.getVariable("numeroFactura"));
    }
}
