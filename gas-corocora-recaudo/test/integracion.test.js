// Pruebas de integracion del servicio de Recaudo contra un motor Camunda simulado.
// Ejecutar:  npm test        (usa node:test, sin dependencias)
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import http from "node:http";
import { abrirBaseDatos } from "../src/db.js";
import { crearClienteMotor } from "../src/motor.js";
import { crearServicioRecaudo } from "../src/recaudo.js";
import { crearApp } from "../src/app.js";

let motorSimulado, recaudo, urlRecaudo;
const mensajesRecibidos = [];
const facturasEsperandoPago = new Set(["FAC-20261118-1001", "FAC-20261118-2002"]);

before(async () => {
  // Motor simulado: responde como /api/facturacion/facturas/{n}/pagos del motor real
  motorSimulado = http.createServer(async (req, res) => {
    let cuerpo = "";
    for await (const p of req) cuerpo += p;
    const m = req.url.match(/^\/api\/facturacion\/facturas\/([^/]+)\/pagos$/);
    const numero = m && decodeURIComponent(m[1]);
    mensajesRecibidos.push({ numero, ...JSON.parse(cuerpo || "{}") });
    res.setHeader("Content-Type", "application/json");
    if (numero && facturasEsperandoPago.has(numero)) {
      facturasEsperandoPago.delete(numero);
      res.end(JSON.stringify({ mensaje: "MensajePagoRecibido", processInstanceId: "pi-" + numero, llaveCorrelacion: numero }));
    } else {
      res.statusCode = 404;
      res.end(JSON.stringify({ estado: 404, error: "Ninguna instancia esta esperando este mensaje", detalle: "sin instancia" }));
    }
  });
  await new Promise((r) => motorSimulado.listen(0, "127.0.0.1", r));
  const urlMotor = `http://127.0.0.1:${motorSimulado.address().port}`;
  const config = { urlMotor, urlPortalUsuarios: "http://127.0.0.1:8081" };
  const servicio = crearServicioRecaudo({ db: abrirBaseDatos(":memory:"), motor: crearClienteMotor(urlMotor), config });
  recaudo = http.createServer(crearApp(servicio, config));
  await new Promise((r) => recaudo.listen(0, "127.0.0.1", r));
  urlRecaudo = `http://127.0.0.1:${recaudo.address().port}`;
});

after(() => { recaudo.close(); motorSimulado.close(); });

const post = (ruta, cuerpo) => fetch(urlRecaudo + ruta, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(cuerpo) })
  .then(async (r) => ({ estado: r.status, cuerpo: await r.json() }));
const get = (ruta) => fetch(urlRecaudo + ruta).then(async (r) => ({ estado: r.status, cuerpo: await r.json() }));

test("CP-R01 crea la obligacion de una factura y devuelve referencia y enlace de pago", async () => {
  const r = await post("/api/obligaciones", { numeroFactura: "FAC-20261118-1001", numeroCuenta: "CTA-1001", valor: 34110, fechaLimite: "2026-12-03" });
  assert.equal(r.estado, 201);
  assert.equal(r.cuerpo.referenciaPago, "RP-202611181001");
  assert.equal(r.cuerpo.saldo, 34110);
  assert.equal(r.cuerpo.estado, "PENDIENTE");
  assert.match(r.cuerpo.urlPago, /\/facturas\/FAC-20261118-1001\/pagar$/);
});

test("CP-R02 crear la misma obligacion otra vez es idempotente (200, sin duplicar)", async () => {
  const r = await post("/api/obligaciones", { numeroFactura: "FAC-20261118-1001", numeroCuenta: "CTA-1001", valor: 34110 });
  assert.equal(r.estado, 200);
  const lista = await get("/api/obligaciones?cuenta=CTA-1001");
  assert.equal(lista.cuerpo.length, 1);
});

test("CP-R03 rechaza datos invalidos con 400 y detalle del campo", async () => {
  const r = await post("/api/obligaciones", { numeroFactura: "FAC-X", numeroCuenta: "CTA-1001", valor: -5 });
  assert.equal(r.estado, 400);
  assert.match(r.cuerpo.detalle, /^valor:/);
  const p = await post("/api/pagos", { referenciaPago: "RP-202611181001", valor: 1000, medioPago: "BITCOIN" });
  assert.equal(p.estado, 400);
  assert.match(p.cuerpo.detalle, /^medioPago:/);
});

test("CP-R04 un pago mayor al saldo se rechaza", async () => {
  const r = await post("/api/pagos", { referenciaPago: "RP-202611181001", valor: 999999, medioPago: "PSE" });
  assert.equal(r.estado, 400);
  assert.match(r.cuerpo.detalle, /supera el saldo/);
});

test("CP-R05 el pago total salda la obligacion y se notifica al motor (MensajePagoRecibido)", async () => {
  const r = await post("/api/pagos", { referenciaPago: "RP-202611181001", valor: 34110, medioPago: "PSE" });
  assert.equal(r.estado, 201);
  assert.equal(r.cuerpo.obligacion.estado, "PAGADA");
  assert.equal(r.cuerpo.obligacion.saldo, 0);
  assert.equal(r.cuerpo.notificacionMotor.entregada, true);
  assert.equal(r.cuerpo.notificacionMotor.processInstanceId, "pi-FAC-20261118-1001");
  assert.deepEqual(mensajesRecibidos.at(-1), { numero: "FAC-20261118-1001", valorPagado: 34110, medioPago: "PSE" });
});

test("CP-R06 no se puede pagar dos veces la misma referencia (409)", async () => {
  const r = await post("/api/pagos", { referenciaPago: "RP-202611181001", valor: 100, medioPago: "PSE" });
  assert.equal(r.estado, 409);
});

test("CP-R07 pago cuando el proceso ya no espera (plazo vencido): se registra pero queda sin notificar", async () => {
  await post("/api/obligaciones", { numeroFactura: "FAC-20261118-3003", numeroCuenta: "CTA-1003", valor: 20000 });
  const r = await post("/api/pagos", { referenciaPago: "RP-202611183003", valor: 20000, medioPago: "CORRESPONSAL" });
  assert.equal(r.estado, 201);
  assert.equal(r.cuerpo.notificacionMotor.entregada, false);
  assert.equal(r.cuerpo.notificacionMotor.estado, 404);
});

test("CP-R08 nota credito parcial reduce el saldo de la obligacion pendiente", async () => {
  await post("/api/obligaciones", { numeroFactura: "FAC-20261118-2002", numeroCuenta: "CTA-1002", valor: 120000 });
  const r = await post("/api/notas-credito", { numeroCuenta: "CTA-1002", valor: 45000, numeroRadicado: "PQRS-20261118-5555" });
  assert.equal(r.estado, 201);
  assert.equal(r.cuerpo.referenciaPago, "RP-202611182002");
  assert.equal(r.cuerpo.saldoAnterior, 120000);
  assert.equal(r.cuerpo.nuevoSaldo, 75000);
  assert.equal(r.cuerpo.saldoAFavor, 0);
  assert.equal(r.cuerpo.notificacionMotor, null);
});

test("CP-R09 nota credito que salda la factura avisa al motor como pago por compensacion", async () => {
  const r = await post("/api/notas-credito", { numeroCuenta: "CTA-1002", valor: 80000, numeroRadicado: "PQRS-20261118-6666" });
  assert.equal(r.cuerpo.nuevoSaldo, 0);
  assert.equal(r.cuerpo.valorAplicado, 75000);
  assert.equal(r.cuerpo.saldoAFavor, 5000);
  assert.equal(r.cuerpo.notificacionMotor.entregada, true);
  assert.equal(mensajesRecibidos.at(-1).medioPago, "NOTA_CREDITO");
  const o = await get("/api/obligaciones/RP-202611182002");
  assert.equal(o.cuerpo.estado, "AJUSTADA");
  assert.equal(o.cuerpo.notasCredito.length, 2);
});

test("CP-R10 el saldo a favor se descuenta de la siguiente factura de la cuenta", async () => {
  const r = await post("/api/obligaciones", { numeroFactura: "FAC-20261218-2002", numeroCuenta: "CTA-1002", valor: 90000 });
  assert.equal(r.cuerpo.saldo, 85000);
  assert.equal(r.cuerpo.saldoAFavorAplicado, 5000);
});

test("CP-R11 nota credito sin deuda pendiente queda como saldo a favor", async () => {
  const r = await post("/api/notas-credito", { numeroCuenta: "CTA-9999", valor: 12000, numeroRadicado: "PQRS-X" });
  assert.equal(r.cuerpo.referenciaPago, null);
  assert.equal(r.cuerpo.saldoAFavor, 12000);
});

test("CP-R12 la consola y el resumen responden", async () => {
  const html = await fetch(urlRecaudo + "/").then((r) => r.text());
  assert.match(html, /Recaudo y Tesorería/);
  const r = await get("/api/resumen");
  assert.ok(r.cuerpo.obligaciones.length >= 3 && r.cuerpo.pagos.length >= 2 && r.cuerpo.notasCredito.length >= 3);
});
