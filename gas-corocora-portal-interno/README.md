# gas-corocora-portal-interno — Aplicación para usuarios internos (funcionarios)

Bandeja de trabajo de los **funcionarios** de Gas Corocora (puerto **8082**), en Spring Boot 3.2 + Thymeleaf. Reemplaza a Tasklist para la operación diaria: cada funcionario ve **solo las tareas de su rol**, consulta la información del caso, toma decisiones en el formulario de la tarea y la completa, con lo que **el proceso avanza**. Forma parte del multirepositorio coordinado por [`gas-corocora-hub`](../gas-corocora-hub).

## Cómo funciona

- **Ingreso por rol:** usuario y clave se validan contra la identidad de Camunda (`POST /engine-rest/identity/verify`) y se cargan sus grupos (`GET /group?member=`). Los usuarios y grupos los crea el motor al arrancar.
- **Bandeja** (`/tareas`): tareas de los grupos candidatos del funcionario (`GET /task?candidateGroups=…&includeAssignedTasks=true`) más las asignadas a él, agrupadas por proceso. Indica si la tarea está disponible, a su cargo o reclamada por otro.
- **Atender tarea** (`/tareas/{id}`):
  - **Formulario**: se descarga el mismo Camunda Form desplegado en el motor (`GET /task/{id}/deployed-form`) y se dibuja en Thymeleaf (texto, textfield, textarea, number, checkbox, select). Los campos deshabilitados muestran datos del caso; los editables son las **decisiones** del funcionario.
  - **Validación**: `required`, `min`, `max`, `pattern` y opciones de los select, en el navegador y otra vez en el servidor (`FormularioCamunda.convertir`).
  - **Actualización de variables**: al completar se envían como variables tipadas (`Long`/`Double`, `Boolean`, `String`) con `POST /task/{id}/submit-form`. Si la tarea estaba libre, se reclama a nombre del funcionario antes de completarla.
  - **Información del caso**: datos de negocio, estado e historial (bitácora `gc_evento_caso`) desde `GET /api/casos/por-instancia/{pi}` del motor, más todas las variables del proceso.
  - Reclamar / liberar la tarea. El portal verifica que el funcionario pertenezca a alguno de los grupos candidatos antes de mostrar, reclamar o completar.
- **Iniciar proceso** (`/procesos`): radicación por ventanilla o teléfono con el formulario de inicio desplegado (`deployed-start-form` + `submit-form` con *business key*). Solo el rol responsable ve cada proceso: asesoresComerciales → P3, analistasFacturacion → P1, agentesServicio → P2.
- **Casos** (`/casos`): últimos casos registrados y búsqueda por referencia (CON-…, FAC-…, PQRS-…).

## Usuarios de prueba (clave `demo`)

| Usuario | Grupo (candidateGroup) | Tareas que atiende |
|---|---|---|
| `asesor` | asesoresComerciales | P3: Revisar documentos del predio, Escalar incumplimiento del contratista |
| `ingeniero` | ingenierosRedes | P3: Realizar visita técnica, Elaborar diseño de acometida |
| `contratista` | contratistas | Subproceso obra: Ejecutar obra |
| `inspector` | inspectoresCalidad | Subproceso obra: Inspeccionar instalación |
| `lector` | lectoresCampo | P1: Registrar lectura, Verificar lectura en terreno |
| `facturador` | analistasFacturacion | P1: Revisar liquidación |
| `tecnico` | tecnicosSuspension | Subproceso suspensión: Ejecutar suspensión |
| `agente` | agentesServicio | P2: Responder en primer contacto |
| `analista` | analistasPqrs | P2: Analizar caso, Resolver recurso |
| `jefe` | jefesServicioCliente | Subproceso ajuste: Aprobar ajuste |
| `demo` | todos | Administrador (también entra a Cockpit/Tasklist) |

> La tabla refleja los `candidateGroups` del BPMN; si el equipo cambia un rol en el Modeler, cambia aquí también.

## Versiones y configuración

Java 17+, Maven 3.9, Spring Boot 3.2.0. `src/main/resources/application.yaml` (copia en `application-example.yaml`):

| Propiedad | Por defecto |
|---|---|
| `server.port` | `8082` |
| `gascorocora.camunda.url` | `http://127.0.0.1:8080/engine-rest` |
| `gascorocora.motor.url` | `http://127.0.0.1:8080` |

## Ejecución

```bash
mvn spring-boot:run        # o iniciar.bat / ./iniciar.sh
```
Abrir http://localhost:8082 e ingresar con un usuario de la tabla.

## Pruebas

```bash
mvn test     # FormularioCamundaTest: CP-I01..CP-I06 (precarga, obligatorios, opcionales vacíos como null, tipos, patrón/lista/rangos, decimales, casillas)
```

## Estructura

```
cliente/       CamundaClient (engine-rest), MotorClient (API de casos), ServicioException
formularios/   CampoFormulario y FormularioCamunda: lectura, validación y conversión de Camunda Forms
web/           AccesoController, BandejaController, ProcesosController, CasosController, sesión e interceptor
templates/     vistas Thymeleaf      static/css/portal.css
```
