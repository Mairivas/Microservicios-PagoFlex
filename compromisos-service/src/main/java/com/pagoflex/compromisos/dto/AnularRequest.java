package com.pagoflex.compromisos.dto;

import jakarta.validation.constraints.NotBlank;

public record AnularRequest(
        @NotBlank(message = "El motivo es obligatorio") String motivo,
        String comentario) {
}
