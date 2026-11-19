// Reglas de negocio del servicio de Recaudo (obligaciones, pagos y notas credito).
import { camel, transaccion } from "./db.js";

export class ErrorNegocio extends Error {
  constructor(estado, mensaje, detalle) {
    super(mensaje);
    this.estado = estado;
    this.detalle = detalle;
  }
}

const MEDIOS_PAGO = ["PSE", "TARJETA", "CORRESPONSAL", "BANCO", "NOTA_CREDITO"];
const redondear = (v) => Math.round(Number(v) * 100) / 100;
const ahora = () => new Date().toISOString().replace("T", " ").slice(0, 19);
const consecutivo = (prefijo) => `${prefijo}-${new Date().toISOString().slice(0, 10).replace(/-/g, "")}-${Math.floor(1000 + Math.random() * 9000)}`;

function exigir(condicion, campo, mensaje) {
  if (!condicion) throw new ErrorNegocio(400, "Datos invalidos", `${campo}: ${mensaje}`);
}

export function crearServicioRecaudo({ db, motor, config }) {
  const q = {
    obligacionPorFactura: db.prepare("SELECT * FROM obligacion WHERE numero_factura = ?"),
    obligacionPorReferencia: db.prepare("SELECT * FROM obligacion WHERE referencia_pago = ?"),
    obligacionesCuenta: db.prepare("SELECT * FROM obligacion WHERE numero_cuenta = ? ORDER BY creada_en DESC"),
    obligacionPendienteCuenta: db.prepare(
      "SELECT * FROM obligacion WHERE numero_cuenta = ? AND saldo > 0 ORDER BY creada_en DESC, rowid DESC LIMIT 1"),
    insertarObligacion: db.prepare(`INSERT INTO obligacion (referencia_pago, numero_factura, numero_cuenta, concepto, valor, saldo,
      fecha_limite, process_instance_id, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`),
    actualizarSaldo: db.prepare("UPDATE obligacion SET saldo = ?, estado = ?, actualizada_en = ? WHERE referencia_pago = ?"),
    insertarPago: db.prepare(`INSERT INTO pago (numero_comprobante, referencia_pago, valor, medio_pago) VALUES (?, ?, ?, ?)`),
    marcarNotificacion: db.prepare("UPDATE pago SET notificado_motor = ?, respuesta_motor = ? WHERE numero_comprobante = ?"),
    pagoPorComprobante: db.prepare("SELECT * FROM pago WHERE numero_comprobante = ?"),
    pagosDe: db.prepare("SELECT * FROM pago WHERE referencia_pago = ? ORDER BY id"),
    notasDe: db.prepare("SELECT * FROM nota_credito WHERE referencia_pago = ? ORDER BY id"),
    insertarNota: db.prepare(`INSERT INTO nota_credito (numero_nota_credito, numero_cuenta, numero_radicado, referencia_pago, valor,
      valor_aplicado, saldo_anterior, nuevo_saldo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`),
    saldoFavor: db.prepare("SELECT valor FROM saldo_a_favor WHERE numero_cuenta = ?"),
    guardarSaldoFavor: db.prepare(`INSERT INTO saldo_a_favor (numero_cuenta, valor) VALUES (?, ?)
      ON CONFLICT(numero_cuenta) DO UPDATE SET valor = excluded.valor`),
    todasObligaciones: db.prepare("SELECT * FROM obligacion ORDER BY creada_en DESC, rowid DESC LIMIT 100"),
    todosPagos: db.prepare("SELECT * FROM pago ORDER BY id DESC LIMIT 100"),
    todasNotas: db.prepare("SELECT * FROM nota_credito ORDER BY id DESC LIMIT 100"),
  };

  const saldoAFavor = (cuenta) => Number(q.saldoFavor.get(cuenta)?.valor || 0);

  function presentar(fila) {
    const o = camel(fila);
    if (!o) return null;
    o.urlPago = `${config.urlPortalUsuarios}/facturas/${encodeURIComponent(o.numeroFactura)}/pagar`;
    return o;
  }

  function detalleObligacion(fila) {
    const o = presentar(fila);
    o.pagos = q.pagosDe.all(o.referenciaPago).map(camel);
    o.notasCredito = q.notasDe.all(o.referenciaPago).map(camel);
    return o;
  }

  return {
    /** Crea (o devuelve, si ya existe) la obligacion de pago de una factura. Idempotente por numeroFactura. */
    crearObligacion(datos = {}) {
      const { numeroFactura, numeroCuenta, valor, fechaLimite, concepto = "FACTURA_GAS", processInstanceId = null } = datos;
      exigir(typeof numeroFactura === "string" && numeroFactura.trim(), "numeroFactura", "es obligatorio");
      exigir(typeof numeroCuenta === "string" && numeroCuenta.trim(), "numeroCuenta", "es obligatorio");
      exigir(Number.isFinite(Number(valor)) && Number(valor) > 0, "valor", "debe ser un numero mayor que cero");
      exigir(!fechaLimite || /^\d{4}-\d{2}-\d{2}/.test(fechaLimite), "fechaLimite", "debe tener formato AAAA-MM-DD");

      const existente = q.obligacionPorFactura.get(numeroFactura);
      if (existente) return { creada: false, obligacion: detalleObligacion(existente) };

      return transaccion(db, () => {
        const total = redondear(valor);
        // Si la cuenta tiene saldo a favor (nota credito anterior) se descuenta de la nueva factura
        const favor = saldoAFavor(numeroCuenta);
        const usado = Math.min(favor, total);
        const saldo = redondear(total - usado);
        if (usado > 0) q.guardarSaldoFavor.run(numeroCuenta, redondear(favor - usado));
        const referencia = `RP-${numeroFactura.replace(/\D/g, "") || Date.now()}`;
        q.insertarObligacion.run(referencia, numeroFactura, numeroCuenta, concepto, total, saldo,
          fechaLimite ? fechaLimite.slice(0, 10) : null, processInstanceId, saldo === 0 ? "AJUSTADA" : "PENDIENTE");
        const obligacion = detalleObligacion(q.obligacionPorReferencia.get(referencia));
        obligacion.saldoAFavorAplicado = usado;
        return { creada: true, obligacion };
      });
    },

    obtenerObligacion(referenciaOFactura) {
      const fila = q.obligacionPorReferencia.get(referenciaOFactura) || q.obligacionPorFactura.get(referenciaOFactura);
      if (!fila) throw new ErrorNegocio(404, "No encontrada", `No existe la obligacion ${referenciaOFactura}`);
      return detalleObligacion(fila);
    },

    listarObligaciones(numeroCuenta) {
      return (numeroCuenta ? q.obligacionesCuenta.all(numeroCuenta) : q.todasObligaciones.all()).map(presentar);
    },

    /** Registra un pago y lo notifica al motor (mensaje MensajePagoRecibido). */
    async registrarPago(datos = {}) {
      const { referenciaPago, valor, medioPago } = datos;
      exigir(typeof referenciaPago === "string" && referenciaPago.trim(), "referenciaPago", "es obligatoria");
      exigir(Number.isFinite(Number(valor)) && Number(valor) > 0, "valor", "debe ser un numero mayor que cero");
      exigir(MEDIOS_PAGO.includes(medioPago), "medioPago", `debe ser uno de ${MEDIOS_PAGO.join(", ")}`);

      const fila = q.obligacionPorReferencia.get(referenciaPago);
      if (!fila) throw new ErrorNegocio(404, "No encontrada", `No existe la referencia de pago ${referenciaPago}`);
      if (fila.saldo <= 0) throw new ErrorNegocio(409, "Obligacion sin saldo", `La referencia ${referenciaPago} ya esta ${fila.estado}`);
      if (redondear(valor) > redondear(fila.saldo)) {
        throw new ErrorNegocio(400, "Datos invalidos", `valor: el pago ($${valor}) supera el saldo pendiente ($${fila.saldo})`);
      }

      const comprobante = consecutivo("CP");
      const saldoFinal = redondear(fila.saldo - valor);
      // Si el pago salda la obligacion se informa el valor total de la factura (el saldo a favor aplicado tambien cuenta)
      const valorNotificado = saldoFinal === 0 ? fila.valor : redondear(valor);
      transaccion(db, () => {
        const saldo = saldoFinal;
        q.insertarPago.run(comprobante, referenciaPago, redondear(valor), medioPago);
        q.actualizarSaldo.run(saldo, saldo === 0 ? "PAGADA" : "PAGO_PARCIAL", ahora(), referenciaPago);
      });

      // Integracion: el motor continua el proceso P1 (evento de mensaje "Pago recibido")
      const notificacion = fila.concepto === "FACTURA_GAS"
        ? await motor.notificarPago(fila.numero_factura, valorNotificado, medioPago)
        : { entregada: false, estado: null, detalle: "Concepto sin proceso asociado" };
      q.marcarNotificacion.run(notificacion.entregada ? 1 : 0, JSON.stringify(notificacion), comprobante);

      return {
        comprobante,
        pago: camel(q.pagoPorComprobante.get(comprobante)),
        obligacion: detalleObligacion(q.obligacionPorReferencia.get(referenciaPago)),
        notificacionMotor: notificacion,
      };
    },

    /** Reintenta avisar al motor un pago que no se pudo notificar (motor caido). */
    async reintentarNotificacion(comprobante) {
      const pago = q.pagoPorComprobante.get(comprobante);
      if (!pago) throw new ErrorNegocio(404, "No encontrado", `No existe el comprobante ${comprobante}`);
      if (pago.notificado_motor) return { comprobante, notificacionMotor: JSON.parse(pago.respuesta_motor) };
      const fila = q.obligacionPorReferencia.get(pago.referencia_pago);
      const notificacion = await motor.notificarPago(fila.numero_factura, pago.valor, pago.medio_pago);
      q.marcarNotificacion.run(notificacion.entregada ? 1 : 0, JSON.stringify(notificacion), comprobante);
      return { comprobante, notificacionMotor: notificacion };
    },

    /**
     * Aplica una nota credito (ajuste aprobado en una PQRS) a la obligacion pendiente mas reciente de la cuenta.
     * Lo que sobre queda como saldo a favor. Si la obligacion queda en cero se avisa al motor como pago por compensacion.
     */
    async aplicarNotaCredito(datos = {}) {
      const { numeroCuenta, valor, numeroRadicado = null } = datos;
      exigir(typeof numeroCuenta === "string" && numeroCuenta.trim(), "numeroCuenta", "es obligatorio");
      exigir(Number.isFinite(Number(valor)) && Number(valor) >= 0, "valor", "debe ser un numero mayor o igual a cero");

      const numeroNota = consecutivo("NC");
      const resultado = transaccion(db, () => {
        const pendiente = q.obligacionPendienteCuenta.get(numeroCuenta);
        const total = redondear(valor);
        let aplicado = 0, saldoAnterior = null, nuevoSaldo = null;
        if (pendiente) {
          saldoAnterior = pendiente.saldo;
          aplicado = Math.min(total, pendiente.saldo);
          nuevoSaldo = redondear(pendiente.saldo - aplicado);
          q.actualizarSaldo.run(nuevoSaldo, nuevoSaldo === 0 ? "AJUSTADA" : pendiente.estado, ahora(), pendiente.referencia_pago);
        }
        const sobrante = redondear(total - aplicado);
        if (sobrante > 0) q.guardarSaldoFavor.run(numeroCuenta, redondear(saldoAFavor(numeroCuenta) + sobrante));
        q.insertarNota.run(numeroNota, numeroCuenta, numeroRadicado, pendiente?.referencia_pago ?? null, total, aplicado,
          saldoAnterior, nuevoSaldo);
        return { pendiente, aplicado, saldoAnterior, nuevoSaldo };
      });

      let notificacionMotor = null;
      const { pendiente } = resultado;
      if (pendiente && resultado.nuevoSaldo === 0 && pendiente.concepto === "FACTURA_GAS") {
        // La factura quedo saldada por compensacion: el proceso P1 debe dejar de esperar el pago
        notificacionMotor = await motor.notificarPago(pendiente.numero_factura, pendiente.valor, "NOTA_CREDITO");
      }

      return {
        numeroNotaCredito: numeroNota,
        numeroCuenta,
        numeroRadicado,
        referenciaPago: pendiente?.referencia_pago ?? null,
        numeroFactura: pendiente?.numero_factura ?? null,
        valor: redondear(valor),
        valorAplicado: resultado.aplicado,
        saldoAnterior: resultado.saldoAnterior,
        nuevoSaldo: resultado.nuevoSaldo ?? 0,
        saldoAFavor: saldoAFavor(numeroCuenta),
        notificacionMotor,
      };
    },

    resumen() {
      return {
        obligaciones: q.todasObligaciones.all().map(presentar),
        pagos: q.todosPagos.all().map(camel),
        notasCredito: q.todasNotas.all().map(camel),
      };
    },
  };
}
