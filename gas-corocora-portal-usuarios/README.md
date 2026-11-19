# gas-corocora-portal-usuarios — Aplicación para usuarios externos

Portal web para **suscriptores y solicitantes** de Gas Corocora (puerto **8081**). Hecho con Spring Boot 3.2 + Thymeleaf; no tiene base de datos propia: todo lo consulta y lo ejecuta a través del **motor** (`gas-corocora-motor`) y de **Recaudo** (`gas-corocora-recaudo`). Forma parte del multirepositorio coordinado por [`gas-corocora-hub`](../gas-corocora-hub).

## Funciones y relación con los procesos

| Pantalla | Ruta | Llamada al servicio | Efecto en Camunda |
|---|---|---|---|
| Registro / ingreso | `/registro`, `/ingresar` | `POST /api/suscriptores`, `POST /api/suscriptores/autenticar` (motor) | — (tabla `gc_suscriptor`) |
| Mis trámites | `/panel` | `GET /api/suscriptores/{doc}/tramites` | Muestra solicitudes, facturas y PQRS con el estado vivo de su instancia y la acción disponible |
| Solicitar conexión | `/conexion/nueva` | `POST /api/conexiones/solicitudes` | **Inicia P3** `Proceso_ConexionNuevosUsuarios` (business key = documento) |
| Firmar contrato | `/conexion/{n}/firmar` | `POST /api/conexiones/solicitudes/{n}/firma-contrato` | Mensaje **`MensajeContratoFirmado`** |
| Radicar PQRS | `/pqrs/nueva` | `POST /api/pqrs` | **Inicia P2** `Proceso_AtencionPqrs` (business key = cuenta) |
| Interponer recurso | `/pqrs/{r}/recurso` | `POST /api/pqrs/{r}/recursos` | Mensaje **`MensajeRecursoReposicion`** |
| Pagar factura | `/facturas/{n}/pagar` | `POST /api/pagos` (Recaudo) | Recaudo correlaciona **`MensajePagoRecibido`** en P1 |
| Detalle del trámite | `/tramites/{ref}` | `GET /api/casos/{ref}` | Datos de negocio + tareas pendientes + bitácora |

Los botones *Firmar*, *Pagar* e *Interponer recurso* solo aparecen cuando la instancia **está esperando ese mensaje** (el motor lo calcula a partir de las suscripciones de eventos), y el portal verifica que el trámite pertenezca al usuario en sesión.

## Validación de entradas

Bean Validation en cada formulario (`formularios/*Form.java`), con mensajes en español junto al campo:
documento de 6 a 12 dígitos, celular de 10 dígitos que empieza por 3, correo válido, estrato 1–6, listas cerradas (tipo de predio, municipio, tipo de PQRS, canal, medio de pago), longitudes mínimas/máximas de textos, valores monetarios positivos y el pago no mayor al saldo (lo revalida Recaudo). Los errores de negocio de los servicios (409, 404, 400) se muestran como aviso en la pantalla.

## Versiones y configuración

Java 17+, Maven 3.9, Spring Boot 3.2.0. `src/main/resources/application.yaml` (copia comentada en `application-example.yaml`):

| Propiedad | Por defecto |
|---|---|
| `server.port` | `8081` |
| `gascorocora.motor.url` | `http://127.0.0.1:8080` |
| `gascorocora.recaudo.url` | `http://127.0.0.1:8083` |

## Ejecución

```bash
mvn spring-boot:run        # o iniciar.bat / ./iniciar.sh
```
Abrir http://localhost:8081. Usuario de prueba: documento **1122334455**, clave **demo123** (Ana Pérez, cuentas CTA-1001 y CTA-1003). También puede registrarse uno nuevo.

## Estructura

```
cliente/      MotorClient, RecaudoClient, ServicioException (traduce errores HTTP a mensajes)
formularios/  objetos de formulario con validaciones
web/          controladores, sesión, interceptor de acceso, formato y catálogos
templates/    vistas Thymeleaf      static/css/portal.css
```
