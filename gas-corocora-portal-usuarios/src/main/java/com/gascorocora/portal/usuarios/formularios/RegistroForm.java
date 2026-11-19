package com.gascorocora.portal.usuarios.formularios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Registro de un usuario externo. Las reglas coinciden con RegistroSuscriptorRequest del motor. */
public class RegistroForm {

    @NotBlank(message = "El documento es obligatorio")
    @Pattern(regexp = "[0-9]{6,12}", message = "Solo números, entre 6 y 12 dígitos")
    private String documento;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 120, message = "Entre 3 y 120 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Correo electrónico no válido")
    private String email;

    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = "3[0-9]{9}", message = "Celular de 10 dígitos que empiece por 3")
    private String telefono;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String direccion;

    @NotBlank(message = "Seleccione el municipio")
    private String municipio;

    @NotBlank(message = "La clave es obligatoria")
    @Size(min = 6, max = 40, message = "Entre 6 y 40 caracteres")
    private String clave;

    @NotBlank(message = "Confirme la clave")
    private String confirmacion;

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento == null ? null : documento.trim(); }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim(); }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono == null ? null : telefono.trim(); }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getConfirmacion() { return confirmacion; }
    public void setConfirmacion(String confirmacion) { this.confirmacion = confirmacion; }
}
