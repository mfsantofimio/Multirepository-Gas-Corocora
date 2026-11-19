package com.gascorocora.implementations.pqrs;

import java.time.LocalDateTime;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service Task "Enviar alerta de vencimiento al jefe" (Delegate Expression #{alertaVencimientoDelegate}).
 * La dispara el timer NO interruptor del analisis: el analista sigue trabajando y en paralelo
 * se avisa al Jefe de Servicio al Cliente para evitar sanciones de la SSPD.
 */
@Service("alertaVencimientoDelegate")
public class AlertaVencimientoDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(AlertaVencimientoDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        execution.setVariable("alertaVencimientoEnviada", true);
        execution.setVariable("fechaAlertaVencimiento", LocalDateTime.now().toString());

        LOG.warn("[PQRS] ALERTA: el radicado {} vence el {} y aun no tiene respuesta. Notificando al jefe.",
                execution.getVariable("numeroRadicado"), execution.getVariable("fechaLimiteRespuesta"));
    }
}
