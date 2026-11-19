package com.gascorocora.config;

/**
 * Llaves de proceso y nombres de mensajes BPMN. Centralizarlos evita "strings magicos"
 * repartidos entre controladores y servicios.
 */
public final class ProcesosConstantes {

    private ProcesosConstantes() {
    }

    // Llaves (id) de los procesos ejecutables
    public static final String PROCESO_MEDICION_FACTURACION = "Proceso_MedicionFacturacion";
    public static final String PROCESO_ATENCION_PQRS = "Proceso_AtencionPqrs";
    public static final String PROCESO_CONEXION_NUEVOS_USUARIOS = "Proceso_ConexionNuevosUsuarios";

    // Nombres de los mensajes (atributo name de bpmn:message)
    public static final String MENSAJE_PAGO_RECIBIDO = "MensajePagoRecibido";
    public static final String MENSAJE_RECURSO_REPOSICION = "MensajeRecursoReposicion";
    public static final String MENSAJE_CONTRATO_FIRMADO = "MensajeContratoFirmado";

    // Variables usadas para correlacionar mensajes
    public static final String VAR_NUMERO_FACTURA = "numeroFactura";
    public static final String VAR_NUMERO_RADICADO = "numeroRadicado";
    public static final String VAR_NUMERO_SOLICITUD = "numeroSolicitud";
}
