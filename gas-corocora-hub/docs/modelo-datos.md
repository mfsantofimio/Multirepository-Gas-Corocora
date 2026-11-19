# Modelo de datos

El sistema tiene **dos almacenes de negocio** y el almacén propio de Camunda:

| Base | Dueño | Tecnología | Contenido | Inicialización |
|---|---|---|---|---|
| `./h2/camunda-db` | gas-corocora-motor | H2 2.x en archivo | `ACT_*` (Camunda: runtime, historia, identidad) + `GC_*` (negocio) | Camunda crea `ACT_*`; `schema.sql` y `data.sql` crean y siembran `GC_*` en cada arranque (idempotentes) |
| `./data/recaudo.db` | gas-corocora-recaudo | SQLite (`node:sqlite`) | Obligaciones, pagos, notas crédito, saldo a favor | `db/schema.sql` al iniciar (idempotente) |

## Modelo de negocio del motor (`GC_*`)

```mermaid
erDiagram
    GC_SUSCRIPTOR ||--o{ GC_CUENTA : "es titular de"
    GC_SUSCRIPTOR ||--o{ GC_SOLICITUD_CONEXION : "solicita"
    GC_CUENTA ||--o{ GC_FACTURA : "se factura en"
    GC_CUENTA ||--o{ GC_PQRS : "origina"
    GC_SOLICITUD_CONEXION |o--o| GC_CUENTA : "matricula (numero_cuenta)"
    GC_SOLICITUD_CONEXION ||--o{ GC_EVENTO_CASO : "bitácora (root_process_instance_id)"
    GC_FACTURA ||--o{ GC_EVENTO_CASO : "bitácora"
    GC_PQRS ||--o{ GC_EVENTO_CASO : "bitácora"

    GC_SUSCRIPTOR {
        varchar documento PK
        varchar nombre
        varchar email
        varchar telefono
        varchar direccion
        varchar municipio
        varchar clave_hash "SHA-256(documento:clave)"
        varchar origen "PORTAL | TASKLIST | SEMILLA"
        timestamp fecha_registro
    }
    GC_CUENTA {
        varchar numero_cuenta PK
        varchar documento FK
        varchar tipo_usuario
        int estrato
        varchar direccion
        decimal consumo_promedio_m3
        decimal ultima_lectura
        varchar estado
        varchar numero_solicitud_origen
    }
    GC_SOLICITUD_CONEXION {
        varchar numero_solicitud PK "CON-AAAAMMDD-NNNN"
        varchar documento FK
        varchar process_instance_id "instancia P3"
        varchar direccion_predio
        varchar tipo_predio
        int estrato
        decimal distancia_red_metros
        varchar viabilidad "DMN"
        decimal valor_cotizacion
        varchar numero_contrato
        varchar numero_certificado
        varchar numero_cuenta
        varchar estado
    }
    GC_FACTURA {
        varchar numero_factura PK "FAC-AAAAMMDD-NNNN"
        varchar numero_cuenta FK
        varchar process_instance_id "instancia P1"
        decimal consumo_m3
        decimal porcentaje_ajuste "DMN"
        decimal total_pagar
        date fecha_limite_pago
        varchar referencia_pago "asignada por Recaudo"
        decimal valor_pagado
        varchar medio_pago
        varchar numero_orden_suspension
        varchar estado
    }
    GC_PQRS {
        varchar numero_radicado PK "PQRS-AAAAMMDD-NNNN"
        varchar numero_cuenta FK
        varchar process_instance_id "instancia P2"
        varchar tipo_solicitud
        varchar canal
        varchar ruta_atencion "DMN"
        varchar respuesta
        boolean procede_ajuste
        decimal valor_ajuste
        varchar numero_nota_credito "asignada por Recaudo"
        varchar motivo_recurso
        boolean decision_confirmada
        varchar estado
        date fecha_limite_respuesta
    }
    GC_EVENTO_CASO {
        bigint id PK
        varchar root_process_instance_id
        varchar process_instance_id "raíz o subproceso"
        varchar proceso
        varchar referencia
        varchar evento "PROCESO_INICIADO, TAREA_CREADA, TAREA_ASIGNADA, TAREA_COMPLETADA, ..."
        varchar actividad
        varchar usuario
        varchar detalle
        timestamp fecha
    }
```

### Vínculo con las instancias de proceso

- Cada caso guarda el `process_instance_id` de su **instancia raíz** (P1, P2 o P3). La referencia de negocio (número de solicitud, factura o radicado) es la llave primaria y también la variable con la que se **correlacionan los mensajes**.
- `GC_EVENTO_CASO` guarda la instancia raíz y la del subproceso (call activity), de modo que la bitácora de un caso incluye lo ocurrido en *Construcción de acometida*, *Ajuste de factura* o *Suspensión del servicio*.
- Quién escribe: `SincronizacionNegocioListener` escucha los eventos del motor (Spring Eventing Bridge, `camunda.bpm.eventing.execution/task: true`). Al terminar cada actividad hace un *upsert* del caso con las variables del proceso; en cada tarea de usuario registra creación, asignación y completado con el usuario. Un error de persistencia se registra en el log y **no detiene el proceso**.
- Quién lee: `CasosService` combina la fila de negocio con el estado vivo del motor (estado de la instancia, tareas pendientes de la raíz y sus subprocesos, mensajes que espera) para los portales.

### Estados de negocio

| Caso | Estados |
|---|---|
| Solicitud de conexión | RADICADA, DEVUELTA, NO_VIABLE, CONTRATO_GENERADO, ESPERANDO_FIRMA, EN_CONSTRUCCION, OBRA_ESCALADA, CONECTADA |
| Factura | EN_REVISION, PENDIENTE_PAGO, PAGADA, PAGO_PARCIAL, EN_MORA, SUSPENDIDA |
| PQRS | RADICADA, RESPONDIDA, RECURSO_EN_TRAMITE, RECURSO_CONFIRMADO, RECURSO_A_FAVOR, EN_APELACION_SSPD |

### Datos semilla (`data.sql`)

| Documento | Nombre | Clave portal | Cuentas |
|---|---|---|---|
| 1122334455 | Ana Pérez | demo123 | CTA-1001 (residencial, estrato 2, promedio 20 m³, lectura 1000), CTA-1003 (residencial, estrato 2, promedio 15 m³, lectura 640) |
| 2233445566 | Carlos Gómez | demo123 | CTA-1002 (comercial, estrato 4, promedio 85 m³, lectura 5400) |
| 3344556677 | Lucía Rincón | demo123 | — (para probar la solicitud de conexión) |

Los usuarios **internos** no están en `GC_*`: son usuarios y grupos de la identidad de Camunda (`ACT_ID_USER`, `ACT_ID_GROUP`, `ACT_ID_MEMBERSHIP`), creados por `IdentidadInicializador` al arrancar.

## Modelo del servicio de Recaudo (SQLite)

```mermaid
erDiagram
    OBLIGACION ||--o{ PAGO : "recibe"
    OBLIGACION ||--o{ NOTA_CREDITO : "se ajusta con"
    SALDO_A_FAVOR

    OBLIGACION {
        text referencia_pago PK "RP-<dígitos de la factura>"
        text numero_factura UK "correlación con P1"
        text numero_cuenta
        real valor
        real saldo
        text fecha_limite
        text process_instance_id "instancia P1"
        text estado "PENDIENTE | PAGO_PARCIAL | PAGADA | AJUSTADA"
    }
    PAGO {
        int id PK
        text numero_comprobante UK
        text referencia_pago FK
        real valor
        text medio_pago
        int notificado_motor "1 si el motor correlacionó el mensaje"
        text respuesta_motor
    }
    NOTA_CREDITO {
        int id PK
        text numero_nota_credito UK
        text numero_cuenta
        text numero_radicado "PQRS que la originó"
        text referencia_pago FK
        real valor
        real valor_aplicado
        real saldo_anterior
        real nuevo_saldo
    }
    SALDO_A_FAVOR {
        text numero_cuenta PK
        real valor
    }
```

## Consultas útiles para la demostración (consola H2)

```sql
-- Casos con su instancia de Camunda
SELECT numero_factura, numero_cuenta, total_pagar, referencia_pago, estado, process_instance_id FROM gc_factura;
SELECT numero_radicado, tipo_solicitud, ruta_atencion, procede_ajuste, numero_nota_credito, estado FROM gc_pqrs;
SELECT numero_solicitud, documento, viabilidad, valor_cotizacion, numero_contrato, numero_cuenta, estado FROM gc_solicitud_conexion;

-- Bitácora de un caso (reemplace la instancia)
SELECT fecha, evento, actividad, usuario, detalle FROM gc_evento_caso
WHERE root_process_instance_id = '<processInstanceId>' ORDER BY id;

-- Cruce con las tablas de Camunda: instancia y estado
SELECT f.numero_factura, h.STATE_, h.START_TIME_, h.END_TIME_
FROM gc_factura f JOIN ACT_HI_PROCINST h ON h.PROC_INST_ID_ = f.process_instance_id;
```
