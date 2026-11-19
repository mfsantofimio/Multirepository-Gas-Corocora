// Acceso a SQLite con el modulo nativo node:sqlite (Node 22.13+). Sin npm install.
import { DatabaseSync } from "node:sqlite";
import { mkdirSync, readFileSync } from "node:fs";
import path from "node:path";
import { RAIZ } from "./config.js";

export function abrirBaseDatos(ruta) {
  if (ruta !== ":memory:") mkdirSync(path.dirname(ruta), { recursive: true });
  const db = new DatabaseSync(ruta);
  db.exec("PRAGMA foreign_keys = ON;");
  db.exec(readFileSync(path.join(RAIZ, "db", "schema.sql"), "utf8"));
  return db;
}

/** Convierte columnas snake_case a camelCase y quita el prototipo nulo de node:sqlite. */
export function camel(fila) {
  if (!fila) return null;
  const r = {};
  for (const [k, v] of Object.entries(fila)) r[k.replace(/_([a-z])/g, (_, c) => c.toUpperCase())] = v;
  return r;
}

/** Ejecuta fn dentro de una transaccion (todo o nada). */
export function transaccion(db, fn) {
  db.exec("BEGIN");
  try {
    const r = fn();
    db.exec("COMMIT");
    return r;
  } catch (e) {
    db.exec("ROLLBACK");
    throw e;
  }
}
