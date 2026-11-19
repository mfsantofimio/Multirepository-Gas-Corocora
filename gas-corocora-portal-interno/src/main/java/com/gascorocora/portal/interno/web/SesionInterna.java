package com.gascorocora.portal.interno.web;

import java.io.Serializable;
import java.util.List;

/** Funcionario autenticado contra la identidad de Camunda, con sus grupos (roles). */
public class SesionInterna implements Serializable {

    public static final String ATRIBUTO = "funcionario";

    private final String usuario;
    private final String nombre;
    private final List<String> grupos;
    private final List<String> nombresGrupos;

    public SesionInterna(String usuario, String nombre, List<String> grupos, List<String> nombresGrupos) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.grupos = List.copyOf(grupos);
        this.nombresGrupos = List.copyOf(nombresGrupos);
    }

    public boolean tieneGrupo(String grupo) {
        return grupos.contains(grupo);
    }

    public boolean tieneAlguno(List<String> otros) {
        return otros.stream().anyMatch(grupos::contains);
    }

    public String getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public List<String> getGrupos() { return grupos; }
    public List<String> getNombresGrupos() { return nombresGrupos; }
}
