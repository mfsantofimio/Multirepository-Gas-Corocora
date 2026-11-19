# Casos de prueba

Cada caso indica el camino BPMN que recorre, quién actúa, en qué aplicación y qué debe verse en el proceso, en la base de datos y en Recaudo.

- **Tipo:** P = camino principal · A = camino alterno · E = excepción.
- **Resultado obtenido:** lo diligencia el equipo al ejecutar la demostración (con la captura en `docs/evidencias/`). Las pruebas **automatizadas** ya tienen su resultado porque se ejecutaron al construir esta entrega.
- **Datos base:** semilla de `data.sql` (Ana Pérez 1122334455 / demo123, cuentas CTA-1001 y CTA-1003) y usuarios internos con clave `demo`.
- **Plazos de demostración** (`application.yaml` del motor): pago 5 min · alerta PQRS 2 min · recurso 5 min · obra 10 min.

## 1. Pruebas automatizadas

| ID | Componente | Comando | Qué verifica | Resultado obtenido |
|---|---|---|---|---|
| CP-R01 … CP-R12 | gas-corocora-recaudo | `npm test` | Obligación idempotente, validaciones 400, pago > saldo, pago total notifica `MensajePagoRecibido`, doble pago 409, pago con plazo vencido queda sin notificar, nota crédito parcial/total, compensación avisa al motor, saldo a favor, consola | **12/12 pasan** (18/11/2026, Node 22, motor simulado) |
| CP-W01 … CP-W04 | gas-corocora-workers | `npm test` (con Recaudo encendido) | `EnviarFactura` crea la obligación y devuelve referencia/enlace/saldo; reintento idempotente; fallo con reintentos; `NotificarRespuestaPqrs` usa la nota crédito | **4/4 pasan** (18/11/2026, contra Recaudo real) |
| CP-I01 … CP-I06 | gas-corocora-portal-interno | `mvn test` | Lectura de los Camunda Forms reales: precarga, obligatorios, opcionales vacíos como null, tipos `Long/Double/Boolean`, patrón de correo, listas, rangos, casillas | **7/7 pasan** (18/11/2026, ejecutadas con un ejecutor mínimo; en el equipo con `mvn test`) |

> Los tres proyectos Java (motor y portales) se compilaron sin errores al cerrar la entrega. La ejecución completa con Camunda se valida con los casos de la sección 2.

## 2. Pruebas funcionales de punta a punta

### P1 — Medición y Facturación (`Proceso_MedicionFacturacion`)

| ID | Tipo | Pasos (actor → aplicación) | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| CP-P1-01 | P | 1. `facturador` → Portal interno → *Iniciar proceso* → Medición y facturación: CTA-1001, Ana Pérez, correo válido, RESIDENCIAL, estrato 2, promedio 20, lectura anterior 1000.<br>2. `lector` → *Registrar lectura del medidor*: 1021.<br>3. `facturador` → *Revisar liquidación*.<br>4. Ana → Portal usuarios → *Mis trámites* → **Pagar** el total por PSE. | Consumo 21 m³, desviación 5 % → no requiere verificación. DMN `porcentajeAjuste = -40`. Total **$34.110** (49.350 − 19.740 + 4.500). El worker crea la obligación `RP-…` en Recaudo y guarda `referenciaPago`/`urlPago`. En el portal aparece el botón *Pagar*. Tras pagar: Recaudo `PAGADA`, `notificado_motor = 1`; instancia **COMPLETED** por *Factura pagada*; `gc_factura.estado = PAGADA`, `valor_pagado = 34110`. | |
| CP-P1-02 | A | Igual que CP-P1-01 pero en el paso 2 la lectura es **1080**. `lector` → *Verificar lectura en terreno*: lectura 1021 y marca *verificada*. | Desviación 300 % > 30 % → `requiereVerificacion = true` → tarea *Verificar lectura en terreno* (rol lectoresCampo). Tras verificar se recalcula y continúa por el camino principal. La bitácora muestra la tarea de verificación. | |
| CP-P1-03 | E | Llevar una factura hasta *Pendiente de pago* y **no pagar** durante 5 min. `tecnico` → *Ejecutar suspensión en terreno*: marcar ejecutada. | Gana el timer del *event-based gateway* → call activity `Proceso_SuspensionServicio` (orden `SUS-…`). Factura `EN_MORA` → `SUSPENDIDA`; cuenta `SUSPENDIDA`. El botón *Pagar* desaparece del portal. | |
| CP-P1-04 | E | En el portal de usuarios pagar un valor **mayor** al saldo o 0. | El portal rechaza con mensaje junto al campo; si se fuerza la llamada, Recaudo responde 400. El proceso sigue esperando el pago. | |
| CP-P1-05 | A | Pagar **parte** del saldo (p. ej. $20.000). | Recaudo `PAGO_PARCIAL`; el motor recibe el mensaje y registra `saldoPendiente = 14110`, `estadoCartera = SALDO_PENDIENTE`; `gc_factura.estado = PAGO_PARCIAL`. | |
| CP-P1-06 | E | Apagar Recaudo y avanzar una factura hasta *Enviar factura*. Luego encender Recaudo. | El worker reporta fallo con 3 reintentos (cada 10 s); al agotarse aparece un **incidente** en Cockpit. Al encender Recaudo y hacer *Retry* (o antes de agotarse) la tarea se completa y el proceso continúa. | |

### P2 — Atención al Usuario y PQRS (`Proceso_AtencionPqrs`)

| ID | Tipo | Pasos (actor → aplicación) | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| CP-P2-01 | P | Prerrequisito: CTA-1001 con factura pendiente en Recaudo (CP-P1-01 hasta el paso 3).<br>1. Ana → Portal usuarios → *Radicar PQRS*: RECLAMO, CTA-1001, valor $60.000.<br>2. `analista` → *Analizar caso*: respuesta, **procede ajuste** $20.000.<br>3. `jefe` → *Aprobar ajuste*: aprobado. | DMN `rutaAtencion = ANALISIS_TECNICO` (reclamo > 50.000). Call activity `Proceso_AjusteFactura`. *Aplicar nota crédito* llama a Recaudo: nota `NC-…`, obligación de CTA-1001 baja de 34.110 a **14.110**. El worker notifica la respuesta con el número de nota y el nuevo saldo. `gc_pqrs.numero_nota_credito` lleno, estado `RESPONDIDA`. | |
| CP-P2-02 | A | Radicar una **PETICION** (o reclamo ≤ $50.000). `agente` → *Responder en primer contacto*, sin ajuste. Esperar 5 min. | DMN `RESPUESTA_INMEDIATA` → tarea del rol agentesServicio. Sin ajuste va directo a notificar. Vence el término del recurso → fin *PQRS cerrada en firme*; instancia COMPLETED. | |
| CP-P2-03 | A | Tras CP-P2-01, Ana → *Interponer recurso* (antes de 5 min). `analista` → *Resolver recurso*: **confirma** la decisión. | Mensaje `MensajeRecursoReposicion` correlacionado por `numeroRadicado`. Al confirmar se lanza el mensaje *Remitir expediente a la SSPD* → estado `EN_APELACION_SSPD`. | |
| CP-P2-04 | A | Igual que CP-P2-03 pero el analista **revoca** (no confirma). | Fin *Recurso resuelto a favor*; estado `RECURSO_A_FAVOR`. | |
| CP-P2-05 | A | En *Analizar caso* esperar más de 2 min sin completar. | Timer **no interruptor**: se ejecuta *Enviar alerta de vencimiento al jefe* y la tarea sigue activa. La bitácora registra la alerta. | |
| CP-P2-06 | A | `jefe` → *Aprobar ajuste*: **no aprobado**. | Fin *Ajuste rechazado*; no se llama a Recaudo; la respuesta indica ajuste NO APROBADO. | |
| CP-P2-07 | E | Apagar Recaudo y aprobar un ajuste. Encender Recaudo y en Cockpit → *Incidents* → **Retry**. | *Aplicar nota crédito* es `asyncBefore`: la tarea del jefe se completa igual; el job falla, se reintenta y queda **incidente** con el mensaje de conexión. Tras el retry se aplica la nota y el proceso continúa. | |
| CP-P2-08 | A | Ajuste mayor que la deuda (p. ej. $50.000 con saldo $14.110). | Recaudo aplica 14.110, la obligación queda `AJUSTADA` (saldo 0) y **avisa al motor** con medio `NOTA_CREDITO`: la instancia P1 de esa factura termina como pagada. El sobrante queda como saldo a favor y se descuenta en la siguiente factura de la cuenta. | |

### P3 — Conexión de Nuevos Usuarios (`Proceso_ConexionNuevosUsuarios`)

| ID | Tipo | Pasos (actor → aplicación) | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| CP-P3-01 | P | 1. Lucía (3344556677) → Portal usuarios → *Solicitar conexión*: RESIDENCIAL, estrato 3.<br>2. `asesor` → *Revisar documentos*: completos.<br>3. `ingeniero` → *Visita técnica*: 120 m, capacidad suficiente.<br>4. `ingeniero` → *Elaborar diseño*.<br>5. Lucía → **Firmar contrato** en el portal.<br>6. `contratista` → *Ejecutar obra*.<br>7. `inspector` → *Inspeccionar*: conforme. | Solicitud `CON-…` con business key = documento. DMN `VIABLE_CON_EXTENSION`; cotización **$7.950.000** (1.150.000 + 80 m × 85.000). Diseño y contrato en paralelo (AND). El worker envía contrato; el portal muestra *Firmar*. Firma → call activity de obra → certificado `CERT-…` → matrícula: cuenta `CTA-…` creada en `gc_cuenta`. Estado `CONECTADA`, instancia COMPLETED. | |
| CP-P3-02 | A | `asesor` → *Revisar documentos*: **incompletos**. | Fin *Solicitud devuelta*; estado `DEVUELTA`. | |
| CP-P3-03 | A | `ingeniero` → *Visita técnica*: capacidad **insuficiente**. | DMN `NO_VIABLE` → fin *Solicitud no viable*; estado `NO_VIABLE`. | |
| CP-P3-04 | A | `inspector` → *Inspeccionar*: **no conforme**. | Vuelve a *Ejecutar obra* (contratista). | |
| CP-P3-05 | E | Tras la firma, no completar la obra en 10 min. `asesor` → *Escalar incumplimiento*. | Timer **interruptor** cancela la call activity → tarea *Escalar incumplimiento del contratista* → fin *Obra escalada*; estado `OBRA_ESCALADA`. | |

### Aplicaciones, seguridad y persistencia

| ID | Tipo | Pasos | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| CP-U01 | E | Registro con documento con letras, celular que no empieza por 3, correo inválido, claves distintas. | Mensajes junto a cada campo; no se llama al motor. | |
| CP-U02 | E | Registrar un documento que ya existe (1122334455). | Aviso "Ya existe un usuario registrado…" (409 del motor). | |
| CP-U03 | E | Ingresar con clave incorrecta. | "Documento o clave incorrectos" (401). | |
| CP-U04 | E | Con Ana, abrir `/tramites/<referencia de otra persona>` o `/facturas/<factura ajena>/pagar`. | El portal lo rechaza: el trámite no pertenece al usuario. | |
| CP-U05 | E | Abrir `/conexion/{n}/firmar` o `/facturas/{n}/pagar` cuando el proceso **no** espera ese mensaje. | El botón no aparece; por URL directa responde que no está esperando esa acción. | |
| CP-I07 | P | Ingresar al portal interno con `lector`, `analista`, `jefe`. | Cada uno ve **solo** las tareas de su grupo, agrupadas por proceso. `demo` ve todas. | |
| CP-I08 | E | Con `lector`, abrir la URL de una tarea del grupo analistasPqrs. | Redirige a la bandeja con "La tarea no corresponde a sus roles". | |
| CP-I09 | E | `analista` reclama una tarea; `demo` intenta completarla. | Botón deshabilitado y mensaje "La tarea está reclamada por analista". | |
| CP-I10 | E | Completar un formulario dejando vacío un obligatorio o con un número fuera de rango. | Error junto al campo; la tarea no se completa. | |
| CP-D01 | P | Durante CP-P1-01 consultar en `/h2-console` `gc_factura` y `gc_evento_caso`. | La factura aparece con su `process_instance_id` y su estado; la bitácora tiene inicio, tareas creadas/asignadas/completadas con usuario, tareas automáticas y fin. | |
| CP-D02 | P | Reiniciar el motor. | Las tablas `GC_*` conservan los datos; la semilla no se duplica (MERGE). | |
| CP-D03 | P | Detener el motor, borrar la carpeta `h2/` y arrancar. | Se recrean las tablas `ACT_*` y `GC_*`, la semilla y los usuarios por rol: instalación limpia reproducible. | |
