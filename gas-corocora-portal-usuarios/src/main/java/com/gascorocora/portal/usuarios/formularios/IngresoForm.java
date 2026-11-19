package com.gascorocora.portal.usuarios.formularios;

import jakarta.validation.constraints.NotBlank;

public class IngresoForm {

    @NotBlank(message = "Ingrese su número de documento")
    private String documento;

    @NotBlank(message = "Ingrese su clave")
    private String clave;

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento == null ? null : documento.trim(); }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
}
