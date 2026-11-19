package com.gascorocora.implementations.pqrs;

import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gascorocora.implementations.comun.VariablesUtil;
import com.gascorocora.services.RecaudoClient;

/**
 * Service Task "Aplicar nota credito" del subproceso Proceso_AjusteFactura
 * (Delegate Expression #{notaCreditoDelegate}, asincrona: camunda:asyncBefore="true").
 *
 * Integracion con el servicio de Recaudo: le envia la cuenta, el valor y el radicado; el servicio
 * descuenta el valor de la obligacion pendiente y devuelve el numero de nota credito y el nuevo saldo,
 * que quedan como variables del proceso y se usan en la notificacion al usuario.
 * Si Recaudo no responde, la excepcion genera reintentos y luego un incidente visible en Cockpit.
 */
@Service("notaCreditoDelegate")
public class NotaCreditoDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(NotaCreditoDelegate.class);

    private final RecaudoClient recaudoClient;

    public NotaCreditoDelegate(RecaudoClient recaudoClient) {
        this.recaudoClient = recaudoClient;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String numeroCuenta = VariablesUtil.getString(execution, "numeroCuenta", null);
        String numeroRadicado = VariablesUtil.getString(execution, "numeroRadicado", null);
        double valorAjuste = VariablesUtil.getDouble(execution, "valorAjuste", 0);

        Map<String, Object> respuesta;
        try {
            respuesta = recaudoClient.aplicarNotaCredito(numeroCuenta, valorAjuste, numeroRadicado);
        } catch (RuntimeException e) {
            throw new IllegalStateException("El servicio de Recaudo (" + recaudoClient.getUrlBase()
                    + ") no aplico la nota credito: " + e.getMessage(), e);
        }

        execution.setVariable("numeroNotaCredito", String.valueOf(respuesta.get("numeroNotaCredito")));
        execution.setVariable("referenciaAjustada", respuesta.get("referenciaPago") == null ? null
                : String.valueOf(respuesta.get("referenciaPago")));
        execution.setVariable("saldoCuentaRecaudo", VariablesUtil.redondear(numero(respuesta.get("nuevoSaldo")), 2));
        execution.setVariable("saldoAFavor", VariablesUtil.redondear(numero(respuesta.get("saldoAFavor")), 2));
        execution.setVariable("estadoAjuste", "APLICADO");

        LOG.info("[Ajuste] Recaudo aplico la nota credito {} por ${} a la cuenta {} (radicado {}). Nuevo saldo ${}",
                respuesta.get("numeroNotaCredito"), valorAjuste, numeroCuenta, numeroRadicado, respuesta.get("nuevoSaldo"));
    }

    private static double numero(Object valor) {
        return valor instanceof Number n ? n.doubleValue() : 0;
    }
}
