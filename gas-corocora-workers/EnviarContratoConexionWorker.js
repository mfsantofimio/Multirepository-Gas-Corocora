import { Client, Variables } from "camunda-external-task-client-js";
import { config, reportarFallo } from "./configuracion.js";

// Proceso: Gestion de Conexion de Nuevos Usuarios
// Service Task "Enviar cotizacion y contrato al usuario"  ->  topic: EnviarContratoConexion
const client = new Client(config);

client.subscribe("EnviarContratoConexion", async function ({ task, taskService }) {
  try {
    const email = task.variables.get("emailSolicitante");
    const nombre = task.variables.get("nombreSolicitante");
    const numeroSolicitud = task.variables.get("numeroSolicitud");
    const numeroContrato = task.variables.get("numeroContrato");
    const valorCotizacion = task.variables.get("valorCotizacion");
    const diametro = task.variables.get("diametroTuberia");

    console.log("** Enviando cotizacion y contrato **" +
      "\nPara: " + email +
      "\nHola " + nombre + ", su solicitud " + numeroSolicitud + " es viable." +
      "\nValor de la conexion: $" + valorCotizacion + " (acometida de " + diametro + " pulgadas)" +
      "\nContrato de condiciones uniformes: " + numeroContrato +
      "\nFirme el contrato para programar la obra.");

    const processVariables = new Variables();
    processVariables.set("contratoEnviado", true);
    processVariables.set("fechaEnvioContrato", new Date().toISOString());
    processVariables.set("estadoSolicitud", "ESPERANDO_FIRMA");

    await taskService.complete(task, processVariables);
  } catch (error) {
    await reportarFallo(taskService, task, error);
  }
});
