package com.pagoflex.compromisos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record CrearCompromisoRequest(
        @NotBlank(message = "El RUT es obligatorio") String rutUsuario,
        @NotBlank(message = "El concepto es obligatorio") String concepto,
        @NotNull(message = "El monto es obligatorio") @Positive(message = "El monto debe ser mayor a 0") Integer monto,
        @NotNull(message = "El vencimiento es obligatorio") LocalDate vencimiento,
        @Positive(message = "La cuota debe ser mayor a 0") Integer cuotaNumero,
        @Positive(message = "El total de cuotas debe ser mayor a 0") Integer cuotaTotal) {
}
