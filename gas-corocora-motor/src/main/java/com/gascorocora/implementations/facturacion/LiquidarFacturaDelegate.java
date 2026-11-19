package com.gascorocora.implementations.facturacion;

import java.time.LocalDate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.gascorocora.implementations.comun.VariablesUtil;

/**
 * Service Task "Liquidar factura" (implementacion: Delegate Expression #{liquidarFacturaDelegate}).
 * Usa el porcentaje de subsidio/contribucion que devolvio la tabla DMN Decision_FactorTarifario.
 */
@Service("liquidarFacturaDelegate")
public class LiquidarFacturaDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(LiquidarFacturaDelegate.class);
    private static final int DIAS_PARA_PAGAR = 15;

    private final double valorMetroCubico;
    private final double cargoFijo;

    public LiquidarFacturaDelegate(@Value("${gascorocora.tarifas.valor-m3:2350}") double valorMetroCubico,
                                   @Value("${gascorocora.tarifas.cargo-fijo:4500}") double cargoFijo) {
        this.valorMetroCubico = valorMetroCubico;
        this.cargoFijo = cargoFijo;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        double consumo = VariablesUtil.getDouble(execution, "consumoM3", 0);
        double porcentajeAjuste = VariablesUtil.getDouble(execution, "porcentajeAjuste", 0);

        long valorConsumo = Math.round(consumo * valorMetroCubico);
        long valorAjuste = Math.round(valorConsumo * porcentajeAjuste / 100);
        long totalPagar = valorConsumo + valorAjuste + Math.round(cargoFijo);

        LocalDate hoy = LocalDate.now();
        String numeroFactura = VariablesUtil.generarCodigo("FAC");

        execution.setVariable("numeroFactura", numeroFactura);
        execution.setVariable("valorConsumo", valorConsumo);
        execution.setVariable("valorAjuste", valorAjuste);
        execution.setVariable("cargoFijo", Math.round(cargoFijo));
        execution.setVariable("totalPagar", totalPagar);
        execution.setVariable("fechaEmision", hoy.toString());
        execution.setVariable("fechaLimitePago", hoy.plusDays(DIAS_PARA_PAGAR).toString());

        LOG.info("[Facturacion] Factura {} liquidada: consumo ${} ajuste ${} ({} %) total ${}",
                numeroFactura, valorConsumo, valorAjuste, porcentajeAjuste, totalPagar);
    }
}
