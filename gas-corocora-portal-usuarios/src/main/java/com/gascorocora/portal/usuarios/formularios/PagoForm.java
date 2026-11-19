package com.gascorocora.portal.usuarios.formularios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class PagoForm {

    @NotBlank(message = "Seleccione el medio de pago")
    @Pattern(regexp = "PSE|TARJETA", message = "Medio de pago no válido")
    private String medioPago = "PSE";

    @NotNull(message = "Indique el valor a pagar")
    @Positive(message = "El valor debe ser mayor que cero")
    private Double valor;

    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
}
