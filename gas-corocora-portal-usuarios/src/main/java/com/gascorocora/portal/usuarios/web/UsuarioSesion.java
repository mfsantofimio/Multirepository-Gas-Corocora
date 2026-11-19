package com.gascorocora.portal.usuarios.web;

import java.io.Serializable;
import java.util.Map;

/** Usuario externo autenticado, guardado en la sesion HTTP. */
public class UsuarioSesion implements Serializable {

    public static final String ATRIBUTO = "usuario";

    private final String documento;
    private final String nombre;
    private final String email;
    private final String telefono;
    private final String direccion;
    private final String municipio;

    public UsuarioSesion(String documento, String nombre, String email, String telefono, String direccion, String municipio) {
        this.documento = documento;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
        this.municipio = municipio;
    }

    public static UsuarioSesion desde(Map<String, Object> suscriptor) {
        return new UsuarioSesion(texto(suscriptor.get("documento")), texto(suscriptor.get("nombre")),
                texto(suscriptor.get("email")), texto(suscriptor.get("telefono")),
                texto(suscriptor.get("direccion")), texto(suscriptor.get("municipio")));
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    public String getDocumento() { return documento; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getDireccion() { return direccion; }
    public String getMunicipio() { return municipio; }
}
