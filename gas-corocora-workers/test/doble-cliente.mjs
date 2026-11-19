// Doble de prueba de camunda-external-task-client-js: guarda los handlers suscritos
// para invocarlos con tareas simuladas, sin necesitar el motor.
export const handlers = (globalThis.__handlersWorkers ??= {});
export const logger = {};
export class Variables {
  constructor() { this.valores = {}; }
  set(nombre, valor) { this.valores[nombre] = valor; return this; }
  get(nombre) { return this.valores[nombre]; }
  getAll() { return this.valores; }
}
export class Client {
  constructor(config) { this.config = config; }
  subscribe(topic, handler) { handlers[topic] = handler; return { unsubscribe() {} }; }
}
