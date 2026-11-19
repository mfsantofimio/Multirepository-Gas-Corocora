// Integracion con el motor Camunda (gas-corocora-motor): entrega el mensaje BPMN "MensajePagoRecibido"
// a traves de la API del motor, que lo correlaciona por numeroFactura.

export function crearClienteMotor(urlMotor) {
  return {
    /**
     * @returns {{entregada: boolean, estado: number|null, detalle: string, processInstanceId?: string}}
     */
    async notificarPago(numeroFactura, valorPagado, medioPago) {
      const url = `${urlMotor}/api/facturacion/facturas/${encodeURIComponent(numeroFactura)}/pagos`;
      try {
        const res = await fetch(url, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ valorPagado, medioPago }),
          signal: AbortSignal.timeout(8000),
        });
        const texto = await res.text();
        let cuerpo = {};
        try { cuerpo = texto ? JSON.parse(texto) : {}; } catch { cuerpo = { detalle: texto }; }
        if (res.ok) {
          return { entregada: true, estado: res.status, detalle: "Mensaje MensajePagoRecibido correlacionado", processInstanceId: cuerpo.processInstanceId };
        }
        return { entregada: false, estado: res.status, detalle: cuerpo.detalle || cuerpo.error || `HTTP ${res.status}` };
      } catch (e) {
        return { entregada: false, estado: null, detalle: `Motor no disponible en ${urlMotor}: ${e.cause?.code || e.message}` };
      }
    },
  };
}
