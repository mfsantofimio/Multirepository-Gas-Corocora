package com.gascorocora.portal.usuarios.formularios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Radicacion de una PQRS por el canal web. */
public class PqrsForm {

    @NotBlank(message = "Seleccione la cuenta")
    private String numeroCuenta;

    @NotBlank(message = "Seleccione el tipo de solicitud")
    @Pattern(regexp = "PETICION|QUEJA|RECLAMO|SUGERENCIA", message = "Tipo no válido")
    private String tipoSolicitud;

    @PositiveOrZero(message = "El valor no puede ser negativo")
    private Double valorReclamado;

    @NotBlank(message = "Describa su solicitud")
    @Size(min = 10, max = 1000, message = "Entre 10 y 1000 caracteres")
    private String descripcion;

    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }
    public String getTipoSolicitud() { return tipoSolicitud; }
    public void setTipoSolicitud(String tipoSolicitud) { this.tipoSolicitud = tipoSolicitud; }
    public Double getValorReclamado() { return valorReclamado; }
    public void setValorReclamado(Double valorReclamado) { this.valorReclamado = valorReclamado; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
