package com.pagoflex.pagos.dto;

import jakarta.validation.constraints.NotNull;

public record PagoRequest(
        @NotNull(message = "Elige el compromiso que quieres pagar") Long compromisoId,
        String medioPago) {
}
