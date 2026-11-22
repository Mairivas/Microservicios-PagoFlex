package com.pagoflex.pagos.dto;

import java.time.LocalDateTime;

public record PagoResponse(
        Long id,
        String comprobante,
        Long compromisoId,
        String folio,
        Long empresaId,
        String empresaNombre,
        String concepto,
        String cuota,
        Integer monto,
        Integer recargo,
        Integer montoPagado,
        String medioPago,
        String canal,
        LocalDateTime fechaHora,
        Boolean aTiempo) {
}
