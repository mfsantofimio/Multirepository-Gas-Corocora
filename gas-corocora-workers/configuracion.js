import { existsSync, readFileSync } from "node:fs";
import { logger } from "camunda-external-task-client-js";

// Motor Camunda embebido en Spring Boot (gas-corocora-motor, puerto 8080).
// Se usa 127.0.0.1 y no "localhost": en Node 18+ "localhost" puede resolverse a IPv6 (::1)
// y el cliente falla con "polling failed with RequestError".
// Para cambiarlo:  set CAMUNDA_URL=http://127.0.0.1:8081/engine-rest   (o copie .env.example como .env)
cargarEnv();

export const config = {
  baseUrl: process.env.CAMUNDA_URL || "http://127.0.0.1:8080/engine-rest",
  use: logger,
  asyncResponseTimeout: 10000
};

// Servicio de Recaudo / Tesoreria (gas-corocora-recaudo, puerto 8083)
export const urlRecaudo = process.env.RECAUDO_URL || "http://127.0.0.1:8083";

// Reintentos cuando un worker falla (Camunda crea un incidente al llegar a 0)
export async function reportarFallo(taskService, task, error) {
  const reintentos = task.retries == null ? 3 : task.retries - 1;
  console.error(`[ERROR] ${task.topicName}: ${error.message} (reintentos restantes: ${Math.max(reintentos, 0)})`);
  await taskService.handleFailure(task, {
    errorMessage: error.message,
    errorDetails: error.stack,
    retries: Math.max(reintentos, 0),
    retryTimeout: 10000
  });
}

/** POST JSON con limite de tiempo; lanza un error legible si el servicio responde con error. */
export async function postJson(url, cuerpo) {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(cuerpo),
    signal: AbortSignal.timeout(8000)
  });
  const datos = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(`${url} respondio ${res.status}: ${datos.detalle || datos.error || "sin detalle"}`);
  return datos;
}

// Lee un archivo .env opcional (CLAVE=valor) sin dependencias.
function cargarEnv() {
  try {
    if (!existsSync(".env")) return;
    for (const linea of readFileSync(".env", "utf8").split(/\r?\n/)) {
      const m = linea.match(/^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*)\s*$/);
      if (m && process.env[m[1]] === undefined) process.env[m[1]] = m[2];
    }
  } catch { /* .env es opcional */ }
}
