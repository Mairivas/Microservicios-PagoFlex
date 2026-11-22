package com.pagoflex.soporte.dto;

import java.time.LocalDateTime;

public record ReporteResponse(
        Long id,
        String numero,
        Long compromisoId,
        String folio,
        Long usuarioId,
        String motivo,
        String comentario,
        LocalDateTime fecha,
        String estado,
        Long agenteId,
        String respuesta,
        Boolean atrasado,
        LocalDateTime fechaResolucion) {
}
