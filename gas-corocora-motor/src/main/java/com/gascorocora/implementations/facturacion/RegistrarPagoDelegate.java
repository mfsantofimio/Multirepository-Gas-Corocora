package com.gascorocora.implementations.facturacion;

import java.time.LocalDateTime;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gascorocora.implementations.comun.VariablesUtil;

/**
 * Service Task "Registrar pago y actualizar cartera" (implementacion: Java Class).
 * Se ejecuta despues de que llega el mensaje MensajePagoRecibido, que trae valorPagado.
 */
public class RegistrarPagoDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrarPagoDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        double totalPagar = VariablesUtil.getDouble(execution, "totalPagar", 0);
        double valorPagado = VariablesUtil.getDouble(execution, "valorPagado", 0);
        long saldoPendiente = Math.round(Math.max(totalPagar - valorPagado, 0));

        execution.setVariable("saldoPendiente", saldoPendiente);
        execution.setVariable("estadoCartera", saldoPendiente == 0 ? "AL_DIA" : "SALDO_PENDIENTE");
        execution.setVariable("fechaPago", LocalDateTime.now().toString());

        LOG.info("[Facturacion] Pago registrado para factura {}: pagado ${}, saldo ${}",
                execution.getVariable("numeroFactura"), valorPagado, saldoPendiente);
    }
}
