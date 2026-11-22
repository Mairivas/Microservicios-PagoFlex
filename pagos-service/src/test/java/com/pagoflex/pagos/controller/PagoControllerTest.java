package com.pagoflex.pagos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pagoflex.pagos.dto.CumplimientoResponse;
import com.pagoflex.pagos.dto.PagoResponse;
import com.pagoflex.pagos.exception.ApiException;
import com.pagoflex.pagos.exception.GlobalExceptionHandler;
import com.pagoflex.pagos.service.PagoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PagoControllerTest {

    @Mock
    private PagoService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PagoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PagoResponse respuesta() {
        return new PagoResponse(1L, "CPR-000609", 7L, "CP-2026-0106", 1L, "Altos del Maipo", "Cuota", "14/36",
                310000, 0, 310000, "Pago simulado", "App", LocalDateTime.of(2026, 11, 22, 12, 0), true);
    }

    @Test
    void pagarResponde201ConElComprobante() throws Exception {
        when(service.pagar(eq(1L), any())).thenReturn(respuesta());

        mvc.perform(post("/api/pagos").contentType(MediaType.APPLICATION_JSON).content("{\"compromisoId\":7}")
                        .header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comprobante").value("CPR-000609"));
    }

    @Test
    void pagarSinCompromisoEsRechazado() throws Exception {
        mvc.perform(post("/api/pagos").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagarComoAgenteEsProhibido() throws Exception {
        mvc.perform(post("/api/pagos").contentType(MediaType.APPLICATION_JSON).content("{\"compromisoId\":7}")
                        .header("X-User-Id", 9).header("X-User-Role", "AGENTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void historialEntregaLosPagos() throws Exception {
        when(service.historial(1L, 6L, LocalDate.of(2026, 9, 1), null)).thenReturn(List.of(respuesta()));

        mvc.perform(get("/api/pagos?empresaId=6&desde=2026-09-01").header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].folio").value("CP-2026-0106"));
    }

    @Test
    void historialSinSesionEsNoAutorizado() throws Exception {
        mvc.perform(get("/api/pagos")).andExpect(status().isUnauthorized());
    }

    @Test
    void cumplimientoEntregaElIndicador() throws Exception {
        when(service.cumplimiento(1L)).thenReturn(new CumplimientoResponse(36, 33, 91.7, "Al día", true, "Bien"));

        mvc.perform(get("/api/pagos/cumplimiento").header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("Al día"));
    }

    @Test
    void comprobanteEntregaElPago() throws Exception {
        when(service.obtener(1L, 1L)).thenReturn(respuesta());

        mvc.perform(get("/api/pagos/1").header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoPagado").value(310000));
    }

    @Test
    void comprobanteAjenoEsNoEncontrado() throws Exception {
        when(service.obtener(2L, 1L)).thenThrow(ApiException.notFound("No encontramos ese comprobante."));

        mvc.perform(get("/api/pagos/2").header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isNotFound());
    }
}
