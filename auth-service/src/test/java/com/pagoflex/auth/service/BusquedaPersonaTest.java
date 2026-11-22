package com.pagoflex.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pagoflex.auth.dto.PersonaResponse;
import com.pagoflex.auth.entity.Rol;
import com.pagoflex.auth.entity.Usuario;
import com.pagoflex.auth.exception.BadRequestException;
import com.pagoflex.auth.exception.ResourceNotFoundException;
import com.pagoflex.auth.repository.ComunaRepository;
import com.pagoflex.auth.repository.RecuperacionClaveRepository;
import com.pagoflex.auth.repository.RolRepository;
import com.pagoflex.auth.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class BusquedaPersonaTest {

    private UsuarioRepository usuarioRepository;
    private AuthService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        service = new AuthService(usuarioRepository, mock(RolRepository.class), mock(ComunaRepository.class),
                mock(RecuperacionClaveRepository.class), mock(PasswordEncoder.class), mock(JwtService.class));
    }

    private Usuario usuario(String rol) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setRut("15.834.207-3");
        u.setNombres("Camila");
        u.setApellidos("Rojas");
        u.setRol(new Rol(1L, rol));
        return u;
    }

    @Test
    void encuentraAlUsuarioFinalPorRut() {
        when(usuarioRepository.findByRut("15.834.207-3")).thenReturn(Optional.of(usuario("USUARIO")));

        PersonaResponse persona = service.buscarPersonaPorRut("15834207-3");

        assertEquals(1L, persona.id());
        assertEquals("Camila", persona.nombres());
    }

    @Test
    void unRutInvalidoEsRechazado() {
        assertThrows(BadRequestException.class, () -> service.buscarPersonaPorRut("15.834.207-4"));
    }

    @Test
    void unAgenteNoSeEntregaComoPersona() {
        when(usuarioRepository.findByRut("15.834.207-3")).thenReturn(Optional.of(usuario("AGENTE")));

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPersonaPorRut("15.834.207-3"));
    }

    @Test
    void unRutSinRegistroNoSeEncuentra() {
        when(usuarioRepository.findByRut("15.834.207-3")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPersonaPorRut("15.834.207-3"));
    }
}
