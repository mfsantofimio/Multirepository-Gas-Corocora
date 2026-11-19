export async function resolve(especificador, contexto, siguiente) {
  if (especificador === "camunda-external-task-client-js") {
    return { url: new URL("./doble-cliente.mjs", import.meta.url).href, shortCircuit: true };
  }
  return siguiente(especificador, contexto);
}
