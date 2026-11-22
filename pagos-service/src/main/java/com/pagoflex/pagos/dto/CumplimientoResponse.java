package com.pagoflex.pagos.dto;

public record CumplimientoResponse(
        Integer considerados,
        Integer aTiempo,
        Double porcentaje,
        String categoria,
        Boolean historialSuficiente,
        String mensaje) {
}
