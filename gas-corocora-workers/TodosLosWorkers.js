// Arranca los tres workers en un solo proceso de Node: npm start
import "./EnviarFacturaWorker.js";
import "./NotificarRespuestaPqrsWorker.js";
import "./EnviarContratoConexionWorker.js";

console.log("Workers de GAS COROCORA escuchando: EnviarFactura, NotificarRespuestaPqrs, EnviarContratoConexion");
