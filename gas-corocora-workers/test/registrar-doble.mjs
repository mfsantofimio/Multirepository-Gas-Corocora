// Redirige "camunda-external-task-client-js" al doble de prueba (solo al ejecutar npm test)
import { register } from "node:module";
register("./resolver-doble.mjs", import.meta.url);
