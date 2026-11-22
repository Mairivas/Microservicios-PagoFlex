package com.pagoflex.pagos.dto;

import java.time.LocalDate;

public record CompromisoDto(
        Long id,
        String folio,
        Long empresaId,
        String empresaNombre,
        String concepto,
        Integer cuotaNumero,
        Integer cuotaTotal,
        Integer monto,
        Integer recargo,
        Integer total,
        LocalDate vencimiento) {
}
