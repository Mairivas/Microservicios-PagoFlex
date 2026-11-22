package com.pagoflex.compromisos.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pagoflex.compromisos.dto.CompromisoResponse;
import com.pagoflex.compromisos.exception.GlobalExceptionHandler;
import com.pagoflex.compromisos.service.CompromisoService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class InternalCompromisoControllerTest {

    @Mock
    private CompromisoService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new InternalCompromisoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CompromisoResponse respuesta(String estado) {
        return new CompromisoResponse(1L, "CP-2026-0101", 1L, 1L, "Altos del Maipo", "Cuota", 1, 12,
                1000, 0, 1000, LocalDate.of(2026, 12, 1), estado, 0, null, null);
    }

    @Test
    void obtenerEntregaElCompromiso() throws Exception {
        when(service.obtenerInterno(1L)).thenReturn(respuesta("PENDIENTE"));

        mvc.perform(get("/api/internal/compromisos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folio").value("CP-2026-0101"));
    }

    @Test
    void pagarEntregaElCompromisoPagado() throws Exception {
        when(service.pagar(1L, 5L)).thenReturn(respuesta("PAGADO"));

        mvc.perform(put("/api/internal/compromisos/1/pagar?usuarioId=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO"));
    }

    @Test
    void revertirPagoDevuelveElCompromiso() throws Exception {
        when(service.revertirPago(1L)).thenReturn(respuesta("PENDIENTE"));

        mvc.perform(put("/api/internal/compromisos/1/revertir-pago")).andExpect(status().isOk());
    }

    @Test
    void revisionDevuelveElCompromiso() throws Exception {
        when(service.marcarRevision(1L)).thenReturn(respuesta("EN_REVISION"));

        mvc.perform(put("/api/internal/compromisos/1/revision")).andExpect(status().isOk());
    }

    @Test
    void restaurarDevuelveElCompromiso() throws Exception {
        when(service.restaurar(1L)).thenReturn(respuesta("PENDIENTE"));

        mvc.perform(put("/api/internal/compromisos/1/restaurar")).andExpect(status().isOk());
    }
}
