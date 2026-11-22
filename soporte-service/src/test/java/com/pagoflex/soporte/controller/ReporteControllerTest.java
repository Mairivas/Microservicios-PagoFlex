package com.pagoflex.soporte.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pagoflex.soporte.dto.ReporteResponse;
import com.pagoflex.soporte.exception.GlobalExceptionHandler;
import com.pagoflex.soporte.service.ReporteService;
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
class ReporteControllerTest {

    @Mock
    private ReporteService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ReporteController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ReporteResponse respuesta(String estado) {
        return new ReporteResponse(1L, "REP-0026", 7L, "CP-2026-0106", 1L, "El monto no corresponde", null,
                LocalDateTime.of(2026, 11, 23, 11, 0), estado, null, null, false, null);
    }

    @Test
    void crearResponde201() throws Exception {
        when(service.crear(eq(1L), any())).thenReturn(respuesta("ABIERTO"));

        mvc.perform(post("/api/reportes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"compromisoId\":7,\"motivo\":\"El monto no corresponde\"}")
                        .header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("REP-0026"));
    }

    @Test
    void crearSinMotivoEsRechazado() throws Exception {
        mvc.perform(post("/api/reportes").contentType(MediaType.APPLICATION_JSON).content("{\"compromisoId\":7}")
                        .header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearComoAgenteEsProhibido() throws Exception {
        mvc.perform(post("/api/reportes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"compromisoId\":7,\"motivo\":\"Otro\"}")
                        .header("X-User-Id", 9).header("X-User-Role", "AGENTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarEntregaLosReportes() throws Exception {
        when(service.listar(9L, "AGENTE", "ABIERTO")).thenReturn(List.of(respuesta("ABIERTO")));

        mvc.perform(get("/api/reportes?estado=ABIERTO").header("X-User-Id", 9).header("X-User-Role", "AGENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].folio").value("CP-2026-0106"));
    }

    @Test
    void obtenerEntregaElReporte() throws Exception {
        when(service.obtener(1L, 1L, "USUARIO")).thenReturn(respuesta("ABIERTO"));

        mvc.perform(get("/api/reportes/1").header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isOk());
    }

    @Test
    void tomarSoloLoHaceUnAgente() throws Exception {
        when(service.tomar(1L, 9L)).thenReturn(respuesta("EN_GESTION"));

        mvc.perform(put("/api/reportes/1/tomar").header("X-User-Id", 9).header("X-User-Role", "AGENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_GESTION"));
        mvc.perform(put("/api/reportes/1/tomar").header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resolverExigeUnaRespuesta() throws Exception {
        mvc.perform(put("/api/reportes/1/resolver").contentType(MediaType.APPLICATION_JSON).content("{\"respuesta\":\" \"}")
                        .header("X-User-Id", 9).header("X-User-Role", "AGENTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resolverEntregaElReporteResuelto() throws Exception {
        when(service.resolver(1L, 9L, "Listo")).thenReturn(respuesta("RESUELTO"));

        mvc.perform(put("/api/reportes/1/resolver").contentType(MediaType.APPLICATION_JSON).content("{\"respuesta\":\"Listo\"}")
                        .header("X-User-Id", 9).header("X-User-Role", "AGENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTO"));
    }

    @Test
    void resolverComoUsuarioEsProhibido() throws Exception {
        mvc.perform(put("/api/reportes/1/resolver").contentType(MediaType.APPLICATION_JSON).content("{\"respuesta\":\"Listo\"}")
                        .header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isForbidden());
    }
}
