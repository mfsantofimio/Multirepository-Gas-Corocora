// API HTTP del servicio de Recaudo (modulo http nativo de Node, sin frameworks).
import { readFileSync } from "node:fs";
import path from "node:path";
import { RAIZ } from "./config.js";
import { ErrorNegocio } from "./recaudo.js";

const CONSOLA = path.join(RAIZ, "public", "index.html");

const CON_ESTADO = Symbol("conEstado");
const con = (estado, cuerpo) => ({ [CON_ESTADO]: true, estado, cuerpo });

function responder(res, estado, cuerpo) {
  res.writeHead(estado, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(cuerpo));
}

async function leerJson(req) {
  let texto = "";
  for await (const parte of req) texto += parte;
  if (!texto) return {};
  try {
    return JSON.parse(texto);
  } catch {
    throw new ErrorNegocio(400, "JSON invalido", "El cuerpo de la peticion no es JSON valido");
  }
}

export function crearApp(servicio, config) {
  // [metodo, patron, manejador(req, params, query)]
  const rutas = [
    ["GET", /^\/api\/salud$/, () => ({ estado: "OK", servicio: "gas-corocora-recaudo", motor: config.urlMotor })],
    ["GET", /^\/api\/resumen$/, () => servicio.resumen()],
    ["GET", /^\/api\/obligaciones$/, (_r, _p, query) => servicio.listarObligaciones(query.get("cuenta"))],
    ["GET", /^\/api\/obligaciones\/([^/]+)$/, (_r, p) => servicio.obtenerObligacion(decodeURIComponent(p[1]))],
    ["POST", /^\/api\/obligaciones$/, async (req) => {
      const r = servicio.crearObligacion(await leerJson(req));
      return con(r.creada ? 201 : 200, r.obligacion);
    }],
    ["POST", /^\/api\/pagos$/, async (req) => con(201, await servicio.registrarPago(await leerJson(req)))],
    ["POST", /^\/api\/pagos\/([^/]+)\/reintentar-notificacion$/, (_r, p) => servicio.reintentarNotificacion(decodeURIComponent(p[1]))],
    ["POST", /^\/api\/notas-credito$/, async (req) => con(201, await servicio.aplicarNotaCredito(await leerJson(req)))],
  ];

  return async function manejar(req, res) {
    const url = new URL(req.url, "http://localhost");
    try {
      if (req.method === "GET" && (url.pathname === "/" || url.pathname === "/index.html")) {
        res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
        return res.end(readFileSync(CONSOLA));
      }
      for (const [metodo, patron, manejador] of rutas) {
        const p = url.pathname.match(patron);
        if (p && req.method === metodo) {
          const resultado = await manejador(req, p, url.searchParams);
          return resultado?.[CON_ESTADO] ? responder(res, resultado.estado, resultado.cuerpo) : responder(res, 200, resultado);
        }
      }
      responder(res, 404, { estado: 404, error: "Ruta no encontrada", detalle: `${req.method} ${url.pathname}` });
    } catch (e) {
      if (e instanceof ErrorNegocio) {
        return responder(res, e.estado, { estado: e.estado, error: e.message, detalle: e.detalle });
      }
      console.error("[Recaudo] Error inesperado:", e);
      responder(res, 500, { estado: 500, error: "Error interno", detalle: e.message });
    }
  };
}
