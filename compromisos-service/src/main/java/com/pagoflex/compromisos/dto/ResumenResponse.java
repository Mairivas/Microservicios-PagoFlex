package com.pagoflex.compromisos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResumenResponse(
        Integer totalPorPagarMes,
        LocalDate proximoVencimiento,
        Long vencidos,
        LocalDateTime actualizadoEn) {
}
