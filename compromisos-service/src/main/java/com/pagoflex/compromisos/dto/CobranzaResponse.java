package com.pagoflex.compromisos.dto;

public record CobranzaResponse(
        String mes,
        Long compromisos,
        Long anulados,
        Long pagados,
        Long vencidos,
        Long pendientes,
        Integer montoRecaudado,
        Double tasa) {
}
