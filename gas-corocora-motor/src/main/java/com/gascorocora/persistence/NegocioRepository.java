package com.gascorocora.persistence;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso a las tablas de negocio GC_* (schema.sql) con JdbcTemplate.
 * Todas las consultas devuelven mapas con llaves en camelCase (numeroCuenta, totalPagar...)
 * para que la API y los portales usen los mismos nombres que las variables del proceso.
 */
@Repository
public class NegocioRepository {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbc;

    public NegocioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ suscriptores

    public boolean existeSuscriptor(String documento) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM gc_suscriptor WHERE documento = ?", Integer.class, documento);
        return n != null && n > 0;
    }

    public void insertarSuscriptor(String documento, String nombre, String email, String telefono,
                                   String direccion, String municipio, String claveHash, String origen) {
        jdbc.update("INSERT INTO gc_suscriptor (documento, nombre, email, telefono, direccion, municipio, clave_hash, origen) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", documento, nombre, email, telefono, direccion, municipio, claveHash, origen);
    }

    /** Crea el suscriptor si no existe (p. ej. cuando la solicitud se radica por Tasklist). */
    public void asegurarSuscriptor(String documento, String nombre, String email, String telefono,
                                   String direccion, String municipio) {
        if (documento == null || documento.isBlank() || existeSuscriptor(documento)) {
            return;
        }
        insertarSuscriptor(documento, nombre == null ? "(sin nombre)" : nombre, email, telefono, direccion, municipio,
                null, "TASKLIST");
    }

    public Map<String, Object> buscarSuscriptor(String documento) {
        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT documento, nombre, email, telefono, direccion, municipio, origen, fecha_registro "
                        + "FROM gc_suscriptor WHERE documento = ?", documento);
        return filas.isEmpty() ? null : camel(filas.get(0));
    }

    public boolean claveValida(String documento, String claveHash) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM gc_suscriptor WHERE documento = ? AND clave_hash = ?",
                Integer.class, documento, claveHash);
        return n != null && n > 0;
    }

    // ------------------------------------------------------------------ cuentas

    public boolean existeCuenta(String numeroCuenta) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM gc_cuenta WHERE numero_cuenta = ?", Integer.class, numeroCuenta);
        return n != null && n > 0;
    }

    /** Crea la cuenta si no existe. El documento puede ser null (cuenta creada desde Tasklist). */
    public void asegurarCuenta(String numeroCuenta, String documento, String tipoUsuario, Integer estrato,
                               String direccion, BigDecimal consumoPromedio, BigDecimal ultimaLectura,
                               String numeroSolicitudOrigen) {
        if (numeroCuenta == null || numeroCuenta.isBlank() || existeCuenta(numeroCuenta)) {
            return;
        }
        String doc = (documento != null && existeSuscriptor(documento)) ? documento : null;
        jdbc.update("INSERT INTO gc_cuenta (numero_cuenta, documento, tipo_usuario, estrato, direccion, "
                        + "consumo_promedio_m3, ultima_lectura, estado, numero_solicitud_origen) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVA', ?)",
                numeroCuenta, doc, tipoUsuario, estrato, direccion, consumoPromedio, ultimaLectura, numeroSolicitudOrigen);
    }

    public void actualizarCuenta(String numeroCuenta, BigDecimal ultimaLectura, String estado) {
        if (ultimaLectura != null) {
            jdbc.update("UPDATE gc_cuenta SET ultima_lectura = ? WHERE numero_cuenta = ?", ultimaLectura, numeroCuenta);
        }
        if (estado != null) {
            jdbc.update("UPDATE gc_cuenta SET estado = ? WHERE numero_cuenta = ?", estado, numeroCuenta);
        }
    }

    public List<Map<String, Object>> cuentasDe(String documento) {
        return camel(jdbc.queryForList("SELECT * FROM gc_cuenta WHERE documento = ? ORDER BY numero_cuenta", documento));
    }

    public Map<String, Object> buscarCuenta(String numeroCuenta) {
        List<Map<String, Object>> filas = jdbc.queryForList("SELECT * FROM gc_cuenta WHERE numero_cuenta = ?", numeroCuenta);
        return filas.isEmpty() ? null : camel(filas.get(0));
    }

    // ------------------------------------------------------------------ casos (upsert generico)

    /**
     * Actualiza la fila si existe o la inserta si no. Solo se escriben las columnas con valor
     * (no se borran datos ya guardados cuando una variable todavia no existe en el proceso).
     */
    public void upsert(String tabla, String columnaLlave, String valorLlave, Map<String, Object> columnas) {
        Map<String, Object> conValor = new LinkedHashMap<>();
        columnas.forEach((k, v) -> {
            if (v != null) {
                conValor.put(k, v);
            }
        });
        int actualizadas = 0;
        if (!conValor.isEmpty()) {
            String set = conValor.keySet().stream().map(c -> c + " = ?").collect(Collectors.joining(", "));
            List<Object> parametros = new ArrayList<>(conValor.values());
            parametros.add(valorLlave);
            actualizadas = jdbc.update("UPDATE " + tabla + " SET " + set + ", fecha_actualizacion = CURRENT_TIMESTAMP WHERE "
                    + columnaLlave + " = ?", parametros.toArray());
        }
        if (actualizadas == 0) {
            List<String> nombres = new ArrayList<>();
            nombres.add(columnaLlave);
            nombres.addAll(conValor.keySet());
            List<Object> parametros = new ArrayList<>();
            parametros.add(valorLlave);
            parametros.addAll(conValor.values());
            String marcas = String.join(", ", Collections.nCopies(nombres.size(), "?"));
            jdbc.update("INSERT INTO " + tabla + " (" + String.join(", ", nombres) + ") VALUES (" + marcas + ")",
                    parametros.toArray());
        }
    }

    public List<Map<String, Object>> solicitudesDe(String documento) {
        return camel(jdbc.queryForList(
                "SELECT * FROM gc_solicitud_conexion WHERE documento = ? ORDER BY fecha_creacion DESC", documento));
    }

    public List<Map<String, Object>> facturasDe(List<String> cuentas) {
        if (cuentas.isEmpty()) {
            return List.of();
        }
        String marcas = String.join(", ", Collections.nCopies(cuentas.size(), "?"));
        return camel(jdbc.queryForList("SELECT * FROM gc_factura WHERE numero_cuenta IN (" + marcas + ") "
                + "ORDER BY fecha_emision DESC, numero_factura DESC", cuentas.toArray()));
    }

    public List<Map<String, Object>> pqrsDe(List<String> cuentas) {
        if (cuentas.isEmpty()) {
            return List.of();
        }
        String marcas = String.join(", ", Collections.nCopies(cuentas.size(), "?"));
        return camel(jdbc.queryForList("SELECT * FROM gc_pqrs WHERE numero_cuenta IN (" + marcas + ") "
                + "ORDER BY fecha_radicacion DESC, numero_radicado DESC", cuentas.toArray()));
    }

    /** Busca un caso por su numero de negocio en las tres tablas. Devuelve {tipo, registro} o null. */
    public Map<String, Object> buscarCaso(String referencia) {
        String[][] fuentes = {
                {"CONEXION", "gc_solicitud_conexion", "numero_solicitud"},
                {"FACTURA", "gc_factura", "numero_factura"},
                {"PQRS", "gc_pqrs", "numero_radicado"}};
        for (String[] f : fuentes) {
            List<Map<String, Object>> filas = jdbc.queryForList("SELECT * FROM " + f[1] + " WHERE " + f[2] + " = ?", referencia);
            if (!filas.isEmpty()) {
                Map<String, Object> caso = new LinkedHashMap<>();
                caso.put("tipo", f[0]);
                caso.put("referencia", referencia);
                caso.put("registro", camel(filas.get(0)));
                return caso;
            }
        }
        return null;
    }

    /** Busca el caso de negocio vinculado a una instancia de proceso principal. */
    public Map<String, Object> buscarCasoPorInstancia(String processInstanceId) {
        String[][] fuentes = {
                {"CONEXION", "gc_solicitud_conexion", "numero_solicitud"},
                {"FACTURA", "gc_factura", "numero_factura"},
                {"PQRS", "gc_pqrs", "numero_radicado"}};
        for (String[] f : fuentes) {
            List<Map<String, Object>> filas = jdbc.queryForList(
                    "SELECT * FROM " + f[1] + " WHERE process_instance_id = ?", processInstanceId);
            if (!filas.isEmpty()) {
                Map<String, Object> registro = camel(filas.get(0));
                Map<String, Object> caso = new LinkedHashMap<>();
                caso.put("tipo", f[0]);
                caso.put("referencia", filas.get(0).get(f[2].toUpperCase(Locale.ROOT)));
                caso.put("registro", registro);
                return caso;
            }
        }
        return null;
    }

    public List<Map<String, Object>> casosRecientes(int limite) {
        String sql = "SELECT * FROM ("
                + " SELECT 'CONEXION' AS tipo, numero_solicitud AS referencia, documento AS titular, estado, process_instance_id,"
                + "        COALESCE(fecha_actualizacion, fecha_creacion) AS ultima_actualizacion FROM gc_solicitud_conexion"
                + " UNION ALL"
                + " SELECT 'FACTURA', numero_factura, numero_cuenta, estado, process_instance_id,"
                + "        COALESCE(fecha_actualizacion, CAST(fecha_emision AS TIMESTAMP)) FROM gc_factura"
                + " UNION ALL"
                + " SELECT 'PQRS', numero_radicado, numero_cuenta, estado, process_instance_id,"
                + "        COALESCE(fecha_actualizacion, CAST(fecha_radicacion AS TIMESTAMP)) FROM gc_pqrs"
                + ") casos ORDER BY ultima_actualizacion DESC NULLS LAST LIMIT ?";
        return camel(jdbc.queryForList(sql, limite));
    }

    // ------------------------------------------------------------------ bitacora

    public void registrarEvento(String rootProcessInstanceId, String processInstanceId, String proceso, String referencia,
                                String evento, String actividad, String usuario, String detalle) {
        jdbc.update("INSERT INTO gc_evento_caso (root_process_instance_id, process_instance_id, proceso, referencia, evento, "
                        + "actividad, usuario, detalle) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                rootProcessInstanceId, processInstanceId, proceso, recortar(referencia, 40), evento,
                recortar(actividad, 150), recortar(usuario, 60), recortar(detalle, 500));
    }

    public List<Map<String, Object>> eventosDe(String rootProcessInstanceId) {
        return camel(jdbc.queryForList("SELECT * FROM gc_evento_caso WHERE root_process_instance_id = ? ORDER BY id",
                rootProcessInstanceId));
    }

    // ------------------------------------------------------------------ conversiones

    public static BigDecimal decimal(Object valor) {
        if (valor instanceof BigDecimal b) {
            return b;
        }
        if (valor instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (valor instanceof String s && !s.isBlank()) {
            try {
                return new BigDecimal(s.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static Integer entero(Object valor) {
        BigDecimal d = decimal(valor);
        return d == null ? null : d.intValue();
    }

    public static Boolean booleano(Object valor) {
        if (valor instanceof Boolean b) {
            return b;
        }
        if (valor instanceof String s && !s.isBlank()) {
            return Boolean.parseBoolean(s.trim());
        }
        return null;
    }

    public static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    /** Acepta java.util.Date o texto ISO (2026-11-18, 2026-11-18T10:15:00...). */
    public static Date fecha(Object valor) {
        if (valor instanceof java.util.Date d) {
            return new Date(d.getTime());
        }
        if (valor instanceof String s && s.length() >= 10) {
            try {
                return Date.valueOf(LocalDate.parse(s.substring(0, 10)));
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    public static Timestamp fechaHora(Object valor) {
        if (valor instanceof java.util.Date d) {
            return new Timestamp(d.getTime());
        }
        if (valor instanceof String s && !s.isBlank()) {
            try {
                return Timestamp.from(Instant.parse(s));
            } catch (RuntimeException e1) {
                try {
                    return Timestamp.from(OffsetDateTime.parse(s).toInstant());
                } catch (RuntimeException e2) {
                    try {
                        return Timestamp.valueOf(LocalDateTime.parse(s));
                    } catch (RuntimeException e3) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private static String recortar(String valor, int max) {
        return valor == null || valor.length() <= max ? valor : valor.substring(0, max);
    }

    private static List<Map<String, Object>> camel(List<Map<String, Object>> filas) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map<String, Object> fila : filas) {
            resultado.add(camel(fila));
        }
        return resultado;
    }

    private static Map<String, Object> camel(Map<String, Object> fila) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        fila.forEach((columna, valor) -> resultado.put(aCamel(columna), presentable(valor)));
        return resultado;
    }

    private static Object presentable(Object valor) {
        if (valor instanceof Timestamp t) {
            return t.toLocalDateTime().format(FORMATO_FECHA_HORA);
        }
        if (valor instanceof Date d) {
            return d.toLocalDate().toString();
        }
        return valor;
    }

    private static String aCamel(String columna) {
        String[] partes = columna.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder(partes[0]);
        for (int i = 1; i < partes.length; i++) {
            if (!partes[i].isEmpty()) {
                sb.append(Character.toUpperCase(partes[i].charAt(0))).append(partes[i].substring(1));
            }
        }
        return sb.toString();
    }
}
