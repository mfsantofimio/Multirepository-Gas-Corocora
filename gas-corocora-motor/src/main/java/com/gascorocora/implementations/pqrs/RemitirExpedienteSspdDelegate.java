package com.gascorocora.implementations.pqrs;

import java.time.LocalDateTime;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evento intermedio de mensaje de LANZAMIENTO (throw) "Remitir expediente a la SSPD".
 * En Camunda 7 un message throw event se implementa igual que una Service Task (Java Class).
 * Simula el envio del expediente a la Superintendencia para el recurso de apelacion.
 */
public class RemitirExpedienteSspdDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(RemitirExpedienteSspdDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        execution.setVariable("estadoPqrs", "EN_APELACION_SSPD");
        execution.setVariable("fechaRemisionSspd", LocalDateTime.now().toString());

        LOG.info("[PQRS] Mensaje 'MensajeRemisionSspd' enviado: expediente {} remitido a la SSPD. Fundamento: {}",
                execution.getVariable("numeroRadicado"), execution.getVariable("respuestaRecurso"));
    }
}
