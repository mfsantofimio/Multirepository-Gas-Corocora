// Configuracion por variables de entorno (ver .env.example). Sin dependencias externas.
import { readFileSync, existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

export const RAIZ = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

// Carga opcional de un archivo .env (CLAVE=valor por linea) sin librerias.
const archivoEnv = path.join(RAIZ, ".env");
if (existsSync(archivoEnv)) {
  for (const linea of readFileSync(archivoEnv, "utf8").split(/\r?\n/)) {
    const m = linea.match(/^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*)\s*$/);
    if (m && process.env[m[1]] === undefined) process.env[m[1]] = m[2];
  }
}

export const config = {
  puerto: Number(process.env.PORT || 8083),
  // Motor Camunda (gas-corocora-motor). Se usa 127.0.0.1 y no "localhost" para evitar el problema de IPv6 en Node 18+.
  urlMotor: process.env.MOTOR_URL || "http://127.0.0.1:8080",
  // Portal de usuarios (para armar el enlace de pago que se envia en la factura)
  urlPortalUsuarios: process.env.PORTAL_USUARIOS_URL || "http://127.0.0.1:8081",
  rutaBaseDatos: process.env.DB_PATH || path.join(RAIZ, "data", "recaudo.db"),
};
