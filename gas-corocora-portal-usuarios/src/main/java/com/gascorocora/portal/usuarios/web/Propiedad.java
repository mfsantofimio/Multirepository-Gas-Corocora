package com.gascorocora.portal.usuarios.web;

import java.util.List;
import java.util.Map;

/** Verifica que un tramite pertenezca al usuario, buscandolo en la respuesta de /tramites del motor. */
final class Propiedad {

    private Propiedad() {
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> lista(Map<String, Object> tramites, String llave) {
        Object valor = tramites.get(llave);
        return valor instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> cuentas(Map<String, Object> tramites) {
        Object suscriptor = tramites.get("suscriptor");
        if (suscriptor instanceof Map<?, ?> s && s.get("cuentas") instanceof List<?> l) {
            return (List<Map<String, Object>>) l;
        }
        return List.of();
    }

    /** Busca por numero de solicitud, factura o radicado. */
    static Map<String, Object> buscar(Map<String, Object> tramites, String referencia) {
        String[][] fuentes = {{"solicitudes", "numeroSolicitud"}, {"facturas", "numeroFactura"}, {"pqrs", "numeroRadicado"}};
        for (String[] f : fuentes) {
            for (Map<String, Object> registro : lista(tramites, f[0])) {
                if (referencia.equals(String.valueOf(registro.get(f[1])))) {
                    return registro;
                }
            }
        }
        return null;
    }

    static boolean tieneAccion(Map<String, Object> registro, String accion) {
        return registro.get("accionesUsuario") instanceof List<?> acciones && acciones.contains(accion);
    }
}
