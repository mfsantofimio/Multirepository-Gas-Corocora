# gas-corocora-motor — Motor de procesos (Camunda 7 embebido en Spring Boot)

Componente central del proyecto **Gas Corocora**: ejecuta los 6 procesos BPMN, las 3 decisiones DMN y los 17 Camunda Forms, expone la API de negocio que usan los portales y Recaudo, y mantiene la **base de datos de negocio** vinculada a las instancias de proceso. Forma parte del multirepositorio coordinado por [`gas-corocora-hub`](../gas-corocora-hub).

## Versiones

| Elemento | Versión |
|---|---|
| Java | 17 o superior (probado 17 y 21; **no** Java 24) |
| Spring Boot | 3.2.0 |
| Camunda Platform 7 | 7.21.0 (`camunda-bpm-spring-boot-starter-webapp` + `-rest` + Spin) |
| Base de datos | H2 2.x en archivo `./h2/camunda-db` (tablas ACT_* de Camunda + GC_* de negocio) |
| Maven | 3.9 |

## Artefactos desplegados

`src/main/resources/static/` — `META-INF/processes.xml` los despliega al arrancar (sufijos `bpmn`, `dmn`, `form`):

| Proceso principal | Subproceso (call activity) | DMN | Responsable |
|---|---|---|---|
| `Proceso_MedicionFacturacion` (P1) | `Proceso_SuspensionServicio` | `Decision_FactorTarifario` | Andrés Santiago Chavarro Pardo |
| `Proceso_AtencionPqrs` (P2) | `Proceso_AjusteFactura` | `Decision_RutaAtencionPqrs` | Marlon Fabian Santofimio Romero |
| `Proceso_ConexionNuevosUsuarios` (P3) | `Proceso_ConstruccionAcometida` | `Decision_ViabilidadTecnica` | Brayan Steven Acosta Vigoya |

Vistas previas en `docs/diagramas/`. La descripción detallada de cada modelo está en el README del hub.

## Cambios de la 3.ª entrega (sobre la versión de la 2.ª)

1. **Tareas por rol:** se quitó `camunda:assignee="demo"` de las 14 tareas de usuario; ahora se atienden por `candidateGroups`. `config/IdentidadInicializador` crea al arrancar los 10 grupos y un usuario por rol (clave `demo`); el administrador `demo` pertenece a todos.
2. **Persistencia de negocio:** `schema.sql` + `data.sql` (idempotentes) crean las tablas `gc_*` y una semilla de suscriptores y cuentas. `listeners/SincronizacionNegocioListener` (Spring Eventing Bridge de Camunda) proyecta las variables del proceso a esas tablas y llena la bitácora `gc_evento_caso` en cada actividad y cada tarea.
3. **Integración con Recaudo:** `NotaCreditoDelegate` ya no simula la nota crédito: la registra en `gas-corocora-recaudo` (`services/RecaudoClient`). La tarea *Aplicar nota crédito* es `asyncBefore`: si Recaudo no responde, el job se reintenta y luego queda como **incidente** en Cockpit, sin perder la transacción del usuario.
4. **API para portales:** suscriptores (registro, autenticación, trámites) y casos (detalle con tareas pendientes, mensajes esperados, acciones disponibles y bitácora).

## Modelo de datos de negocio (`schema.sql`)

```
gc_suscriptor (documento PK, nombre, email, telefono, direccion, municipio, clave_hash)
   │1
   ├──< gc_cuenta (numero_cuenta PK, documento FK, tipo_usuario, estrato, consumo_promedio_m3, ultima_lectura)
   │        │1
   │        ├──< gc_factura (numero_factura PK, numero_cuenta, consumo, total, fechas, estado, process_instance_id)
   │        └──< gc_pqrs    (numero_radicado PK, numero_cuenta, tipo, ruta, respuesta, ajuste, recurso, estado, process_instance_id)
   └──< gc_solicitud_conexion (numero_solicitud PK, documento, predio, viabilidad, cotizacion, contrato, estado, process_instance_id)

gc_evento_caso (id, root_process_instance_id, process_instance_id, proceso, referencia, evento, actividad, usuario, detalle, fecha)
```

- **Vínculo con Camunda:** cada caso guarda el `process_instance_id` de la instancia raíz; la bitácora guarda además la instancia del subproceso. Con eso `GET /api/casos/por-instancia/{id}` encuentra el caso desde cualquier tarea, incluso de una call activity.
- **Inicialización reproducible:** `spring.sql.init.mode: always` ejecuta los scripts en cada arranque; usan `CREATE TABLE IF NOT EXISTS` y `MERGE`, así que no duplican ni borran datos. Borrar la carpeta `h2/` deja todo como recién instalado.
- Consola H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./h2/camunda-db`, usuario `sa`, sin clave).

Semilla: suscriptores 1122334455 (Ana Pérez), 2233445566 (Carlos Gómez), 3344556677 (Lucía Rincón), todos con clave `demo123`; cuentas CTA-1001 y CTA-1003 (Ana), CTA-1002 (Carlos).

## API REST

| Método | Ruta | Uso |
|---|---|---|
| POST | `/api/suscriptores` | Registro de usuario externo |
| POST | `/api/suscriptores/autenticar` | Ingreso del portal de usuarios |
| GET | `/api/suscriptores/{documento}` · `/{documento}/tramites` | Perfil y trámites con estado vivo |
| POST | `/api/conexiones/solicitudes` | Inicia P3 |
| POST | `/api/conexiones/solicitudes/{n}/firma-contrato` | Mensaje `MensajeContratoFirmado` |
| POST | `/api/facturacion/ciclos` | Inicia P1 |
| POST | `/api/facturacion/facturas/{n}/pagos` | Mensaje `MensajePagoRecibido` (lo llama Recaudo) |
| POST | `/api/pqrs` | Inicia P2 |
| POST | `/api/pqrs/{r}/recursos` | Mensaje `MensajeRecursoReposicion` |
| GET | `/api/casos?limite=` · `/api/casos/{ref}` · `/api/casos/por-instancia/{pi}` | Casos de negocio + estado del motor + bitácora |
| GET | `/api/procesos/{pi}` | Estado y variables de una instancia |
| * | `/engine-rest/**` | API REST estándar de Camunda (la usan el portal interno y los workers) |

Errores: JSON `ErrorResponse` con 400 (validación), 401, 404 (no existe o ninguna instancia espera el mensaje), 409 (documento ya registrado).

## Configuración (`application.yaml`)

| Propiedad | Valor | Nota |
|---|---|---|
| `server.port` | 8080 | |
| `spring.datasource.url` | `jdbc:h2:file:./h2/camunda-db` | |
| `camunda.bpm.admin-user` | demo / demo | Cockpit, Tasklist, Admin |
| `camunda.bpm.eventing.execution/task` | true | Necesario para la persistencia de negocio |
| `gascorocora.plazos.*` | PT5M, PT2M, PT5M, PT10M | Cortos para la demo; el valor real está comentado |
| `gascorocora.recaudo.url` | `http://127.0.0.1:8083` | Servicio de Recaudo |
| `gascorocora.usuarios.clave-por-defecto` | demo | Clave de los usuarios por rol |

## Ejecución

```bash
mvn spring-boot:run        # o doble clic en iniciar.bat (verifica Java, puerto y Maven; descarga Maven si falta)
```
Espere `Started Application` y abra http://localhost:8080/camunda (demo / demo). Sin Maven: abra la carpeta en IntelliJ o VS Code y ejecute `Application.java`.

## Problemas comunes

| Síntoma | Solución |
|---|---|
| `Port 8080 was already in use` | Cierre el otro Camunda (Camunda Run, ejemplo de clase u otra ventana) |
| `UnsupportedClassVersionError` / `invalid target release: 17` | Instale JDK 17 o 21 |
| `Database may be already in use` | Hay otra ejecución abierta; ciérrela o borre `h2/` |
| Incidente en *Aplicar nota crédito* | Recaudo apagado: inícielo y en Cockpit → *Incidents* → *Retry* |
| Una tarea External no avanza | Los workers no están corriendo (`gas-corocora-workers`) |
| `404 Ninguna instancia esta esperando este mensaje` | La instancia no está en el evento (ya pagó, o venció el plazo) |

## Estructura

```
src/main/java/com/gascorocora/
  Application.java          @SpringBootApplication + @EnableProcessApplication
  config/                   constantes de procesos, plazos, usuarios y grupos por rol
  controllers/              API REST (DTO validados)
  services/                 casos de uso (RuntimeService, HistoryService, TaskService) + RecaudoClient
  implementations/          JavaDelegates por proceso (facturacion, pqrs, conexion, comun)
  listeners/                proyección de negocio y bitácora (Spring Eventing Bridge)
  persistence/              NegocioRepository (JdbcTemplate)
src/main/resources/
  application.yaml  schema.sql  data.sql  META-INF/processes.xml
  static/bpmn  static/dmn  static/forms
docs/diagramas/             PNG de los 6 BPMN
```
