package com.gascorocora.implementations.conexion;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.gascorocora.implementations.comun.VariablesUtil;

/**
 * Service Task "Calcular cotizacion de la conexion" (Delegate Expression #{cotizacionConexionDelegate}).
 * Costo base de la conexion + metros de extension de red cuando la DMN devuelve VIABLE_CON_EXTENSION.
 */
@Service("cotizacionConexionDelegate")
public class CotizacionConexionDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(CotizacionConexionDelegate.class);
    static final String VIABLE_CON_EXTENSION = "VIABLE_CON_EXTENSION";

    private final long costoBase;
    private final long costoExtensionMetro;
    private final double metrosIncluidos;

    public CotizacionConexionDelegate(@Value("${gascorocora.conexion.costo-base:1150000}") long costoBase,
                                      @Value("${gascorocora.conexion.costo-extension-metro:85000}") long costoExtensionMetro,
                                      @Value("${gascorocora.conexion.metros-incluidos:40}") double metrosIncluidos) {
        this.costoBase = costoBase;
        this.costoExtensionMetro = costoExtensionMetro;
        this.metrosIncluidos = metrosIncluidos;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String viabilidad = VariablesUtil.getString(execution, "viabilidad", "VIABLE");
        double distancia = VariablesUtil.getDouble(execution, "distanciaRedMetros", 0);

        long valorExtension = 0;
        if (VIABLE_CON_EXTENSION.equals(viabilidad)) {
            valorExtension = Math.round(Math.max(distancia - metrosIncluidos, 0) * costoExtensionMetro);
        }
        long valorCotizacion = costoBase + valorExtension;

        execution.setVariable("valorExtensionRed", valorExtension);
        execution.setVariable("valorCotizacion", valorCotizacion);

        LOG.info("[Conexion] Cotizacion solicitud {}: {} -> base ${} + extension ${} = ${}",
                execution.getVariable("numeroSolicitud"), viabilidad, costoBase, valorExtension, valorCotizacion);
    }
}
