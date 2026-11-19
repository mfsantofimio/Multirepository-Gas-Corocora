# gas-corocora-recaudo — Servicio de Recaudo / Tesorería

Servicio adicional del proyecto **Gas Corocora** (3.ª entrega). Simula la tesorería de la empresa: guarda las **obligaciones de pago** de cada factura, recibe los **pagos** del portal de usuarios, aplica **notas crédito** de los ajustes de PQRS y **avisa al motor Camunda** cuando una factura queda saldada.

Forma parte del multirepositorio coordinado por [`gas-corocora-hub`](../gas-corocora-hub).

## Qué hace dentro de los procesos

| Proceso | Quién llama | Endpoint | Dato que intercambia | Cómo se usa el resultado |
|---|---|---|---|---|
| P1 Medición y Facturación | Worker `EnviarFactura` (gas-corocora-workers) | `POST /api/obligaciones` | numeroFactura, numeroCuenta, totalPagar, fechaLimitePago, processInstanceId | El worker guarda `referenciaPago`, `urlPago` y `saldoRecaudo` como variables del proceso |
| P1 | Portal de usuarios (botón *Pagar*) | `POST /api/pagos` | referencia/factura, valor, medio de pago | Recaudo registra el pago y llama al motor `POST /api/facturacion/facturas/{n}/pagos` → mensaje **`MensajePagoRecibido`** (el *event-based gateway* sale por el pago y no por el timer) |
| P2 Atención PQRS (subproceso Ajuste de factura) | `NotaCreditoDelegate` del motor | `POST /api/notas-credito` | numeroCuenta, valor, numeroRadicado | El delegate guarda `numeroNotaCredito`, `referenciaAjustada`, `saldoCuentaRecaudo`, `saldoAFavor`; el worker `NotificarRespuestaPqrs` los incluye en la respuesta al usuario |

Reglas de negocio implementadas en `src/recaudo.js`:
- Una obligación por factura (idempotente: crearla dos veces devuelve la misma). Si la cuenta tiene saldo a favor, se descuenta.
- Medios de pago válidos: `PSE`, `TARJETA`, `CORRESPONSAL`, `BANCO`. El valor debe ser > 0 y ≤ saldo; una obligación pagada responde 409.
- Cuando el pago **salda** la obligación se notifica al motor el valor total de la factura (el motor marca la factura como pagada). Un pago parcial también se notifica: el proceso registra `saldoPendiente` y `estadoCartera = SALDO_PENDIENTE` (la factura queda `PAGO_PARCIAL` en la base del motor). Como el BPMN espera **un** mensaje de pago, los abonos posteriores quedan solo en Recaudo.
- Si el motor ya no espera el pago (venció el plazo), el pago queda registrado con `notificado_motor = 0` y se puede reintentar con `POST /api/pagos/{comprobante}/reintentar-notificacion`.
- La nota crédito se aplica a la obligación pendiente más reciente de la cuenta; si sobra, queda como **saldo a favor**. Si deja la factura en 0 se avisa al motor con medio `NOTA_CREDITO`.

## Tecnología y versiones

- **Node.js 22.13 o superior** (probado con Node 24). Sin dependencias npm: usa `node:http` y la base embebida **`node:sqlite`**.
- Base de datos SQLite en `data/recaudo.db`, creada en el primer arranque a partir de `db/schema.sql` (idempotente).

## Configuración

Copie `.env.example` como `.env` si necesita cambiar algo (opcional):

| Variable | Por defecto | Uso |
|---|---|---|
| `PORT` | `8083` | Puerto HTTP |
| `MOTOR_URL` | `http://127.0.0.1:8080` | API del motor (gas-corocora-motor) |
| `PORTAL_USUARIOS_URL` | `http://127.0.0.1:8081` | Para armar el enlace de pago que recibe el usuario |
| `DB_PATH` | `./data/recaudo.db` | Archivo SQLite |

## Ejecución

```bash
npm start          # o doble clic en iniciar.bat (Windows) / ./iniciar.sh
npm test           # 12 pruebas de integración (CP-R01 … CP-R12) con un motor simulado
npm run reset-db   # borra la base local; se recrea al iniciar
```

Consola web: http://localhost:8083 (obligaciones, pagos y notas crédito, se refresca sola).

## API

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/salud` | Estado del servicio |
| GET | `/api/resumen` | Totales para la consola |
| GET | `/api/obligaciones?cuenta=CTA-1001` | Obligaciones (filtro opcional por cuenta) |
| GET | `/api/obligaciones/{referenciaPago o numeroFactura}` | Detalle con sus pagos |
| POST | `/api/obligaciones` | `{numeroFactura, numeroCuenta, valor, fechaLimite?, processInstanceId?}` |
| POST | `/api/pagos` | `{referenciaPago o numeroFactura, valor, medioPago}` → 201 con comprobante y respuesta del motor |
| POST | `/api/pagos/{comprobante}/reintentar-notificacion` | Reenvía al motor un pago no notificado |
| POST | `/api/notas-credito` | `{numeroCuenta, valor, numeroRadicado?}` → 201 con número de nota y saldos |

Los errores responden JSON `{error, detalle}` con 400 (validación), 404 (no existe), 409 (estado no permite la operación) o 502 (motor caído).

## Estructura

```
db/schema.sql          modelo de datos (obligacion, pago, nota_credito, saldo_a_favor)
src/config.js          lectura de .env sin dependencias
src/db.js              conexión SQLite + transacciones
src/recaudo.js         reglas de negocio
src/motor.js           cliente HTTP del motor (correlación del mensaje de pago)
src/app.js / server.js API HTTP
public/index.html      consola de tesorería
test/                  pruebas de integración (node:test)
```
