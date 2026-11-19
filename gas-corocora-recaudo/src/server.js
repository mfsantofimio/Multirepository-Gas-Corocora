// Punto de entrada: npm start  (o  node src/server.js)
import http from "node:http";
import { config } from "./config.js";
import { abrirBaseDatos } from "./db.js";
import { crearClienteMotor } from "./motor.js";
import { crearServicioRecaudo } from "./recaudo.js";
import { crearApp } from "./app.js";

const db = abrirBaseDatos(config.rutaBaseDatos);
const servicio = crearServicioRecaudo({ db, motor: crearClienteMotor(config.urlMotor), config });
const servidor = http.createServer(crearApp(servicio, config));

servidor.on("error", (e) => {
  if (e.code === "EADDRINUSE") {
    console.error(`[Recaudo] El puerto ${config.puerto} esta ocupado. Cambie PORT en .env o cierre el otro proceso.`);
    process.exit(1);
  }
  throw e;
});

servidor.listen(config.puerto, () => {
  console.log("==========================================================");
  console.log(` GAS COROCORA - Servicio de Recaudo / Tesoreria`);
  console.log(` API y consola:  http://127.0.0.1:${config.puerto}/`);
  console.log(` Base de datos:  ${config.rutaBaseDatos}`);
  console.log(` Motor Camunda:  ${config.urlMotor}`);
  console.log("==========================================================");
});
