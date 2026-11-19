# gas-corocora-workers — External Task workers (JavaScript)

Workers del proyecto **Gas Corocora** que atienden las tareas de servicio de tipo *External* de los tres procesos, con [`camunda-external-task-client-js`](https://github.com/camunda/camunda-external-task-client-js). Forma parte del multirepositorio coordinado por [`gas-corocora-hub`](../gas-corocora-hub).

| Topic | Proceso / tarea BPMN | Worker | Integración |
|---|---|---|---|
| `EnviarFactura` | P1 · *Enviar factura al usuario* | `EnviarFacturaWorker.js` | Crea la obligación de pago en **Recaudo** (`POST /api/obligaciones`) y devuelve `referenciaPago`, `urlPago`, `saldoRecaudo`, `facturaEnviada` |
| `NotificarRespuestaPqrs` | P2 · *Notificar respuesta al usuario* | `NotificarRespuestaPqrsWorker.js` | Arma la respuesta usando el resultado de la nota crédito (`numeroNotaCredito`, `saldoCuentaRecaudo`, `saldoAFavor`) |
| `EnviarContratoConexion` | P3 · *Enviar cotización y contrato* | `EnviarContratoConexionWorker.js` | Notifica cotización y contrato; el proceso queda esperando la firma |

Si Recaudo no responde, el worker reporta el fallo con reintentos (3, cada 10 s); al agotarse, Camunda crea un **incidente** visible en Cockpit.

## Versiones y configuración

- Node.js 18 o superior (probado con 24). Dependencia: `camunda-external-task-client-js ^3.1.0`.
- `.env.example` → copie como `.env` para cambiar algo:

| Variable | Por defecto |
|---|---|
| `CAMUNDA_URL` | `http://127.0.0.1:8080/engine-rest` |
| `RECAUDO_URL` | `http://127.0.0.1:8083` |

Se usa `127.0.0.1` y no `localhost` porque en Node 18+ `localhost` puede resolverse a IPv6 y el *polling* falla.

## Ejecución

```bash
npm install
npm start              # los 3 workers en un proceso; o iniciar.bat / ./iniciar.sh (espera a que el motor responda)
npm run facturacion    # solo uno (también: pqrs, conexion)
```

## Pruebas

```bash
npm test     # CP-W01..CP-W04: workers contra Recaudo real con un doble del cliente de Camunda (Node 20.6+)
```
Requiere `gas-corocora-recaudo` encendido (o `RECAUDO_URL`); si no responde, los casos que lo necesitan se omiten. El doble (`test/doble-cliente.mjs`) solo se carga en `npm test`, no afecta la ejecución normal.
