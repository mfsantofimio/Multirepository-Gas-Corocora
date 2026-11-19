# Guion de demostración de punta a punta (≈ 20 min)

Objetivo: mostrar en una sola corrida que **el proceso, las dos aplicaciones, el servicio de Recaudo y la persistencia** trabajan juntos. Cada paso indica qué mostrar y qué captura guardar en `docs/evidencias/` (ver [evidencias/README.md](evidencias/README.md)).

## Preparación (antes de la clase)

1. Clonar todo y arrancar con `scripts/iniciar-todo.bat` (o `.sh`). Verificar con `scripts/verificar.bat`: los cuatro servicios deben responder.
2. Para que la factura no venza mientras se muestra la PQRS, arrancar el motor con un plazo de pago más largo:
   ```bash
   cd gas-corocora-motor
   mvn spring-boot:run -Dspring-boot.run.arguments=--gascorocora.plazos.pago-factura=PT20M
   ```
   (El timer de 5 min se muestra aparte en CP-P1-03.)
3. Partir de una base limpia si se quiere: detener el motor, borrar `gas-corocora-motor/h2/` y `gas-corocora-recaudo/data/`.
4. Abrir cinco pestañas: Cockpit `http://localhost:8080/camunda` (demo/demo) · Portal usuarios `http://localhost:8081` · Portal interno `http://localhost:8082` · Consola Recaudo `http://localhost:8083` · Consola H2 `http://localhost:8080/h2-console`.

## 1. Despliegue (1 min)

Cockpit → *Processes*: 6 definiciones de proceso y 3 decisiones desplegadas. Admin → *Groups*: los 10 grupos por rol. **Captura 01, 02.**

## 2. Facturación con lectura sospechosa (P1, 4 min)

1. Portal interno como `facturador` → *Iniciar proceso* → **Medición y facturación** con CTA-1001 (Ana Pérez, residencial, estrato 2, promedio 20, lectura anterior 1000). **Captura 03.**
2. Salir y entrar como `lector` → bandeja → *Registrar lectura del medidor*: **1080**. Mostrar que la tarea siguiente es *Verificar lectura en terreno* (desviación 300 %). Completar con **1021** y *verificada*. **Captura 04.**
3. Entrar como `facturador` → *Revisar liquidación*: el formulario muestra consumo 21 m³, ajuste −40 % (DMN) y total **$34.110**. Completar. **Captura 05.**
4. Consola de Recaudo: aparece la obligación `RP-…` por $34.110 creada por el worker. **Captura 06.**
5. Portal usuarios como Ana (1122334455 / demo123) → *Mis trámites*: la factura está *Pendiente de pago* con botón **Pagar**. (No pagar todavía.) **Captura 07.**

## 3. Reclamo con ajuste a la factura (P2 + Recaudo, 5 min)

1. Ana → *Radicar PQRS*: RECLAMO sobre CTA-1001 por $60.000. **Captura 08.**
2. Portal interno como `analista` → *Analizar caso y proyectar respuesta*: mostrar la información del caso (bitácora, variables). Escribir la respuesta, marcar **procede ajuste** por $20.000. **Captura 09.**
3. Como `jefe` → *Aprobar ajuste a la factura*: aprobar.
4. Consola de Recaudo: nota crédito `NC-…` aplicada; la obligación baja a **$14.110**. En la ventana de los workers se ve la notificación con el número de nota y el nuevo saldo. **Captura 10.**
5. Ana → *Mis trámites*: la PQRS está *Respondida* y ofrece **Interponer recurso**. Interponerlo. **Captura 11.**
6. Como `analista` → *Resolver recurso*: confirmar la decisión → la PQRS queda *En apelación SSPD* (mensaje *throw* a la SSPD). **Captura 12.**

## 4. Pago en línea que continúa el proceso (P1 + Recaudo, 2 min)

1. Ana → **Pagar** la factura: el formulario propone el saldo $14.110 → PSE → *Pagar*. El resultado confirma que el proceso registró el pago. **Captura 13.**
2. Cockpit → la instancia de facturación terminó en *Factura pagada*; Recaudo muestra el pago con `notificado_motor = 1`. **Captura 14.**

## 5. Conexión de un nuevo usuario (P3, 5 min)

1. Portal usuarios como Lucía (3344556677 / demo123) → *Solicitar conexión* (residencial). **Captura 15.**
2. Portal interno: `asesor` → documentos completos; `ingeniero` → visita (120 m, capacidad suficiente) → DMN *viable con extensión*, cotización $7.950.000; `ingeniero` → diseño de acometida. **Captura 16.**
3. Lucía → **Firmar contrato** (el botón aparece solo cuando el proceso espera la firma). **Captura 17.**
4. `contratista` → *Ejecutar obra*; `inspector` → *Inspeccionar*: conforme → certificado y matrícula. Lucía ve la solicitud *Conectada* y su nueva cuenta. **Captura 18.**

## 6. Persistencia y trazabilidad (2 min)

1. Portal interno → *Casos*: los tres casos con su estado de negocio y estado del proceso; abrir uno y mostrar la bitácora. **Captura 19.**
2. Consola H2 (JDBC `jdbc:h2:file:./h2/camunda-db`, usuario `sa`): ejecutar las consultas de [modelo-datos.md](modelo-datos.md#consultas-útiles-para-la-demostración-consola-h2), incluida la que cruza `gc_factura` con `ACT_HI_PROCINST`. **Captura 20.**

## 7. Excepción controlada (opcional, 2 min)

Detener Recaudo, radicar otro reclamo con ajuste y aprobarlo como `jefe`: Cockpit muestra un **incidente** en *Aplicar nota crédito*. Encender Recaudo → *Retry* → el proceso continúa. **Captura 21.**
