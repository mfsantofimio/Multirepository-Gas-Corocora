import { Client, Variables } from "camunda-external-task-client-js";
import { config, reportarFallo } from "./configuracion.js";

// Proceso: Atencion al Usuario y PQRS
// Service Task "Notificar respuesta al usuario"  ->  topic: NotificarRespuestaPqrs
// Usa los resultados que devolvio el servicio de Recaudo en el subproceso de ajuste
// (numero de nota credito y nuevo saldo de la cuenta).
const client = new Client(config);

client.subscribe("NotificarRespuestaPqrs", async function ({ task, taskService }) {
  try {
    const email = task.variables.get("emailUsuario");
    const numeroRadicado = task.variables.get("numeroRadicado");
    const respuesta = task.variables.get("respuestaUsuario");
    const procedeAjuste = task.variables.get("procedeAjuste");
    const estadoAjuste = task.variables.get("estadoAjuste");
    const numeroNotaCredito = task.variables.get("numeroNotaCredito");
    const saldoCuenta = task.variables.get("saldoCuentaRecaudo");
    const saldoAFavor = task.variables.get("saldoAFavor");

    let textoAjuste = "NO APLICA";
    if (procedeAjuste) {
      textoAjuste = estadoAjuste === "APLICADO"
        ? "APLICADO con la nota credito " + numeroNotaCredito + ". Nuevo saldo de su cuenta: $" + saldoCuenta +
          (saldoAFavor ? " (saldo a favor: $" + saldoAFavor + ")" : "")
        : "NO APROBADO";
    }

    console.log("** Notificando respuesta de PQRS **" +
      "\nPara: " + email +
      "\nRadicado: " + numeroRadicado +
      "\nRespuesta: " + respuesta +
      "\nAjuste a la factura: " + textoAjuste +
      "\nContra esta decision procede el recurso de reposicion y en subsidio el de apelacion ante la SSPD.");

    const processVariables = new Variables();
    processVariables.set("estadoPqrs", "RESPONDIDA");
    processVariables.set("fechaNotificacion", new Date().toISOString());

    await taskService.complete(task, processVariables);
  } catch (error) {
    await reportarFallo(taskService, task, error);
  }
});
