package com.pagoflex.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PreferenciasAvisosRequest {

    @NotNull(message = "Indica si los avisos están activos")
    private Boolean avisosActivos;

    @NotNull(message = "Indica los días de anticipación")
    @Min(value = 0, message = "Días inválidos")
    @Max(value = 5, message = "Días inválidos")
    private Integer avisosDias;

    public Boolean getAvisosActivos() {
        return avisosActivos;
    }

    public void setAvisosActivos(Boolean avisosActivos) {
        this.avisosActivos = avisosActivos;
    }

    public Integer getAvisosDias() {
        return avisosDias;
    }

    public void setAvisosDias(Integer avisosDias) {
        this.avisosDias = avisosDias;
    }
}
