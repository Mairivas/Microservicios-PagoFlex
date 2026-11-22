package com.pagoflex.soporte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolverRequest(
        @NotBlank(message = "Escribe una respuesta para el usuario")
        @Size(max = 500, message = "La respuesta puede tener hasta 500 caracteres") String respuesta) {
}
