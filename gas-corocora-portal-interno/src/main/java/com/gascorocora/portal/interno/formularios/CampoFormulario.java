package com.gascorocora.portal.interno.formularios;

import java.util.ArrayList;
import java.util.List;

/** Un componente de un Camunda Form listo para dibujarse en Thymeleaf. */
public class CampoFormulario {

    /** Opcion de un select. */
    public record Opcion(String valor, String etiqueta) {
        public String getValor() { return valor; }
        public String getEtiqueta() { return etiqueta; }
    }

    private String tipo;          // text | textfield | textarea | number | checkbox | select
    private String llave;
    private String etiqueta;
    private String descripcion;
    private String titulo;        // componente "text": primera linea (markdown #)
    private String parrafo;       // componente "text": resto del texto
    private boolean deshabilitado;
    private boolean requerido;
    private String patron;
    private String minimo;
    private String maximo;
    private String valor;         // valor actual como texto
    private boolean marcado;      // checkbox
    private String error;
    private final List<Opcion> opciones = new ArrayList<>();

    public boolean esEntrada() {
        return !"text".equals(tipo);
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getLlave() { return llave; }
    public void setLlave(String llave) { this.llave = llave; }
    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getParrafo() { return parrafo; }
    public void setParrafo(String parrafo) { this.parrafo = parrafo; }
    public boolean isDeshabilitado() { return deshabilitado; }
    public void setDeshabilitado(boolean deshabilitado) { this.deshabilitado = deshabilitado; }
    public boolean isRequerido() { return requerido; }
    public void setRequerido(boolean requerido) { this.requerido = requerido; }
    public String getPatron() { return patron; }
    public void setPatron(String patron) { this.patron = patron; }
    public String getMinimo() { return minimo; }
    public void setMinimo(String minimo) { this.minimo = minimo; }
    public String getMaximo() { return maximo; }
    public void setMaximo(String maximo) { this.maximo = maximo; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public boolean isMarcado() { return marcado; }
    public void setMarcado(boolean marcado) { this.marcado = marcado; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public List<Opcion> getOpciones() { return opciones; }
}
