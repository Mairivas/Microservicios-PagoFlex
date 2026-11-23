package com.pagoflex.soporte.dto;

import jakarta.validation.constraints.NotBlank;

public record EvidenciaDto(@NotBlank(message = "Adjunta una imagen") String imagenBase64) {
}
