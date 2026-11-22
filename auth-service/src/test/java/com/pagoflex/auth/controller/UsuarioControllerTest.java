package com.pagoflex.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagoflex.auth.dto.PerfilResponse;
import com.pagoflex.auth.exception.GlobalExceptionHandler;
import com.pagoflex.auth.exception.ResourceNotFoundException;
import com.pagoflex.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private UsuarioController usuarioController;

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        mapper = new ObjectMapper();
    }

    @Test
    void verPerfilRetorna200() throws Exception {
        PerfilResponse perfil = new PerfilResponse();
        perfil.setId(1L);
        perfil.setCorreo("camila@correo.ejemplo");
        when(authService.obtenerPerfil(1L)).thenReturn(perfil);

        mockMvc.perform(get("/api/usuarios/perfil").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("camila@correo.ejemplo"));
    }

    @Test
    void verPerfilNoEncontradoRetorna404() throws Exception {
        when(authService.obtenerPerfil(99L)).thenThrow(new ResourceNotFoundException("No existe"));

        mockMvc.perform(get("/api/usuarios/perfil").header("X-User-Id", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiarClaveRetorna200() throws Exception {
        String body = "{\"claveActual\":\"oldpass\",\"nuevaClave\":\"newpassword\",\"confirmacionClave\":\"newpassword\"}";
        mockMvc.perform(post("/api/usuarios/cambiar-clave")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarClaveSinClaveActualRetorna400() throws Exception {
        String body = "{\"nuevaClave\":\"newpassword\",\"confirmacionClave\":\"newpassword\"}";
        mockMvc.perform(post("/api/usuarios/cambiar-clave")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editarPerfilRetorna200() throws Exception {
        PerfilResponse perfil = new PerfilResponse();
        perfil.setNombres("Camila Andrea");
        when(authService.actualizarPerfil(eq(1L), any())).thenReturn(perfil);
        String body = "{\"nombres\":\"Camila Andrea\",\"apellidos\":\"Rojas\",\"comunaId\":1}";
        mockMvc.perform(put("/api/usuarios/perfil")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Camila Andrea"));
    }

    @Test
    void actualizarPreferenciasRetorna200() throws Exception {
        PerfilResponse perfil = new PerfilResponse();
        perfil.setAvisosActivos(true);
        perfil.setAvisosDias(5);
        when(authService.actualizarPreferencias(eq(1L), any())).thenReturn(perfil);
        String body = "{\"avisosActivos\":true,\"avisosDias\":5}";
        mockMvc.perform(put("/api/usuarios/preferencias-avisos")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisosDias").value(5));
    }

    @Test
    void buscarPorRutEntregaLaPersona() throws Exception {
        when(authService.buscarPersonaPorRut("15.834.207-3"))
                .thenReturn(new com.pagoflex.auth.dto.PersonaResponse(1L, "15.834.207-3", "Camila", "Rojas"));

        mockMvc.perform(get("/api/usuarios/buscar").param("rut", "15.834.207-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Camila"));
    }
}
