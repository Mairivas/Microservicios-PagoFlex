package com.pagoflex.compromisos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CompromisoResponse(
        Long id,
        String folio,
        Long usuarioId,
        Long empresaId,
        String empresaNombre,
        String concepto,
        Integer cuotaNumero,
        Integer cuotaTotal,
        Integer monto,
        Integer recargo,
        Integer total,
        LocalDate vencimiento,
        String estado,
        Integer diasAtraso,
        LocalDateTime fechaPago,
        String motivoAnulacion) {
}
