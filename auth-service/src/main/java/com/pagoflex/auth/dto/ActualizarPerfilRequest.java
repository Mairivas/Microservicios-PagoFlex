package com.pagoflex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ActualizarPerfilRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 60)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 60)
    private String apellidos;

    @Pattern(regexp = "^$|^\\+56 9 \\d{4} \\d{4}$", message = "Formato de teléfono inválido")
    private String telefono;

    @NotNull(message = "La comuna es obligatoria")
    private Long comunaId;

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Long getComunaId() {
        return comunaId;
    }

    public void setComunaId(Long comunaId) {
        this.comunaId = comunaId;
    }
}
