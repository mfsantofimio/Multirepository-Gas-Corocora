# Multirepository - Corocora Gas
Final deliverable of the Corocora Gas project, carried out throughout the semester following a BPMN-based multi-repository approach for business process modeling and management.

# GAS COROCORA S.A. E.S.P.

## 1. Descripción del proyecto

Gas Corocora S.A. E.S.P. es una empresa dedicada a la distribución de gas natural domiciliario en Villavicencio y municipios del departamento del Meta.

Este proyecto tiene como propósito modelar, automatizar y gestionar tres procesos de negocio fundamentales de la organización mediante el uso de tecnologías de gestión de procesos empresariales (BPM).

La solución utiliza **Camunda 7** como motor de automatización, permitiendo ejecutar procesos modelados bajo el estándar BPMN, gestionar tareas, controlar tiempos de respuesta, integrar servicios y mantener la trazabilidad de las actividades realizadas.

El proyecto contempla tres procesos principales y sus respectivos subprocesos, acompañados de aplicaciones web y servicios que permiten integrar las diferentes actividades de la organización.

## 2. Objetivo general

Diseñar e implementar una solución de automatización de procesos de negocio para Gas Corocora S.A. E.S.P., mediante el modelado BPMN y la implementación de Camunda 7, facilitando la gestión, seguimiento y ejecución de los procesos de atención al usuario, conexión de nuevos usuarios y medición y facturación.

## 3. Integrantes del grupo

| **Estudiante (nombre completo)** |
| -------------------------------- |
| Marlon Fabian Santofimio Romero  |
| Brayan Steven Acosta Vigoya      |
| Andrés Santiago Chavarro Pardo   |

## 4. Procesos de negocio desarrollados

El proyecto contempla la automatización de los siguientes procesos:

### 4.1. Atención al Usuario y PQRS

Gestiona las peticiones, quejas y reclamos presentados por los usuarios de Gas Corocora, desde su radicación hasta la emisión de una respuesta.

Incluye el subproceso **Ajuste de Factura**, encargado de gestionar las solicitudes que requieren una revisión y posible modificación de los valores facturados mediante notas crédito.

**Responsable:** Marlon Fabian Santofimio Romero.

### 4.2. Gestión de Conexión de Nuevos Usuarios

Gestiona las solicitudes de nuevos usuarios interesados en acceder al servicio de gas natural, contemplando la revisión documental, evaluación de viabilidad técnica, cotización, formalización del contrato y ejecución de las actividades necesarias para la conexión.

Incluye el subproceso **Construcción de Acometida e Instalación Interna**, que contempla las actividades de construcción, inspección y certificación correspondientes.

**Responsable:** Brayan Steven Acosta Vigoya.

### 4.3. Gestión de Medición y Facturación

Gestiona el proceso de registro de lecturas, cálculo del consumo, aplicación de tarifas, liquidación y generación de facturas para los usuarios del servicio.

Incluye el subproceso **Suspensión del Servicio por Mora**, encargado de gestionar las actividades relacionadas con la suspensión del suministro cuando se presentan incumplimientos en los pagos.

**Responsable:** Andrés Santiago Chavarro Pardo.

## 5. Arquitectura y componentes del sistema

La solución está organizada en diferentes componentes que trabajan de manera integrada:

| **Componente**      | **Descripción**                                                                                                       |
| ------------------- | --------------------------------------------------------------------------------------------------------------------- |
| Motor de procesos   | Ejecuta los procesos BPMN y las decisiones DMN mediante Camunda 7 y Spring Boot.                                      |
| Portal de usuarios  | Permite a los usuarios realizar solicitudes de conexión, radicar PQRS, consultar trámites y efectuar pagos simulados. |
| Portal interno      | Permite a los funcionarios gestionar tareas, consultar casos y ejecutar actividades según sus roles.                  |
| Servicio de Recaudo | Gestiona las obligaciones de pago, los pagos y las notas crédito.                                                     |
| Workers             | Ejecutan tareas externas y permiten integrar los procesos con otros servicios.                                        |
| Hub integrador      | Centraliza la documentación, configuración, scripts de ejecución y evidencias del proyecto.                           |

## 6. Tecnologías utilizadas

Las principales tecnologías y herramientas utilizadas en el desarrollo son:

- **Camunda 7:** motor de automatización de procesos.
- **BPMN 2.0:** modelado de los procesos de negocio.
- **DMN:** implementación de reglas y decisiones de negocio.
- **Java 17:** desarrollo del motor de procesos y servicios.
- **Spring Boot:** construcción de aplicaciones y servicios web.
- **JavaScript / Node.js:** implementación de workers y servicio de recaudo.
- **HTML y Thymeleaf:** interfaces web.
- **H2 y SQLite:** almacenamiento de información.
- **REST API:** comunicación e integración entre componentes.
- **Git y GitHub:** control de versiones y gestión del código fuente.

## 7. Estructura del repositorio

El proyecto se encuentra organizado en los siguientes componentes:

```text
gas-corocora/
│
├── gas-corocora-motor/
│   └── Motor de procesos Camunda, BPMN, DMN y servicios.
│
├── gas-corocora-portal-usuarios/
│   └── Aplicación web para usuarios externos.
│
├── gas-corocora-portal-interno/
│   └── Aplicación web para funcionarios.
│
├── gas-corocora-recaudo/
│   └── Servicio de recaudo y tesorería.
│
├── gas-corocora-workers/
│   └── Workers para la ejecución de tareas externas.
│
└── gas-corocora-hub/
    └── Documentación, arquitectura, diagramas,
        evidencias, configuración y scripts.
```

## 8. Modelado de procesos

Los procesos y subprocesos fueron representados mediante diagramas BPMN, permitiendo identificar las actividades, responsables, decisiones, eventos, compuertas y conexiones entre las diferentes etapas.

Adicionalmente, se utilizan reglas de negocio DMN para apoyar la toma de decisiones dentro de los procesos automatizados.

Los modelos BPMN se encuentran en el componente `gas-corocora-motor`, mientras que sus representaciones gráficas y documentación complementaria se encuentran en `gas-corocora-hub/docs/`.

## 9. Alcance del proyecto

La solución implementa los tres procesos principales de negocio y sus respectivos subprocesos, integrando aplicaciones web, servicios REST, workers y persistencia de información.

El sistema permite:

- Automatizar actividades y flujos de trabajo.
- Gestionar tareas según los roles de los funcionarios.
- Aplicar reglas de negocio mediante decisiones DMN.
- Controlar tiempos y vencimientos de actividades.
- Integrar los procesos mediante servicios REST.
- Consultar el estado y seguimiento de los casos.
- Gestionar pagos simulados y notas crédito.
- Mantener la trazabilidad de las operaciones realizadas.

**Nota:** El proyecto tiene fines académicos. Los pagos y las notificaciones externas se encuentran simulados y no corresponden a integraciones con entidades financieras o servicios reales de correo electrónico.

## 10. Documentación y evidencias

La documentación complementaria del proyecto se encuentra en el directorio `gas-corocora-hub/docs/`, donde se incluyen:

- Diagramas de arquitectura.
- Diagramas de los procesos BPMN.
- Modelo de datos.
- Documentación de pruebas.
- Evidencias de ejecución.
- Guion de demostración.
- Documentación de entregas anteriores.
