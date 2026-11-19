package com.gascorocora.implementations.comun;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import org.camunda.bpm.engine.delegate.VariableScope;

/**
 * Lectura segura de variables de proceso. Los Camunda Forms pueden enviar numeros como
 * Long, Integer o Double (y los campos de texto como String), por eso se normalizan aqui.
 */
public final class VariablesUtil {

    private static final DateTimeFormatter FORMATO_CODIGO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private VariablesUtil() {
    }

    public static double getDouble(VariableScope scope, String nombre, double porDefecto) {
        Object valor = scope.getVariable(nombre);
        if (valor instanceof Number numero) {
            return numero.doubleValue();
        }
        if (valor instanceof String texto && !texto.isBlank()) {
            try {
                return Double.parseDouble(texto.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                return porDefecto;
            }
        }
        return porDefecto;
    }

    public static boolean getBoolean(VariableScope scope, String nombre, boolean porDefecto) {
        Object valor = scope.getVariable(nombre);
        if (valor instanceof Boolean booleano) {
            return booleano;
        }
        if (valor instanceof String texto) {
            return Boolean.parseBoolean(texto.trim());
        }
        return porDefecto;
    }

    public static String getString(VariableScope scope, String nombre, String porDefecto) {
        Object valor = scope.getVariable(nombre);
        return valor == null ? porDefecto : valor.toString();
    }

    /** Genera codigos legibles del tipo PREFIJO-20261113-4821. */
    public static String generarCodigo(String prefijo) {
        int consecutivo = ThreadLocalRandom.current().nextInt(1000, 10000);
        return prefijo + "-" + LocalDate.now().format(FORMATO_CODIGO) + "-" + consecutivo;
    }

    /** Suma dias habiles (lunes a viernes) a una fecha. */
    public static LocalDate sumarDiasHabiles(LocalDate desde, int diasHabiles) {
        LocalDate fecha = desde;
        int sumados = 0;
        while (sumados < diasHabiles) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() != DayOfWeek.SATURDAY && fecha.getDayOfWeek() != DayOfWeek.SUNDAY) {
                sumados++;
            }
        }
        return fecha;
    }

    public static double redondear(double valor, int decimales) {
        double factor = Math.pow(10, decimales);
        return Math.round(valor * factor) / factor;
    }
}
