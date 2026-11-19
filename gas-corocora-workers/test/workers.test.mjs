// Pruebas de integracion workers -> Recaudo (CP-W01 a CP-W04 de docs/pruebas.md del hub).
// Requiere gas-corocora-recaudo encendido (por defecto http://127.0.0.1:8083). Si no responde, se omiten.
import { test, before } from "node:test";
import assert from "node:assert/strict";

const RECAUDO = process.env.RECAUDO_URL || "http://127.0.0.1:8083";
let recaudoArriba = false;
let handlers;
const factura = `FAC-TEST-${Date.now()}`;

before(async () => {
  try { recaudoArriba = (await fetch(`${RECAUDO}/api/salud`, { signal: AbortSignal.timeout(2000) })).ok; } catch { recaudoArriba = false; }
  await import("../TodosLosWorkers.js");
  ({ handlers } = await import("camunda-external-task-client-js"));
});

const tarea = (variables) => ({ processInstanceId: "pi-prueba", topicName: "prueba", retries: null,
  variables: { get: (nombre) => variables[nombre] } });
const servicioTareas = () => {
  const s = {};
  s.complete = async (_t, vars) => { s.completada = vars?.getAll?.() ?? {}; };
  s.handleFailure = async (_t, fallo) => { s.fallo = fallo; };
  return s;
};

test("CP-W01 EnviarFactura crea la obligacion en Recaudo y devuelve referencia, enlace y saldo", async (t) => {
  if (!recaudoArriba) return t.skip(`Recaudo no responde en ${RECAUDO}`);
  const s = servicioTareas();
  await handlers.EnviarFactura({ task: tarea({ numeroFactura: factura, numeroCuenta: "CTA-PRUEBA", totalPagar: 34110,
    fechaLimitePago: "2026-12-18", emailUsuario: "prueba@correo.co", nombreUsuario: "Prueba" }), taskService: s });
  assert.equal(s.fallo, undefined);
  assert.match(s.completada.referenciaPago, /^RP-/);
  assert.match(s.completada.urlPago, new RegExp(`/facturas/${factura}/pagar$`));
  assert.equal(s.completada.saldoRecaudo, 34110);
  const obligacion = await (await fetch(`${RECAUDO}/api/obligaciones/${factura}`)).json();
  assert.equal(obligacion.processInstanceId, "pi-prueba");
});

test("CP-W02 un reintento del worker no duplica la obligacion", async (t) => {
  if (!recaudoArriba) return t.skip(`Recaudo no responde en ${RECAUDO}`);
  const s = servicioTareas();
  await handlers.EnviarFactura({ task: tarea({ numeroFactura: factura, numeroCuenta: "CTA-PRUEBA", totalPagar: 34110 }), taskService: s });
  const lista = await (await fetch(`${RECAUDO}/api/obligaciones?cuenta=CTA-PRUEBA`)).json();
  assert.equal(lista.filter((o) => o.numeroFactura === factura).length, 1);
});

test("CP-W03 datos invalidos: el worker reporta el fallo con reintentos (incidente al agotarse)", async (t) => {
  if (!recaudoArriba) return t.skip(`Recaudo no responde en ${RECAUDO}`);
  const s = servicioTareas();
  await handlers.EnviarFactura({ task: tarea({ numeroFactura: `${factura}-X`, numeroCuenta: "CTA-PRUEBA", totalPagar: 0 }), taskService: s });
  assert.equal(s.completada, undefined);
  assert.equal(s.fallo.retries, 3);
});

test("CP-W04 NotificarRespuestaPqrs usa el resultado de la nota credito y marca la PQRS como respondida", async () => {
  const s = servicioTareas();
  await handlers.NotificarRespuestaPqrs({ task: tarea({ numeroRadicado: "PQRS-PRUEBA", respuestaUsuario: "Procede",
    procedeAjuste: true, estadoAjuste: "APLICADO", numeroNotaCredito: "NC-1", saldoCuentaRecaudo: 0, saldoAFavor: 5000 }), taskService: s });
  assert.equal(s.completada.estadoPqrs, "RESPONDIDA");
});
