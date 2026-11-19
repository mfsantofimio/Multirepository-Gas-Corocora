package com.gascorocora.implementations.conexion;

import java.time.LocalDate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gascorocora.implementations.comun.VariablesUtil;

/**
 * Service Task "Emitir certificado de conformidad" del subproceso Proceso_ConstruccionAcometida
 * (invocado por Call Activity). Implementacion: Java Class.
 */
public class EmitirCertificadoConformidadDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(EmitirCertificadoConformidadDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String numeroCertificado = VariablesUtil.generarCodigo("CERT");
        execution.setVariable("numeroCertificado", numeroCertificado);
        execution.setVariable("fechaCertificado", LocalDate.now().toString());

        LOG.info("[Obra] Certificado de conformidad {} emitido para la solicitud {}",
                numeroCertificado, execution.getVariable("numeroSolicitud"));
    }
}
