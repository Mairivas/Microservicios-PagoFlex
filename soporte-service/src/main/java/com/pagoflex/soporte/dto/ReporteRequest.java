package com.pagoflex.soporte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReporteRequest(
        @NotNull(message = "Elige el compromiso que quieres reportar") Long compromisoId,
        @NotBlank(message = "Elige un motivo") String motivo,
        @Size(max = 500, message = "El comentario puede tener hasta 500 caracteres") String comentario) {
}
