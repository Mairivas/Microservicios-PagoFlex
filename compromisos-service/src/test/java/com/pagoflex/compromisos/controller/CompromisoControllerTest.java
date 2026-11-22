package com.pagoflex.compromisos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pagoflex.compromisos.dto.CobranzaResponse;
import com.pagoflex.compromisos.dto.CompromisoResponse;
import com.pagoflex.compromisos.dto.ResumenResponse;
import com.pagoflex.compromisos.exception.GlobalExceptionHandler;
import com.pagoflex.compromisos.service.CompromisoService;
import java.time.LocalDate;
import java.time.YearMonth;
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
class CompromisoControllerTest {

    private static final String CUERPO_VALIDO =
            "{\"rutUsuario\":\"15.834.207-3\",\"concepto\":\"Cuota\",\"monto\":1000,\"vencimiento\":\"2030-01-10\"}";

    @Mock
    private CompromisoService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CompromisoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CompromisoResponse respuesta() {
        return new CompromisoResponse(1L, "CP-2026-0101", 1L, 1L, "Altos del Maipo", "Cuota", 1, 12,
                1000, 0, 1000, LocalDate.of(2026, 12, 1), "PENDIENTE", 0, null, null);
    }

    @Test
    void listarEntregaLosCompromisos() throws Exception {
        when(service.listar(1L, "USUARIO", null, null)).thenReturn(List.of(respuesta()));

        mvc.perform(get("/api/compromisos").header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].folio").value("CP-2026-0101"));
    }

    @Test
    void listarSinSesionEsNoAutorizado() throws Exception {
        mvc.perform(get("/api/compromisos")).andExpect(status().isUnauthorized());
    }

    @Test
    void listarConEmpresaDelEjecutivo() throws Exception {
        when(service.listar(14L, "EJECUTIVO", 1L, "VENCIDO")).thenReturn(List.of());

        mvc.perform(get("/api/compromisos?estado=VENCIDO")
                        .header("X-User-Id", 14).header("X-User-Role", "EJECUTIVO").header("X-Empresa-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void resumenEsSoloParaUsuarios() throws Exception {
        mvc.perform(get("/api/compromisos/resumen").header("X-User-Id", 14).header("X-User-Role", "EJECUTIVO"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resumenEntregaLosTotales() throws Exception {
        when(service.resumen(1L)).thenReturn(new ResumenResponse(5000, LocalDate.of(2026, 12, 1), 1L, null));

        mvc.perform(get("/api/compromisos/resumen").header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPorPagarMes").value(5000));
    }

    @Test
    void cobranzaEntregaLaTasa() throws Exception {
        when(service.cobranza(1L, YearMonth.of(2026, 9)))
                .thenReturn(new CobranzaResponse("2026-09", 4L, 0L, 3L, 1L, 0L, 300, 75.0));

        mvc.perform(get("/api/compromisos/cobranza?mes=2026-09")
                        .header("X-User-Role", "EJECUTIVO").header("X-Empresa-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasa").value(75.0));
    }

    @Test
    void cobranzaConMesInvalidoEsRechazada() throws Exception {
        mvc.perform(get("/api/compromisos/cobranza?mes=septiembre")
                        .header("X-User-Role", "EJECUTIVO").header("X-Empresa-Id", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cobranzaSinEmpresaEsProhibida() throws Exception {
        mvc.perform(get("/api/compromisos/cobranza").header("X-User-Role", "EJECUTIVO").header("X-Empresa-Id", ""))
                .andExpect(status().isForbidden());
    }

    @Test
    void cobranzaDeUsuarioEsProhibida() throws Exception {
        mvc.perform(get("/api/compromisos/cobranza").header("X-User-Role", "USUARIO"))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerEntregaElDetalle() throws Exception {
        when(service.obtener(1L, 1L, "USUARIO", null)).thenReturn(respuesta());

        mvc.perform(get("/api/compromisos/1").header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaNombre").value("Altos del Maipo"));
    }

    @Test
    void crearResponde201() throws Exception {
        when(service.crear(any(), eq(1L), eq(14L))).thenReturn(respuesta());

        mvc.perform(post("/api/compromisos").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO)
                        .header("X-User-Id", 14).header("X-User-Role", "EJECUTIVO").header("X-Empresa-Id", "1"))
                .andExpect(status().isCreated());
    }

    @Test
    void crearConMontoNegativoEsRechazado() throws Exception {
        String cuerpo = CUERPO_VALIDO.replace("1000", "-5");

        mvc.perform(post("/api/compromisos").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                        .header("X-User-Id", 14).header("X-User-Role", "EJECUTIVO").header("X-Empresa-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").exists());
    }

    @Test
    void crearComoUsuarioEsProhibido() throws Exception {
        mvc.perform(post("/api/compromisos").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO)
                        .header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearSinEmpresaEsProhibido() throws Exception {
        mvc.perform(post("/api/compromisos").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO)
                        .header("X-User-Id", 14).header("X-User-Role", "EJECUTIVO"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anularEntregaElCompromisoAnulado() throws Exception {
        when(service.anular(eq(1L), any(), eq(1L), eq(14L))).thenReturn(respuesta());

        mvc.perform(post("/api/compromisos/1/anular").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Repactación\"}")
                        .header("X-User-Id", 14).header("X-User-Role", "EJECUTIVO").header("X-Empresa-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void anularSinMotivoEsRechazado() throws Exception {
        mvc.perform(post("/api/compromisos/1/anular").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header("X-User-Id", 14).header("X-User-Role", "EJECUTIVO").header("X-Empresa-Id", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anularComoUsuarioEsProhibido() throws Exception {
        mvc.perform(post("/api/compromisos/1/anular").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Repactación\"}")
                        .header("X-User-Id", 1).header("X-User-Role", "USUARIO"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anularSinEmpresaEsProhibido() throws Exception {
        mvc.perform(post("/api/compromisos/1/anular").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Repactación\"}")
                        .header("X-User-Id", 14).header("X-User-Role", "EJECUTIVO"))
                .andExpect(status().isForbidden());
    }
}
