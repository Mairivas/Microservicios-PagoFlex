package com.pagoflex.auth.controller;

import com.pagoflex.auth.dto.LoginRequest;
import com.pagoflex.auth.dto.RegistroRequest;
import com.pagoflex.auth.dto.LoginResponse;
import com.pagoflex.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    @WithMockUser
    void loginTest() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new LoginResponse("fake-token", 1L, "Test", "test@test.cl", "ROLE_USER", null));
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"test@test.cl\",\"clave\":\"Pass123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void registerTest() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rut\":\"11111111-1\",\"nombres\":\"Test\",\"apellidos\":\"User\",\"correo\":\"test@test.cl\",\"clave\":\"Pass1234\",\"confirmacionClave\":\"Pass1234\",\"comunaId\":1}"))
                .andExpect(status().isCreated());
    }
}
