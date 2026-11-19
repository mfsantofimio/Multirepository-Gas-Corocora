import { Client, Variables } from "camunda-external-task-client-js";
import { config, reportarFallo, postJson, urlRecaudo } from "./configuracion.js";

// Proceso: Gestion de Medicion y Facturacion
// Service Task "Enviar factura al usuario"  ->  topic: EnviarFactura
//
// Integracion con el servicio de Recaudo: antes de enviar la factura se registra la obligacion de pago.
// Recaudo devuelve la referencia de pago y el enlace del portal; ambos se guardan como variables del
// proceso y se usan en el mensaje al usuario y en el portal (boton "Pagar").
const client = new Client(config);

client.subscribe("EnviarFactura", async function ({ task, taskService }) {
  try {
    const email = task.variables.get("emailUsuario");
    const nombre = task.variables.get("nombreUsuario");
    const numeroFactura = task.variables.get("numeroFactura");
    const numeroCuenta = task.variables.get("numeroCuenta");
    const totalPagar = task.variables.get("totalPagar");
    const fechaLimitePago = task.variables.get("fechaLimitePago");

    const obligacion = await postJson(`${urlRecaudo}/api/obligaciones`, {
      numeroFactura,
      numeroCuenta,
      valor: Number(totalPagar),
      fechaLimite: fechaLimitePago,
      processInstanceId: task.processInstanceId
    });

    console.log("** Enviando factura **" +
      "\nPara: " + email +
      "\nHola " + nombre + ", su factura " + numeroFactura +
      " por $" + totalPagar + " vence el " + fechaLimitePago + "." +
      (obligacion.saldoAFavorAplicado ? "\nSe aplico su saldo a favor de $" + obligacion.saldoAFavorAplicado + "." : "") +
      "\nValor a pagar: $" + obligacion.saldo +
      "\nReferencia de pago: " + obligacion.referenciaPago +
      "\nPague en linea: " + obligacion.urlPago);

    const processVariables = new Variables();
    processVariables.set("facturaEnviada", true);
    processVariables.set("fechaEnvioFactura", new Date().toISOString());
    processVariables.set("canalEnvioFactura", "EMAIL");
    processVariables.set("referenciaPago", obligacion.referenciaPago);
    processVariables.set("urlPago", obligacion.urlPago);
    processVariables.set("saldoRecaudo", Number(obligacion.saldo));

    await taskService.complete(task, processVariables);
  } catch (error) {
    await reportarFallo(taskService, task, error);
  }
});
