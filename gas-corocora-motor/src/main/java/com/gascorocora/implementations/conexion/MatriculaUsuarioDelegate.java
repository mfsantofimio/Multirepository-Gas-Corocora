package com.gascorocora.implementations.conexion;

import java.time.LocalDate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gascorocora.implementations.comun.VariablesUtil;

/**
 * Service Task "Matricular usuario en el sistema comercial" (Delegate Expression #{matriculaUsuarioDelegate}).
 * Crea la cuenta con la que el nuevo usuario entra al ciclo de Medicion y Facturacion.
 */
@Service("matriculaUsuarioDelegate")
public class MatriculaUsuarioDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(MatriculaUsuarioDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String numeroCuenta = VariablesUtil.generarCodigo("CTA");
        execution.setVariable("numeroCuenta", numeroCuenta);
        execution.setVariable("fechaHabilitacion", LocalDate.now().toString());
        execution.setVariable("estadoSolicitud", "CONECTADA");

        LOG.info("[Conexion] Usuario {} matriculado con la cuenta {}. Certificado {}. Listo para facturacion.",
                execution.getVariable("nombreSolicitante"), numeroCuenta,
                execution.getVariable("numeroCertificado"));
    }
}
