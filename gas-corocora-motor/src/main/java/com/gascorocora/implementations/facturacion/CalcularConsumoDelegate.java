package com.gascorocora.implementations.facturacion;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gascorocora.implementations.comun.VariablesUtil;

/**
 * Service Task "Calcular consumo y desviacion" (implementacion: Java Class).
 * Calcula el consumo del periodo y lo compara con el promedio para detectar
 * lecturas erroneas (problema principal del proceso segun el Process Portfolio).
 */
public class CalcularConsumoDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(CalcularConsumoDelegate.class);
    static final double UMBRAL_DESVIACION_PORCENTAJE = 30.0;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        double lecturaAnterior = VariablesUtil.getDouble(execution, "lecturaAnterior", 0);
        double lecturaActual = VariablesUtil.getDouble(execution, "lecturaActual", 0);
        double consumoPromedio = VariablesUtil.getDouble(execution, "consumoPromedioM3", 0);
        boolean lecturaVerificada = VariablesUtil.getBoolean(execution, "lecturaVerificada", false);

        double consumo = lecturaActual - lecturaAnterior;
        double desviacion = consumoPromedio > 0
                ? Math.abs(consumo - consumoPromedio) / consumoPromedio * 100
                : 0;

        boolean lecturaSospechosa = consumo < 0 || desviacion > UMBRAL_DESVIACION_PORCENTAJE;
        // Si ya fue verificada en terreno se acepta, aunque la desviacion siga siendo alta
        boolean requiereVerificacion = lecturaSospechosa && !lecturaVerificada;

        execution.setVariable("consumoM3", VariablesUtil.redondear(Math.max(consumo, 0), 2));
        execution.setVariable("desviacionPorcentaje", VariablesUtil.redondear(desviacion, 2));
        execution.setVariable("requiereVerificacion", requiereVerificacion);

        LOG.info("[Facturacion] Cuenta {} -> consumo {} m3, desviacion {} %, requiere verificacion: {}",
                execution.getVariable("numeroCuenta"), consumo, VariablesUtil.redondear(desviacion, 2),
                requiereVerificacion);
    }
}
