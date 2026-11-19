package com.gascorocora.portal.usuarios.formularios;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos del predio para la solicitud de conexion (los datos personales salen del registro). */
public class SolicitudConexionForm {

    @NotBlank(message = "La dirección del predio es obligatoria")
    @Size(min = 5, max = 200, message = "Entre 5 y 200 caracteres")
    private String direccionPredio;

    @NotBlank(message = "Seleccione el municipio")
    private String municipio;

    @NotBlank(message = "Seleccione el tipo de predio")
    @Pattern(regexp = "RESIDENCIAL|COMERCIAL|INDUSTRIAL", message = "Tipo de predio no válido")
    private String tipoPredio;

    @NotNull(message = "Indique el estrato")
    @Min(value = 1, message = "El estrato va de 1 a 6")
    @Max(value = 6, message = "El estrato va de 1 a 6")
    private Integer estrato;

    public String getDireccionPredio() { return direccionPredio; }
    public void setDireccionPredio(String direccionPredio) { this.direccionPredio = direccionPredio; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public String getTipoPredio() { return tipoPredio; }
    public void setTipoPredio(String tipoPredio) { this.tipoPredio = tipoPredio; }
    public Integer getEstrato() { return estrato; }
    public void setEstrato(Integer estrato) { this.estrato = estrato; }
}
