package com.gascorocora.implementations.pqrs;

import java.time.LocalDate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gascorocora.implementations.comun.VariablesUtil;

/**
 * Service Task "Radicar PQRS y calcular termino legal" (implementacion: Java Class).
 * Asigna el numero de radicado y calcula la fecha limite de respuesta: 15 dias habiles
 * (art. 158 de la Ley 142 de 1994).
 */
public class RadicarPqrsDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(RadicarPqrsDelegate.class);
    static final int DIAS_HABILES_RESPUESTA = 15;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LocalDate hoy = LocalDate.now();
        String numeroRadicado = VariablesUtil.generarCodigo("PQRS");

        execution.setVariable("numeroRadicado", numeroRadicado);
        execution.setVariable("fechaRadicacion", hoy.toString());
        execution.setVariable("fechaLimiteRespuesta",
                VariablesUtil.sumarDiasHabiles(hoy, DIAS_HABILES_RESPUESTA).toString());
        execution.setVariable("estadoPqrs", "RADICADA");

        // Valores por defecto para que la DMN y las compuertas siempre tengan dato
        execution.setVariable("valorReclamado", VariablesUtil.getDouble(execution, "valorReclamado", 0));
        execution.setVariable("procedeAjuste", false);
        execution.setVariable("valorAjuste", 0.0);

        LOG.info("[PQRS] Radicado {} ({}) para la cuenta {}. Respuesta antes de {}",
                numeroRadicado, execution.getVariable("tipoSolicitud"), execution.getVariable("numeroCuenta"),
                execution.getVariable("fechaLimiteRespuesta"));
    }
}
